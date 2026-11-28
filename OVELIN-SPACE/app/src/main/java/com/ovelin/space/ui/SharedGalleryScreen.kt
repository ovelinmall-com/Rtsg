package com.ovelin.space.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.niunaijun.blackbox.media.BlackBoxMediaEntry
import top.niunaijun.blackbox.media.BlackBoxMediaStore

/**
 * The one gallery every clone sees. Whatever is in here is what a cloned Messenger/WhatsApp/...
 * shows in its own photo grid and can attach; whatever a clone saves ends up here too.
 */
object SharedGallery {
    private const val SHARED_USER = BlackBoxMediaStore.SHARED_USER_ID

    suspend fun list(): List<BlackBoxMediaEntry> = withContext(Dispatchers.IO) {
        runCatching { BlackBoxMediaStore.listMediaEntries(SHARED_USER) }.getOrDefault(emptyList())
    }

    /** Copies the given images/videos into the shared gallery. Returns how many were added. */
    suspend fun import(context: Context, uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var added = 0
        for (uri in uris) {
            runCatching { BlackBoxMediaStore.importMedia(context.applicationContext, SHARED_USER, uri, null) }
                .onSuccess { added++ }
        }
        added
    }

    suspend fun delete(entries: Collection<BlackBoxMediaEntry>): Int = withContext(Dispatchers.IO) {
        var removed = 0
        for (entry in entries) {
            removed += runCatching {
                BlackBoxMediaStore.delete(SHARED_USER, entry.privateUri, null, null)
            }.getOrDefault(0)
        }
        removed
    }
}

private val thumbCache = LruCache<String, ImageBitmap>(80)

private fun loadThumbnail(entry: BlackBoxMediaEntry): ImageBitmap? {
    val key = "${entry.id}:${entry.dateModified}"
    thumbCache.get(key)?.let { return it }
    val bitmap: Bitmap? = runCatching {
        if (entry.isVideo) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(entry.filePath)
                retriever.getFrameAtTime(0)
            } finally {
                runCatching { retriever.release() }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(entry.filePath, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= 360 && bounds.outHeight / (sample * 2) >= 360) sample *= 2
            BitmapFactory.decodeFile(entry.filePath, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }.getOrNull()
    val image = bitmap?.asImageBitmap()
    if (image != null) thumbCache.put(key, image)
    return image
}

/** Used when a cloned app asked for a photo/video: the user picks from the shared gallery. */
class GalleryPick(
    val multiple: Boolean,
    val images: Boolean,
    val videos: Boolean,
    val onDone: (List<BlackBoxMediaEntry>) -> Unit,
)

@Composable
fun SharedGalleryScreen(
    isArabic: Boolean,
    snackbarHostState: SnackbarHostState,
    pick: GalleryPick? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    var items by remember { mutableStateOf<List<BlackBoxMediaEntry>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(refresh) {
        items = SharedGallery.list().filter { entry ->
            pick == null || (entry.isVideo && pick.videos) || (!entry.isVideo && pick.images)
        }
        selected = selected.intersect(items.map { it.id }.toSet())
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            val added = SharedGallery.import(context, uris)
            busy = false
            refresh++
            snackbarHostState.showSnackbar(
                tr(isArabic, "تمت إضافة $added من ${uris.size} إلى المعرض المشترك.", "Added $added of ${uris.size} to the shared gallery."),
            )
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    tr(isArabic, "المعرض المشترك", "Shared gallery"),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    tr(
                        isArabic,
                        "${items.size} عنصر — تراه كل النسخ المستنسخة",
                        "${items.size} items — visible to every clone",
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (pick != null && selected.isNotEmpty()) {
                Button(onClick = { pick.onDone(items.filter { it.id in selected }) }) {
                    Text(tr(isArabic, "تم (${selected.size})", "Done (${selected.size})"))
                }
            }
            if (pick == null && selected.isNotEmpty()) {
                IconButton(
                    onClick = {
                        val toDelete = items.filter { it.id in selected }
                        scope.launch {
                            val removed = SharedGallery.delete(toDelete)
                            selected = emptySet()
                            refresh++
                            snackbarHostState.showSnackbar(
                                tr(isArabic, "تم حذف $removed من المعرض المشترك.", "Removed $removed from the shared gallery."),
                            )
                        }
                    },
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = tr(isArabic, "حذف", "Delete"))
                }
            }
            Button(
                enabled = !busy,
                onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                },
            ) {
                Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    if (busy) tr(isArabic, "جارٍ الإضافة…", "Adding…") else tr(isArabic, "إضافة من معرضي", "Add from my gallery"),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    tr(
                        isArabic,
                        "المعرض فارغ. أضف صوراً وفيديوهات من معرضك الحقيقي، أو شاركها إلى Iskander من أي تطبيق.",
                        "The gallery is empty. Add photos and videos from your real gallery, or share them to Iskander from any app.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items, key = { it.id }) { entry ->
                    val isSelected = entry.id in selected
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                selected = when {
                                    pick != null && !pick.multiple -> setOf(entry.id)
                                    isSelected -> selected - entry.id
                                    else -> selected + entry.id
                                }
                            },
                    ) {
                        val thumb by produceState<ImageBitmap?>(null, entry.id, entry.dateModified) {
                            value = withContext(Dispatchers.IO) { loadThumbnail(entry) }
                        }
                        thumb?.let {
                            Image(
                                bitmap = it,
                                contentDescription = entry.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        if (entry.isVideo) {
                            Icon(
                                Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(4.dp)
                                    .size(22.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), RoundedCornerShape(11.dp)),
                            )
                        }
                        if (isSelected) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            )
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
