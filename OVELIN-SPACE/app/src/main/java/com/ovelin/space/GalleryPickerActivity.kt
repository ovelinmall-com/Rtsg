package com.ovelin.space

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.ovelin.space.ui.GalleryPick
import com.ovelin.space.ui.OvelinSpaceTheme
import com.ovelin.space.ui.SharedGalleryScreen
import top.niunaijun.blackbox.media.BlackBoxMediaEntry
import top.niunaijun.blackbox.media.MediaPickerRedirect

/**
 * Opened instead of the phone's gallery whenever a cloned app asks to pick a photo/video.
 * Returns content:// URIs served by the engine's media provider, which is how a clone reads them.
 */
class GalleryPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val pick = GalleryPick(
            multiple = intent.getBooleanExtra(MediaPickerRedirect.EXTRA_ALLOW_MULTIPLE, false),
            images = intent.getBooleanExtra(MediaPickerRedirect.EXTRA_WANT_IMAGES, true),
            videos = intent.getBooleanExtra(MediaPickerRedirect.EXTRA_WANT_VIDEOS, true),
            onDone = ::finishWith,
        )
        setContent {
            OvelinSpaceTheme {
                val snackbar = remember { SnackbarHostState() }
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(snackbar) },
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        SharedGalleryScreen(isArabic = true, snackbarHostState = snackbar, pick = pick)
                    }
                }
            }
        }
    }

    private fun finishWith(entries: List<BlackBoxMediaEntry>) {
        if (entries.isEmpty()) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        val uris = entries.map { it.privateUri }
        val result = Intent().apply {
            data = uris.first()
            clipData = ClipData.newRawUri("shared-gallery", uris.first()).also { clip ->
                uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        setResult(Activity.RESULT_OK, result)
        finish()
    }
}
