package com.ovelin.space.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.ovelin.space.data.ImportedAppEntity
import com.ovelin.space.data.InstalledAppItem
import com.ovelin.space.data.StorageSummary
import com.ovelin.space.data.WorkspaceRepository
import com.ovelin.space.engine.DeviceIdentity
import com.ovelin.space.engine.EngineResult
import com.ovelin.space.engine.VirtualizationEngine
import androidx.compose.material3.Switch
import kotlinx.coroutines.launch

private enum class WorkspaceTab { HOME, APPS, COPIES, FILES, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OvelinSpaceApp(repository: WorkspaceRepository, engine: VirtualizationEngine) {
    val apps by repository.observeImportedApps().collectAsState(initial = emptyList())
    val instances by repository.observeInstances().collectAsState(initial = emptyList())
    val language by repository.observeSetting("language").collectAsState(initial = null)
    val isArabic = language?.value != "en"
    val engineStatus = engine.status
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTabName by rememberSaveable { mutableStateOf(WorkspaceTab.HOME.name) }
    var storage by remember { mutableStateOf<StorageSummary?>(null) }
    var storageRefresh by remember { mutableIntStateOf(0) }
    var installedPickerOpen by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var installedAppsLoaded by remember { mutableStateOf(false) }
    var installedAppsLoading by remember { mutableStateOf(false) }
    var installedAppsError by remember { mutableStateOf<String?>(null) }
    var installedSearch by rememberSaveable { mutableStateOf("") }
    var appToDelete by remember { mutableStateOf<ImportedAppEntity?>(null) }
    var appDetails by remember { mutableStateOf<ImportedAppEntity?>(null) }
    var appToClone by remember { mutableStateOf<ImportedAppEntity?>(null) }
    var cloneSpoof by remember { mutableStateOf(false) }
    var cloneManufacturer by remember { mutableStateOf("Samsung") }
    var cloneModel by remember { mutableStateOf("Galaxy S24") }
    var capacity by remember { mutableIntStateOf(0) }
    var diagnosticsText by remember { mutableStateOf<String?>(null) }
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val selectedTab = runCatching { WorkspaceTab.valueOf(selectedTabName) }.getOrDefault(WorkspaceTab.HOME)

    LaunchedEffect(storageRefresh, apps.size, instances.size, selectedTab) {
        storage = runCatching { repository.storageSummary() }.getOrNull()
        capacity = runCatching { engine.remainingCapacity() }.getOrDefault(0)
    }

    fun runEngine(action: suspend () -> EngineResult) {
        runTask {
            when (val result = action()) {
                is EngineResult.Completed -> result.message
                is EngineResult.Unsupported -> result.reason
            }
        }
    }

    fun runTask(action: suspend () -> String) {
        scope.launch {
            val message = try {
                action().also { storageRefresh++ }
            } catch (error: Exception) {
                error.message ?: tr(isArabic, "تعذر إكمال العملية.", "The operation could not be completed.")
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    fun openInstalledPicker() {
        installedPickerOpen = true
        installedAppsError = null
        if (!installedAppsLoaded && !installedAppsLoading) {
            installedAppsLoading = true
            scope.launch {
                try {
                    installedApps = repository.listInstalledApps()
                    installedAppsLoaded = true
                } catch (error: Exception) {
                    installedAppsError = error.message
                        ?: tr(isArabic, "تعذر قراءة التطبيقات المثبتة.", "Could not read installed apps.")
                } finally {
                    installedAppsLoading = false
                }
            }
        }
    }

    val apkPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runTask {
                val imported = repository.importApk(uri)
                tr(isArabic, "تم استيراد ${imported.appName} وملف APK.", "${imported.appName} and its APK were imported.")
            }
        }
    }

    val tabTitle = when (selectedTab) {
        WorkspaceTab.HOME -> tr(isArabic, "لوحة التحكم", "Dashboard")
        WorkspaceTab.APPS -> tr(isArabic, "التطبيقات", "Apps")
        WorkspaceTab.COPIES -> tr(isArabic, "النسخ", "Instances")
        WorkspaceTab.FILES -> tr(isArabic, "الملفات", "Files")
        WorkspaceTab.SETTINGS -> tr(isArabic, "الإعدادات", "Settings")
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "OVELIN SPACE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            )
                            Text(
                                tabTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                val nextLanguage = if (isArabic) "en" else "ar"
                                runTask {
                                    repository.setSetting("language", nextLanguage)
                                    if (nextLanguage == "en") "Language changed to English." else "تم تغيير اللغة إلى العربية."
                                }
                            },
                        ) {
                            Text(if (isArabic) "EN" else "عربي")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    val tabs = listOf(
                        WorkspaceTab.HOME,
                        WorkspaceTab.APPS,
                        WorkspaceTab.COPIES,
                        WorkspaceTab.FILES,
                        WorkspaceTab.SETTINGS,
                    )
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTabName = tab.name },
                            icon = {
                                Icon(
                                    imageVector = tabIcon(tab),
                                    contentDescription = tabLabel(tab, isArabic),
                                )
                            },
                            label = { Text(tabLabel(tab, isArabic), maxLines = 1) },
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (selectedTab) {
                    WorkspaceTab.HOME -> DashboardScreen(
                        isArabic = isArabic,
                        apps = apps,
                        instances = instances,
                        storage = storage,
                        engineStatus = engineStatus,
                        onImportInstalled = { openInstalledPicker() },
                        onInstallApk = {
                            apkPicker.launch(
                                arrayOf(
                                    "application/vnd.android.package-archive",
                                    "application/octet-stream",
                                ),
                            )
                        },
                        onOpenApps = { selectedTabName = WorkspaceTab.APPS.name },
                        onOpenCopies = { selectedTabName = WorkspaceTab.COPIES.name },
                        onOpenFiles = { selectedTabName = WorkspaceTab.FILES.name },
                        onOpenSettings = { selectedTabName = WorkspaceTab.SETTINGS.name },
                    )
                    WorkspaceTab.APPS -> AppsScreen(
                        isArabic = isArabic,
                        apps = apps,
                        onImportInstalled = { openInstalledPicker() },
                        onInstallApk = {
                            apkPicker.launch(
                                arrayOf(
                                    "application/vnd.android.package-archive",
                                    "application/octet-stream",
                                ),
                            )
                        },
                        onDelete = { appToDelete = it },
                        onOpenDetails = { appDetails = it },
                        onClone = { appToClone = it },
                        engineAvailable = engineStatus.available,
                    )
                    WorkspaceTab.COPIES -> CopiesScreen(
                        isArabic = isArabic,
                        instances = instances,
                        apps = apps,
                        engineStatus = engineStatus,
                        remainingCapacity = capacity,
                        hasIdentity = { engine.hasIdentity(it) },
                        onLaunch = { runEngine { engine.launchInstance(it) } },
                        onStop = { runEngine { engine.stopInstance(it) } },
                        onDelete = { runEngine { engine.deleteInstance(it) } },
                    )
                    WorkspaceTab.FILES -> FileManagerScreen(
                        repository = repository,
                        isArabic = isArabic,
                        snackbarHostState = snackbarHostState,
                    )
                    WorkspaceTab.SETTINGS -> SettingsScreen(
                        isArabic = isArabic,
                        onLanguageChange = { next ->
                            runTask {
                                repository.setSetting("language", next)
                                tr(next == "ar", "تم حفظ اللغة.", "Language saved.")
                            }
                        },
                        engineStatus = engineStatus,
                        storage = storage,
                        onRunDiagnostics = {
                            scope.launch {
                                diagnosticsText = tr(isArabic, "جارٍ الفحص…", "Running…")
                                diagnosticsText = runCatching { engine.diagnostics() }
                                    .getOrElse { "Diagnostics failed: ${it.message}" }
                            }
                        },
                    )
                }
            }
        }
    }

    if (installedPickerOpen) {
        InstalledAppsDialog(
            isArabic = isArabic,
            apps = installedApps,
            loading = installedAppsLoading,
            error = installedAppsError,
            query = installedSearch,
            onQueryChange = { installedSearch = it },
            onDismiss = { installedPickerOpen = false },
            onRetry = {
                installedAppsLoaded = false
                openInstalledPicker()
            },
            onSelect = { app ->
                installedPickerOpen = false
                runTask {
                    val imported = repository.importInstalledApp(app)
                    tr(isArabic, "تم استيراد ${imported.appName}.", "${imported.appName} was imported.")
                }
            },
        )
    }

    diagnosticsText?.let { report ->
        AlertDialog(
            onDismissRequest = { diagnosticsText = null },
            title = { Text(tr(isArabic, "نتيجة الفحص", "Self-check result")) },
            text = {
                Text(
                    report,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier
                        .heightIn(max = 380.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(report))
                        scope.launch { snackbarHostState.showSnackbar(tr(isArabic, "تم النسخ.", "Copied.")) }
                    },
                ) { Text(tr(isArabic, "نسخ", "Copy")) }
            },
            dismissButton = {
                TextButton(onClick = { diagnosticsText = null }) { Text(tr(isArabic, "إغلاق", "Close")) }
            },
        )
    }

    appToClone?.let { app ->
        AlertDialog(
            onDismissRequest = { appToClone = null },
            title = { Text(tr(isArabic, "نسخ ${app.appName}", "Clone ${app.appName}")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        tr(
                            isArabic,
                            "ستُنشأ نسخة مستقلة ببياناتها. يمكنك إنشاء أكثر من نسخة حسب المساحة المتاحة.",
                            "An isolated copy with its own data will be created. You can create several, within free storage.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Switch(checked = cloneSpoof, onCheckedChange = { cloneSpoof = it })
                        Text(tr(isArabic, "تغيير هوية الجهاز لهذه النسخة", "Change device identity for this copy"))
                    }
                    if (cloneSpoof) {
                        OutlinedTextField(
                            value = cloneManufacturer,
                            onValueChange = { cloneManufacturer = it },
                            label = { Text(tr(isArabic, "الشركة المصنّعة", "Manufacturer")) },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = cloneModel,
                            onValueChange = { cloneModel = it },
                            label = { Text(tr(isArabic, "الموديل", "Model")) },
                            singleLine = true,
                        )
                        Text(
                            tr(
                                isArabic,
                                "سيُنشأ Android ID مستقل لهذه النسخة. بعض التطبيقات تمنع الحسابات عند اكتشاف تغيير الهوية.",
                                "A separate Android ID is generated for this copy. Some apps restrict accounts when they detect identity changes.",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = app
                        val identity = if (cloneSpoof && cloneManufacturer.isNotBlank() && cloneModel.isNotBlank()) {
                            DeviceIdentity(cloneManufacturer.trim(), cloneModel.trim())
                        } else {
                            null
                        }
                        val number = instances.count { it.appId == target.id } + 1
                        appToClone = null
                        runEngine { engine.createInstance(target, "${target.appName} $number", identity) }
                    },
                ) { Text(tr(isArabic, "إنشاء", "Create")) }
            },
            dismissButton = {
                TextButton(onClick = { appToClone = null }) { Text(tr(isArabic, "إلغاء", "Cancel")) }
            },
        )
    }

    appToDelete?.let { app ->
        AlertDialog(
            onDismissRequest = { appToDelete = null },
            title = { Text(tr(isArabic, "إزالة التطبيق المستورد؟", "Remove imported app?")) },
            text = {
                Text(
                    tr(
                        isArabic,
                        "سيتم حذف نسخة الملفات المحفوظة داخل OVELIN SPACE فقط. التطبيق الأصلي على الهاتف لن يتأثر.",
                        "Only the stored copy inside OVELIN SPACE will be removed. The app installed on the phone is unchanged.",
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        appToDelete = null
                        runTask {
                            repository.deleteImportedApp(app)
                            tr(isArabic, "تم حذف التطبيق المستورد.", "Imported app removed.")
                        }
                    },
                ) {
                    Text(tr(isArabic, "حذف", "Remove"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { appToDelete = null }) { Text(tr(isArabic, "إلغاء", "Cancel")) }
            },
        )
    }

    appDetails?.let { app ->
        AlertDialog(
            onDismissRequest = { appDetails = null },
            title = { Text(app.appName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Text(
                    "${tr(isArabic, "اسم الحزمة", "Package")}: ${app.packageName}\n" +
                        "${tr(isArabic, "الإصدار", "Version")}: ${app.versionName} (${app.versionCode})\n" +
                        "${tr(isArabic, "بصمة شهادة التوقيع SHA-256", "Signer certificate SHA-256")}: ${app.signerSha256 ?: "—"}\n" +
                        "${tr(isArabic, "ملفات APK", "APK files")}: ${app.apkPaths.lines().size}",
                )
            },
            confirmButton = {
                TextButton(onClick = { appDetails = null }) { Text(tr(isArabic, "إغلاق", "Close")) }
            },
        )
    }
}

@Composable
private fun InstalledAppsDialog(
    isArabic: Boolean,
    apps: List<InstalledAppItem>,
    loading: Boolean,
    error: String?,
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onSelect: (InstalledAppItem) -> Unit,
) {
    val filteredApps = remember(apps, query) {
        apps.filter {
            query.isBlank() ||
                it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr(isArabic, "استيراد تطبيق مثبت", "Import installed app")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    tr(
                        isArabic,
                        "سيتم حفظ ملفات APK فقط، وليس بيانات التطبيق الخاصة.",
                        "Only APK files are stored, not private app data.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(tr(isArabic, "ابحث عن تطبيق", "Search apps")) },
                )
                if (loading) {
                    Box(
                        Modifier.fillMaxWidth().heightIn(min = 100.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (error != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(error, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetry) { Text(tr(isArabic, "إعادة المحاولة", "Retry")) }
                    }
                } else if (filteredApps.isEmpty()) {
                    Text(
                        tr(isArabic, "لا توجد نتائج.", "No matching apps."),
                        modifier = Modifier.padding(vertical = 20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 390.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            InstalledAppRow(app, onSelect)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(tr(isArabic, "إغلاق", "Close")) }
        },
    )
}

@Composable
private fun InstalledAppRow(
    app: InstalledAppItem,
    onSelect: (InstalledAppItem) -> Unit,
) {
    val context = LocalContext.current
    val icon = remember(app.packageName) {
        runCatching {
            app.applicationInfo.loadIcon(context.packageManager)
                .toBitmap(64, 64)
                .asImageBitmap()
        }.getOrNull()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(app) }
            .padding(vertical = 7.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Image(icon, contentDescription = app.label, modifier = Modifier.size(42.dp))
        } else {
            Icon(Icons.Outlined.Apps, contentDescription = null, modifier = Modifier.size(42.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(app.label, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                app.packageName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun tabIcon(tab: WorkspaceTab) = when (tab) {
    WorkspaceTab.HOME -> Icons.Outlined.Home
    WorkspaceTab.APPS -> Icons.Outlined.Apps
    WorkspaceTab.COPIES -> Icons.Outlined.Layers
    WorkspaceTab.FILES -> Icons.Outlined.Folder
    WorkspaceTab.SETTINGS -> Icons.Outlined.Settings
}

private fun tabLabel(tab: WorkspaceTab, isArabic: Boolean) = when (tab) {
    WorkspaceTab.HOME -> tr(isArabic, "الرئيسية", "Home")
    WorkspaceTab.APPS -> tr(isArabic, "التطبيقات", "Apps")
    WorkspaceTab.COPIES -> tr(isArabic, "النسخ", "Copies")
    WorkspaceTab.FILES -> tr(isArabic, "الملفات", "Files")
    WorkspaceTab.SETTINGS -> tr(isArabic, "الإعدادات", "Settings")
}
