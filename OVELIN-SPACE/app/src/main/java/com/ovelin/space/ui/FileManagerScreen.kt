package com.ovelin.space.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextField
import com.ovelin.space.data.WorkspaceFile
import com.ovelin.space.data.WorkspaceRepository
import kotlinx.coroutines.launch

@Composable
fun FileManagerScreen(
    repository: WorkspaceRepository,
    isArabic: Boolean,
    snackbarHostState: SnackbarHostState,
) {
    val scope = rememberCoroutineScope()
    var currentPath by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    var refresh by remember { mutableStateOf(0) }
    var files by remember { mutableStateOf<List<WorkspaceFile>>(emptyList()) }
    var createFolderOpen by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<WorkspaceFile?>(null) }
    var deleteTarget by remember { mutableStateOf<WorkspaceFile?>(null) }
    var detailsTarget by remember { mutableStateOf<WorkspaceFile?>(null) }
    var nameInput by remember { mutableStateOf("") }
    var loadingError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentPath, search, refresh) {
        loadingError = null
        runCatching {
            files = if (search.isBlank()) {
                repository.listWorkspaceFiles(currentPath)
            } else {
                repository.searchWorkspaceFiles(search)
            }
        }.onFailure { loadingError = it.message ?: tr(isArabic, "تعذر تحميل الملفات.", "Could not load files.") }
    }

    fun runFileAction(success: String, action: suspend () -> Unit) {
        scope.launch {
            try {
                action()
                refresh++
                snackbarHostState.showSnackbar(success)
            } catch (error: Exception) {
                snackbarHostState.showSnackbar(
                    error.message ?: tr(isArabic, "تعذر تنفيذ العملية.", "The operation failed."),
                )
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentPath.isNotBlank() && search.isBlank()) {
                    IconButton(
                        onClick = {
                            currentPath = currentPath.substringBeforeLast('/', missingDelimiterValue = "")
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = tr(isArabic, "رجوع", "Back"))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        tr(isArabic, "مدير الملفات", "File manager"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (search.isNotBlank()) {
                            tr(isArabic, "نتائج البحث في مساحة العمل", "Search results in workspace")
                        } else {
                            currentPath.ifBlank { tr(isArabic, "مساحة العمل", "Workspace") }
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { createFolderOpen = true }) {
                    Icon(Icons.Outlined.CreateNewFolder, contentDescription = tr(isArabic, "مجلد جديد", "New folder"))
                }
            }
        }
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text(tr(isArabic, "ابحث في الملفات", "Search files")) },
                shape = RoundedCornerShape(15.dp),
            )
        }
        if (loadingError != null) {
            item {
                EmptyStateCard(
                    tr(isArabic, "تعذر عرض المجلد", "Could not open this folder"),
                    loadingError.orEmpty(),
                )
            }
        } else if (files.isEmpty()) {
            item {
                EmptyStateCard(
                    tr(isArabic, "المجلد فارغ", "This folder is empty"),
                    tr(isArabic, "أنشئ مجلدًا أو استورد تطبيقًا لإضافة ملفات.", "Create a folder or import an app to add files."),
                )
            }
        } else {
            items(files, key = { it.relativePath }) { file ->
                WorkspaceFileRow(
                    file = file,
                    isArabic = isArabic,
                    onOpen = {
                        if (file.isDirectory && search.isBlank()) currentPath = file.relativePath
                        else detailsTarget = file
                    },
                    onAction = { action ->
                        when (action) {
                            FileAction.RENAME -> {
                                nameInput = file.name
                                renameTarget = file
                            }
                            FileAction.COPY -> runFileAction(
                                tr(isArabic, "تم إنشاء نسخة من العنصر.", "Item copied."),
                            ) { repository.copyFile(file.relativePath) }
                            FileAction.MOVE_ROOT -> runFileAction(
                                tr(isArabic, "تم نقل العنصر للمجلد الرئيسي.", "Item moved to workspace root."),
                            ) { repository.moveFileToRoot(file.relativePath) }
                            FileAction.DELETE -> deleteTarget = file
                            FileAction.DETAILS -> detailsTarget = file
                        }
                    },
                )
            }
        }
    }

    if (createFolderOpen) {
        NameDialog(
            isArabic = isArabic,
            title = tr(isArabic, "إنشاء مجلد", "Create folder"),
            confirmLabel = tr(isArabic, "إنشاء", "Create"),
            value = nameInput,
            onValueChange = { nameInput = it },
            onDismiss = { createFolderOpen = false; nameInput = "" },
            onConfirm = {
                val name = nameInput
                createFolderOpen = false
                nameInput = ""
                runFileAction(tr(isArabic, "تم إنشاء المجلد.", "Folder created.")) {
                    repository.createFolder(currentPath, name)
                }
            },
        )
    }

    renameTarget?.let { target ->
        NameDialog(
            isArabic = isArabic,
            title = tr(isArabic, "إعادة تسمية", "Rename"),
            confirmLabel = tr(isArabic, "حفظ", "Save"),
            value = nameInput,
            onValueChange = { nameInput = it },
            onDismiss = { renameTarget = null; nameInput = "" },
            onConfirm = {
                val newName = nameInput
                renameTarget = null
                nameInput = ""
                runFileAction(tr(isArabic, "تم تحديث الاسم.", "Name updated.")) {
                    repository.renameFile(target.relativePath, newName)
                }
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(tr(isArabic, "حذف العنصر؟", "Delete item?")) },
            text = {
                Text(
                    tr(
                        isArabic,
                        "سيتم حذف «${target.name}» من مساحة OVELIN SPACE فقط.",
                        "“${target.name}” will be deleted from OVELIN SPACE only.",
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteTarget = null
                        runFileAction(tr(isArabic, "تم حذف العنصر.", "Item deleted.")) {
                            repository.deleteWorkspaceFile(target.relativePath)
                        }
                    },
                ) {
                    Text(tr(isArabic, "حذف", "Delete"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(tr(isArabic, "إلغاء", "Cancel"))
                }
            },
        )
    }

    detailsTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { detailsTarget = null },
            title = { Text(target.name) },
            text = {
                Text(
                    "${if (target.isDirectory) tr(isArabic, "مجلد", "Folder") else tr(isArabic, "ملف", "File")}\n" +
                        "${tr(isArabic, "المسار", "Path")}: ${target.relativePath}\n" +
                        "${tr(isArabic, "الحجم", "Size")}: ${formatBytes(target.bytes, isArabic)}",
                )
            },
            confirmButton = {
                TextButton(onClick = { detailsTarget = null }) { Text(tr(isArabic, "إغلاق", "Close")) }
            },
        )
    }
}

private enum class FileAction { RENAME, COPY, MOVE_ROOT, DETAILS, DELETE }

@Composable
private fun WorkspaceFileRow(
    file: WorkspaceFile,
    isArabic: Boolean,
    onOpen: () -> Unit,
    onAction: (FileAction) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 13.dp, end = 4.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Icon(
                if (file.isDirectory) Icons.Outlined.Folder else Icons.Outlined.InsertDriveFile,
                contentDescription = null,
                tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(23.dp),
            )
            Column(
                modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(file.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (file.isDirectory) {
                        "${tr(isArabic, "مجلد", "Folder")} · ${formatBytes(file.bytes, isArabic)}"
                    } else {
                        "${tr(isArabic, "ملف", "File")} · ${formatBytes(file.bytes, isArabic)}"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
            TextButton(onClick = onOpen) {
                Text(if (file.isDirectory) tr(isArabic, "فتح", "Open") else tr(isArabic, "تفاصيل", "Details"))
            }
            androidx.compose.foundation.layout.Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = tr(isArabic, "خيارات", "Options"))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(tr(isArabic, "إعادة تسمية", "Rename")) },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = { menuOpen = false; onAction(FileAction.RENAME) },
                    )
                    DropdownMenuItem(
                        text = { Text(tr(isArabic, "نسخ", "Copy")) },
                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                        onClick = { menuOpen = false; onAction(FileAction.COPY) },
                    )
                    DropdownMenuItem(
                        text = { Text(tr(isArabic, "نقل للرئيسي", "Move to root")) },
                        leadingIcon = { Icon(Icons.Outlined.DriveFileMove, contentDescription = null) },
                        onClick = { menuOpen = false; onAction(FileAction.MOVE_ROOT) },
                    )
                    DropdownMenuItem(
                        text = { Text(tr(isArabic, "تفاصيل", "Details")) },
                        leadingIcon = { Icon(Icons.Outlined.FolderOpen, contentDescription = null) },
                        onClick = { menuOpen = false; onAction(FileAction.DETAILS) },
                    )
                    DropdownMenuItem(
                        text = { Text(tr(isArabic, "حذف", "Delete"), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = { menuOpen = false; onAction(FileAction.DELETE) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NameDialog(
    isArabic: Boolean,
    title: String,
    confirmLabel: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                placeholder = { Text(title) },
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr(isArabic, "إلغاء", "Cancel"))
            }
        },
    )
}
