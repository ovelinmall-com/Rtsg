package com.ovelin.space.ui

import android.app.ActivityManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ovelin.space.R
import com.ovelin.space.data.CloneFolderEntity
import com.ovelin.space.data.ImportedAppEntity
import com.ovelin.space.data.StorageSummary
import com.ovelin.space.data.VirtualInstanceEntity
import com.ovelin.space.engine.EngineStatus

@Composable
fun DashboardScreen(
    isArabic: Boolean,
    apps: List<ImportedAppEntity>,
    instances: List<VirtualInstanceEntity>,
    storage: StorageSummary?,
    engineStatus: EngineStatus,
    onImportInstalled: () -> Unit,
    onInstallApk: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenCopies: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val usedRam = remember(context) { usedRamBytes(context) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 12.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF3A0808), Color(0xFF1A0607), Color(0xFF0D0405)),
                            ),
                        )
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_app_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp)),
                        )
                        Text(
                            text = "Iskander",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = tr(
                            isArabic,
                            "استورد التطبيقات والملفات وأدرها محليًا.",
                            "Import and manage apps and files locally.",
                        ),
                        color = Color(0xFFE8D4D3),
                        fontSize = 14.sp,
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = tr(isArabic, "التطبيقات", "Imported apps"),
                        value = apps.size.toString(),
                        icon = Icons.Outlined.Apps,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = tr(isArabic, "النسخ الافتراضية", "Virtual copies"),
                        value = instances.size.toString(),
                        icon = Icons.Outlined.PhoneAndroid,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = tr(isArabic, "استهلاك المساحة", "Workspace used"),
                        value = formatBytes(storage?.workspaceBytes ?: 0L, isArabic),
                        icon = Icons.Outlined.Storage,
                        tint = Color(0xFFFF8A3D),
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = tr(isArabic, "الذاكرة المستخدمة", "RAM in use"),
                        value = usedRam?.let { formatBytes(it, isArabic) } ?: "—",
                        icon = Icons.Outlined.Memory,
                        tint = Color(0xFFE2302A),
                    )
                }
            }
        }
        item {
            Text(
                tr(isArabic, "الوصول السريع", "Quick actions"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ActionButton(
                        Modifier.weight(1f),
                        tr(isArabic, "استيراد تطبيق", "Import app"),
                        Icons.Outlined.Apps,
                        onImportInstalled,
                    )
                    ActionButton(
                        Modifier.weight(1f),
                        tr(isArabic, "اختيار APK", "Choose APK"),
                        Icons.Outlined.InstallMobile,
                        onInstallApk,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ActionButton(
                        Modifier.weight(1f),
                        tr(isArabic, "مدير التطبيقات", "App manager"),
                        Icons.Outlined.PhoneAndroid,
                        onOpenApps,
                    )
                    ActionButton(
                        Modifier.weight(1f),
                        tr(isArabic, "مدير الملفات", "File manager"),
                        Icons.Outlined.Folder,
                        onOpenFiles,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ActionButton(
                        Modifier.weight(1f),
                        tr(isArabic, "إدارة النسخ", "Copy manager"),
                        Icons.Outlined.Lock,
                        onOpenCopies,
                    )
                    ActionButton(
                        Modifier.weight(1f),
                        tr(isArabic, "الإعدادات", "Settings"),
                        Icons.AutoMirrored.Outlined.ArrowForward,
                        onOpenSettings,
                    )
                }
            }
        }
        item {
            Text(
                tr(isArabic, "أضيفت مؤخرًا", "Recently imported"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        if (apps.isEmpty()) {
            item {
                EmptyStateCard(
                    title = tr(isArabic, "لا توجد تطبيقات مستوردة", "No apps imported"),
                    message = tr(
                        isArabic,
                        "استورد تطبيقًا أو اختر ملف APK لحفظ معلوماته وملفاته داخل المساحة.",
                        "Import an installed app or choose an APK to store its details and files here.",
                    ),
                )
            }
        } else {
            items(apps.take(3), key = { it.id }) { app ->
                ImportedAppCard(app = app, isArabic = isArabic, onDelete = {})
            }
        }
    }
}

@Composable
fun AppsScreen(
    isArabic: Boolean,
    apps: List<ImportedAppEntity>,
    onImportInstalled: () -> Unit,
    onInstallApk: () -> Unit,
    onDelete: (ImportedAppEntity) -> Unit,
    onOpenDetails: (ImportedAppEntity) -> Unit,
    onClone: (ImportedAppEntity) -> Unit,
    engineAvailable: Boolean,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                tr(isArabic, "التطبيقات المستوردة", "Imported apps"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Button(
                    onClick = onImportInstalled,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text(tr(isArabic, "استيراد مثبت", "Import installed"))
                }
                OutlinedButton(onClick = onInstallApk, modifier = Modifier.weight(1f)) {
                    Text(tr(isArabic, "اختيار APK", "Choose APK"))
                }
            }
        }
        item {
            Text(
                tr(
                    isArabic,
                    "الاستيراد يحفظ ملفات التثبيت والبيانات الوصفية فقط، ولا ينسخ بيانات التطبيق الخاصة.",
                    "Import saves installation files and metadata; it does not copy private app data.",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (apps.isEmpty()) {
            item {
                EmptyStateCard(
                    tr(isArabic, "قائمة التطبيقات فارغة", "Your app list is empty"),
                    tr(isArabic, "ابدأ باستيراد تطبيق من الهاتف أو ملف APK.", "Import an app from the device or an APK file."),
                )
            }
        } else {
            items(apps, key = { it.id }) { app ->
                ImportedAppCard(
                    app,
                    isArabic,
                    onDelete = { onDelete(app) },
                    onOpen = { onOpenDetails(app) },
                    onClone = if (engineAvailable) ({ onClone(app) }) else null,
                )
            }
        }
    }
}

/**
 * A folder tile styled like Android's own app-group folders (a rounded square holding a small
 * grid preview of what's inside), but in the app's red brand instead of system gray — a folder
 * "on fire" rather than a plain stock icon. Shows up to 4 of the cloned apps' real icons.
 */
@Composable
fun FlamingFolderIcon(previewIconPaths: List<String?>, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size.value.dp * 0.28f))
            .background(
                Brush.linearGradient(listOf(IskanderRedDeep, IskanderRed, IskanderRedBright)),
            )
            .padding(size * 0.12f),
        contentAlignment = Alignment.Center,
    ) {
        if (previewIconPaths.isEmpty()) {
            Icon(
                Icons.Filled.Folder,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(size * 0.6f),
            )
        } else {
            val cell = (size - size * 0.24f) / 2f
            Column(verticalArrangement = Arrangement.spacedBy(size.value.dp * 0.06f)) {
                for (row in 0..1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(size.value.dp * 0.06f)) {
                        for (col in 0..1) {
                            val index = row * 2 + col
                            val path = previewIconPaths.getOrNull(index)
                            if (path != null || index < previewIconPaths.size) {
                                MiniAppTile(path, cell)
                            } else {
                                Box(Modifier.size(cell))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniAppTile(path: String?, size: Dp) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val image = remember(path) {
        path?.let {
            runCatching {
                val file = java.io.File(it)
                val resolved = if (file.isAbsolute) file else java.io.File(java.io.File(context.filesDir, "ovelin-space"), it)
                BitmapFactory.decodeFile(resolved.path)?.asImageBitmap()
            }.getOrNull()
        }
    }
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = Modifier.size(size).clip(RoundedCornerShape(size.value.dp * 0.22f)),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(size.value.dp * 0.22f))
                .background(Color.White.copy(alpha = 0.25f)),
        )
    }
}

@Composable
fun CopiesScreen(
    isArabic: Boolean,
    instances: List<VirtualInstanceEntity>,
    apps: List<ImportedAppEntity>,
    folders: List<CloneFolderEntity>,
    engineStatus: EngineStatus,
    remainingCapacity: Int,
    hasIdentity: (VirtualInstanceEntity) -> Boolean,
    onLaunch: (VirtualInstanceEntity) -> Unit,
    onStop: (VirtualInstanceEntity) -> Unit,
    onDelete: (VirtualInstanceEntity) -> Unit,
    onRenameFolder: (CloneFolderEntity) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                tr(isArabic, "مدير النسخ", "Copy manager"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            Text(
                tr(
                    isArabic,
                    "يمكن إنشاء نحو $remainingCapacity نسخة إضافية حسب المساحة المتاحة (الحد الفعلي يعتمد على التخزين والذاكرة).",
                    "About $remainingCapacity more instances fit in free storage (the real limit depends on storage and memory).",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (instances.isEmpty()) {
            item {
                EmptyStateCard(
                    tr(isArabic, "لا توجد نسخ افتراضية", "No virtual instances"),
                    tr(
                        isArabic,
                        "استورد تطبيقًا من تبويب التطبيقات ثم اضغط «نسخ» لإنشاء نسخة مستقلة.",
                        "Import an app from the Apps tab, then tap Clone to create an isolated instance.",
                    ),
                )
            }
        } else {
            val grouped = instances.groupBy { it.folderId }
            val orderedFolderIds = folders.map { it.id }.filter { grouped.containsKey(it) }
            for (folderId in orderedFolderIds) {
                val folder = folders.first { it.id == folderId }
                item(key = "folder-${folder.id}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clickable { onRenameFolder(folder) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val previewPaths = grouped.getValue(folderId).take(4).map { inst ->
                            apps.firstOrNull { it.id == inst.appId }?.iconPath
                        }
                        FlamingFolderIcon(previewIconPaths = previewPaths, size = 40.dp)
                        Text(
                            folder.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = tr(isArabic, "تعديل اسم المجلد", "Rename folder"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                items(grouped.getValue(folderId), key = { it.id }) { instance ->
                    InstanceCard(isArabic, instance, apps, hasIdentity, onLaunch, onStop, onDelete)
                }
            }
            val unfiled = grouped[null].orEmpty()
            if (unfiled.isNotEmpty()) {
                if (orderedFolderIds.isNotEmpty()) {
                    item(key = "unfiled-header") {
                        Text(
                            tr(isArabic, "بدون مجلد", "No folder"),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                items(unfiled, key = { it.id }) { instance ->
                    InstanceCard(isArabic, instance, apps, hasIdentity, onLaunch, onStop, onDelete)
                }
            }
        }
    }
}

@Composable
private fun InstanceCard(
    isArabic: Boolean,
    instance: VirtualInstanceEntity,
    apps: List<ImportedAppEntity>,
    hasIdentity: (VirtualInstanceEntity) -> Boolean,
    onLaunch: (VirtualInstanceEntity) -> Unit,
    onStop: (VirtualInstanceEntity) -> Unit,
    onDelete: (VirtualInstanceEntity) -> Unit,
) {
    val app = apps.firstOrNull { it.id == instance.appId }
    val running = instance.state == "RUNNING"
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppIcon(app?.iconPath, instance.displayName, 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(instance.displayName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    (if (running) tr(isArabic, "قيد التشغيل", "Running") else tr(isArabic, "جاهزة", "Ready")) +
                        if (hasIdentity(instance)) " · " + tr(isArabic, "هوية جهاز مخصّصة", "custom device identity") else "",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                androidx.compose.material3.TextButton(onClick = { onLaunch(instance) }) {
                    Text(tr(isArabic, "تشغيل", "Open"))
                }
                Row {
                    if (running) {
                        androidx.compose.material3.TextButton(onClick = { onStop(instance) }) {
                            Text(tr(isArabic, "إيقاف", "Stop"))
                        }
                    }
                    androidx.compose.material3.TextButton(onClick = { onDelete(instance) }) {
                        Text(tr(isArabic, "حذف", "Delete"), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    isArabic: Boolean,
    onLanguageChange: (String) -> Unit,
    engineStatus: EngineStatus,
    storage: StorageSummary?,
    onRunDiagnostics: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                tr(isArabic, "الإعدادات", "Settings"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            InfoCard(
                title = tr(isArabic, "اللغة", "Language"),
                message = tr(isArabic, "اختر لغة واجهة التطبيق.", "Choose the app interface language."),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LanguageButton("العربية", isArabic) { onLanguageChange("ar") }
                    LanguageButton("English", !isArabic) { onLanguageChange("en") }
                }
            }
        }
        item {
            InfoCard(
                title = tr(isArabic, "التخزين", "Storage"),
                message = tr(
                    isArabic,
                    "المستخدم داخل مساحة العمل: ${formatBytes(storage?.workspaceBytes ?: 0L, true)}\nالمتاح على الجهاز: ${formatBytes(storage?.deviceAvailableBytes ?: 0L, true)}",
                    "Workspace used: ${formatBytes(storage?.workspaceBytes ?: 0L, false)}\nAvailable on device: ${formatBytes(storage?.deviceAvailableBytes ?: 0L, false)}",
                ),
            )
        }
        item {
            InfoCard(
                title = tr(isArabic, "الخصوصية", "Privacy"),
                message = tr(
                    isArabic,
                    "ملفات APK والبيانات الوصفية تُحفظ محليًا داخل مساحة التطبيق. لا ننسخ بيانات التطبيقات المثبتة.",
                    "APK files and metadata stay in this app's private storage. Existing apps' private data is not copied.",
                ),
            )
        }
        item {
            InfoCard(
                title = tr(isArabic, "الإشعارات الافتراضية", "Virtual app notifications"),
                message = tr(
                    isArabic,
                    "تعتمد على المحرك والتطبيق؛ قد لا تظهر إشعارات بعض النسخ حتى تختبرها على جهازك.",
                    "Depends on the engine and the app; some instances may not show notifications until tested on your device.",
                ),
            )
        }
        item {
            val tapCount = androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
            Text(
                "Iskander · 0.4.3",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable {
                        tapCount.intValue++
                        if (tapCount.intValue >= 5) {
                            tapCount.intValue = 0
                            onRunDiagnostics()
                        }
                    },
            )
        }
    }
}

@Composable
fun ImportedAppCard(
    app: ImportedAppEntity,
    isArabic: Boolean,
    onDelete: () -> Unit,
    onOpen: (() -> Unit)? = null,
    onClone: (() -> Unit)? = null,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().then(
            if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier,
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppIcon(app.iconPath, app.appName, 48.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(app.appName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    app.packageName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${tr(isArabic, "الإصدار", "Version")} ${app.versionName} · ${tr(isArabic, "استيراد", "Source")}: ${if (app.sourceType == "INSTALLED") tr(isArabic, "الهاتف", "device") else "APK"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (onClone != null) {
                    androidx.compose.material3.TextButton(onClick = onClone) {
                        Text(tr(isArabic, "نسخ", "Clone"))
                    }
                }
                androidx.compose.material3.TextButton(onClick = onDelete) {
                    Text(tr(isArabic, "حذف", "Remove"), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun EngineStatusCard(isArabic: Boolean, engineStatus: EngineStatus) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF342C1B)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = Color(0xFFFFC46B))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    tr(isArabic, "محرك التشغيل الافتراضي ${if (engineStatus.available) "جاهز" else "غير متاح"} · ${engineStatus.engineName}", "Virtual runtime ${if (engineStatus.available) "ready" else "unavailable"} · ${engineStatus.engineName}"),
                    color = Color(0xFFFFD990),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (engineStatus.available) {
                        tr(
                            isArabic,
                            "التوافق يختلف من تطبيق لآخر؛ تطبيقات البنوك والتطبيقات التي تفحص سلامة الجهاز قد لا تعمل.",
                            "Compatibility varies per app; banking apps and apps that check device integrity may not work.",
                        )
                    } else {
                        tr(
                            isArabic,
                            "استيراد التطبيقات يعمل، لكن تشغيل نسخ مستقلة غير مفعّل في هذا الإصدار.",
                            "App import works; isolated virtual instances are not enabled in this build.",
                        )
                    },
                    color = Color(0xFFE7DCC7),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(title: String, message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    message: String,
    content: @Composable (() -> Unit)? = null,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            content?.invoke()
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ActionButton(
    modifier: Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(15.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(7.dp))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun RowScope.LanguageButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    if (isActive) {
        Button(onClick = onClick, modifier = Modifier.weight(1f)) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.weight(1f)) {
            Text(label)
        }
    }
}

@Composable
fun AppIcon(path: String?, label: String, size: Dp) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val image = remember(path) {
        path?.let {
            runCatching {
                val file = java.io.File(it)
                val resolved = if (file.isAbsolute) file else java.io.File(java.io.File(context.filesDir, "ovelin-space"), it)
                BitmapFactory.decodeFile(resolved.path)?.asImageBitmap()
            }.getOrNull()
        }
    }
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = label,
            modifier = Modifier.size(size).clip(RoundedCornerShape(13.dp)),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(13.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(label.firstOrNull()?.uppercase() ?: "A", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

private fun usedRamBytes(context: Context): Long? =
    runCatching {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        (info.totalMem - info.availMem).coerceAtLeast(0L)
    }.getOrNull()

fun formatBytes(bytes: Long, isArabic: Boolean): String {
    if (bytes <= 0) return if (isArabic) "0 بايت" else "0 B"
    val units = if (isArabic) listOf("بايت", "ك.ب", "م.ب", "ج.ب", "ت.ب") else listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    return "${"%.1f".format(java.util.Locale.ROOT, value)} ${units[unitIndex]}"
}

fun tr(isArabic: Boolean, arabic: String, english: String): String =
    if (isArabic) arabic else english
