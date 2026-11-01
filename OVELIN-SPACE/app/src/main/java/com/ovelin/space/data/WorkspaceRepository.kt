package com.ovelin.space.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.StatFs
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val packageName: String,
    val label: String,
    val versionName: String,
    val versionCode: Long,
    val applicationInfo: ApplicationInfo,
)

data class WorkspaceFile(
    val name: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val bytes: Long,
)

data class StorageSummary(
    val workspaceBytes: Long,
    val deviceAvailableBytes: Long,
)

class WorkspaceRepository(
    private val context: Context,
    private val dao: WorkspaceDao,
) {
    private val packageManager = context.packageManager
    private val workspaceRoot = File(context.filesDir, "ovelin-space").apply { mkdirs() }

    fun observeImportedApps(): Flow<List<ImportedAppEntity>> = dao.observeImportedApps()

    fun observeInstances(): Flow<List<VirtualInstanceEntity>> = dao.observeInstances()

    fun observeSetting(key: String): Flow<WorkspaceSettingEntity?> = dao.observeSetting(key)

    suspend fun setSetting(key: String, value: String) {
        dao.upsertSetting(WorkspaceSettingEntity(key, value, System.currentTimeMillis()))
    }

    suspend fun listInstalledApps(): List<InstalledAppItem> = withContext(Dispatchers.IO) {
        val applications = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(0)
        }
        applications.asSequence()
            .filterNot { it.packageName == context.packageName }
            .mapNotNull { application ->
                runCatching {
                    val info = getPackageInfo(application.packageName)
                    InstalledAppItem(
                        packageName = application.packageName,
                        label = application.loadLabel(packageManager).toString(),
                        versionName = info.versionName ?: "—",
                        versionCode = versionCode(info),
                        applicationInfo = application,
                    )
                }.getOrNull()
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    suspend fun importInstalledApp(app: InstalledAppItem): ImportedAppEntity =
        withContext(Dispatchers.IO) {
            val packageInfo = getPackageInfo(app.packageName)
            val appDirectory = appDirectory(app.packageName).apply { mkdirs() }
            val sources = buildList {
                add(File(app.applicationInfo.sourceDir))
                app.applicationInfo.splitSourceDirs?.forEach { add(File(it)) }
            }
            require(sources.isNotEmpty() && sources.first().isFile) {
                "ملف APK الأساسي غير متاح لهذا التطبيق."
            }
            val relativePaths = sources.mapIndexed { index, source ->
                require(source.isFile) { "ملف تثبيت إضافي غير متاح: ${source.name}" }
                val destination = File(appDirectory, if (index == 0) "base.apk" else "split_$index.apk")
                source.copyTo(destination, overwrite = true)
                relativeToWorkspace(destination)
            }
            val icon = runCatching { saveIcon(app.packageName, app.applicationInfo.loadIcon(packageManager)) }
                .getOrNull()
            val record = ImportedAppEntity(
                id = app.packageName,
                packageName = app.packageName,
                appName = app.label,
                versionName = app.versionName,
                versionCode = app.versionCode,
                apkPaths = relativePaths.joinToString("\n"),
                iconPath = icon,
                signerSha256 = signerFingerprint(packageInfo),
                sourceType = "INSTALLED",
                importedAt = System.currentTimeMillis(),
            )
            dao.upsertImportedApp(record)
            dao.insertImportLog(
                ImportLogEntity(
                    id = UUID.randomUUID().toString(),
                    packageName = app.packageName,
                    sourceType = "INSTALLED",
                    createdAt = System.currentTimeMillis(),
                    succeeded = true,
                    detail = "تم حفظ ملفات APK المتاحة دون نسخ بيانات التطبيق.",
                ),
            )
            record
        }

    suspend fun importApk(uri: Uri): ImportedAppEntity = withContext(Dispatchers.IO) {
        var detectedPackage: String? = null
        val temporaryFile = File(context.cacheDir, "apk-${UUID.randomUUID()}.apk")
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("تعذر فتح ملف APK المحدد.")
            input.use { source ->
                temporaryFile.outputStream().use { destination ->
                    copyWithLimit(source, destination, MAX_APK_BYTES)
                }
            }
            require(temporaryFile.length() > 0) { "ملف APK فارغ." }

            val packageInfo = getArchiveInfo(temporaryFile.absolutePath)
                ?: throw IllegalArgumentException("الملف غير صالح أو لا يحتوي على APK قابل للقراءة.")
            val applicationInfo = packageInfo.applicationInfo
                ?: throw IllegalArgumentException("تعذر قراءة بيانات التطبيق من APK.")
            detectedPackage = packageInfo.packageName
            require(!detectedPackage.isNullOrBlank()) { "اسم الحزمة غير موجود في APK." }
            applicationInfo.sourceDir = temporaryFile.absolutePath
            applicationInfo.publicSourceDir = temporaryFile.absolutePath

            val fingerprint = signerFingerprint(packageInfo)
                ?: throw IllegalArgumentException("تعذر قراءة شهادة توقيع APK؛ لم يتم استيراده.")
            val name = applicationInfo.loadLabel(packageManager).toString().ifBlank { detectedPackage }
            val appDirectory = appDirectory(detectedPackage!!).apply { mkdirs() }
            val savedApk = File(appDirectory, "base.apk")
            temporaryFile.copyTo(savedApk, overwrite = true)
            val icon = runCatching { saveIcon(detectedPackage!!, applicationInfo.loadIcon(packageManager)) }
                .getOrNull()
            val record = ImportedAppEntity(
                id = detectedPackage!!,
                packageName = detectedPackage!!,
                appName = name,
                versionName = packageInfo.versionName ?: "—",
                versionCode = versionCode(packageInfo),
                apkPaths = relativeToWorkspace(savedApk),
                iconPath = icon,
                signerSha256 = fingerprint,
                sourceType = "APK_FILE",
                importedAt = System.currentTimeMillis(),
            )
            dao.upsertImportedApp(record)
            dao.insertImportLog(
                ImportLogEntity(
                    id = UUID.randomUUID().toString(),
                    packageName = detectedPackage,
                    sourceType = "APK_FILE",
                    createdAt = System.currentTimeMillis(),
                    succeeded = true,
                    detail = "تم تحليل بيانات الحزمة وحفظ APK وبصمة شهادة التوقيع.",
                ),
            )
            record
        } catch (error: Exception) {
            dao.insertImportLog(
                ImportLogEntity(
                    id = UUID.randomUUID().toString(),
                    packageName = detectedPackage,
                    sourceType = "APK_FILE",
                    createdAt = System.currentTimeMillis(),
                    succeeded = false,
                    detail = error.message ?: "فشل استيراد APK.",
                ),
            )
            throw error
        } finally {
            temporaryFile.delete()
        }
    }

    suspend fun deleteImportedApp(app: ImportedAppEntity) = withContext(Dispatchers.IO) {
        require(dao.countInstances(app.id) == 0) {
            "لا يمكن حذف التطبيق ما دامت له نسخ مرتبطة."
        }
        appDirectory(app.packageName).deleteRecursively()
        app.iconPath?.let(::File)?.delete()
        dao.deleteImportedApp(app)
    }

    suspend fun listWorkspaceFiles(relativeDirectory: String = ""): List<WorkspaceFile> =
        withContext(Dispatchers.IO) {
            val directory = resolveWorkspacePath(relativeDirectory)
            require(directory.isDirectory) { "المسار المحدد ليس مجلدًا." }
            directory.listFiles().orEmpty()
                .filterNot { it.name.startsWith(".") }
                .map { file ->
                    WorkspaceFile(
                        name = file.name,
                        relativePath = relativeToWorkspace(file),
                        isDirectory = file.isDirectory,
                        bytes = if (file.isDirectory) directoryBytes(file) else file.length(),
                    )
                }
                .sortedWith(compareByDescending<WorkspaceFile> { it.isDirectory }.thenBy { it.name.lowercase() })
        }

    suspend fun searchWorkspaceFiles(query: String): List<WorkspaceFile> = withContext(Dispatchers.IO) {
        val normalized = query.trim()
        if (normalized.isEmpty()) return@withContext listWorkspaceFiles()
        workspaceRoot.walkTopDown()
            .filter { it != workspaceRoot && it.name.contains(normalized, ignoreCase = true) }
            .map { file ->
                WorkspaceFile(
                    name = file.name,
                    relativePath = relativeToWorkspace(file),
                    isDirectory = file.isDirectory,
                    bytes = if (file.isDirectory) directoryBytes(file) else file.length(),
                )
            }
            .take(MAX_SEARCH_RESULTS)
            .toList()
    }

    suspend fun createFolder(parentPath: String, name: String) = withContext(Dispatchers.IO) {
        val safeName = validateName(name)
        val parent = resolveWorkspacePath(parentPath)
        require(parent.isDirectory) { "المجلد الحالي غير موجود." }
        val folder = File(parent, safeName)
        require(folder.mkdir()) { "تعذر إنشاء المجلد؛ قد يكون الاسم مستخدمًا." }
    }

    suspend fun renameFile(relativePath: String, newName: String) = withContext(Dispatchers.IO) {
        val source = resolveWorkspacePath(relativePath)
        require(source != workspaceRoot && source.exists()) { "الملف غير موجود." }
        val destination = File(source.parentFile ?: workspaceRoot, validateName(newName))
        require(!destination.exists()) { "يوجد ملف أو مجلد بهذا الاسم." }
        require(source.renameTo(destination)) { "تعذر إعادة تسمية العنصر." }
    }

    suspend fun copyFile(relativePath: String) = withContext(Dispatchers.IO) {
        val source = resolveWorkspacePath(relativePath)
        require(source != workspaceRoot && source.exists()) { "الملف غير موجود." }
        val destination = uniqueSibling(source, "نسخة")
        if (source.isDirectory) {
            source.copyRecursively(destination, overwrite = false)
        } else {
            source.copyTo(destination, overwrite = false)
        }
    }

    suspend fun moveFileToRoot(relativePath: String) = withContext(Dispatchers.IO) {
        val source = resolveWorkspacePath(relativePath)
        require(source != workspaceRoot && source.exists()) { "الملف غير موجود." }
        require(source.parentFile?.canonicalFile != workspaceRoot.canonicalFile) {
            "العنصر موجود أصلًا في المجلد الرئيسي."
        }
        val destination = uniqueSiblingInRoot(source.name)
        require(source.renameTo(destination)) { "تعذر نقل العنصر إلى المجلد الرئيسي." }
    }

    suspend fun deleteWorkspaceFile(relativePath: String) = withContext(Dispatchers.IO) {
        val target = resolveWorkspacePath(relativePath)
        require(target != workspaceRoot && target.exists()) { "العنصر غير موجود أو لا يمكن حذفه." }
        require(target.deleteRecursively()) { "تعذر حذف العنصر." }
    }

    suspend fun storageSummary(): StorageSummary = withContext(Dispatchers.IO) {
        val stat = StatFs(context.filesDir.absolutePath)
        StorageSummary(
            workspaceBytes = directoryBytes(workspaceRoot),
            deviceAvailableBytes = stat.availableBytes,
        )
    }

    private fun appDirectory(packageName: String): File {
        val safePackage = packageName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(File(workspaceRoot, "apps"), safePackage)
    }

    private fun getPackageInfo(packageName: String): PackageInfo {
        @Suppress("DEPRECATION")
        return if (Build.VERSION.SDK_INT >= 33) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
            )
        } else {
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        }
    }

    private fun getArchiveInfo(path: String): PackageInfo? {
        @Suppress("DEPRECATION")
        return if (Build.VERSION.SDK_INT >= 33) {
            packageManager.getPackageArchiveInfo(
                path,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
            )
        } else {
            packageManager.getPackageArchiveInfo(path, PackageManager.GET_SIGNING_CERTIFICATES)
        }
    }

    private fun signerFingerprint(info: PackageInfo): String? {
        val signers = if (Build.VERSION.SDK_INT >= 28) {
            info.signingInfo?.apkContentsSigners?.toList()
        } else {
            @Suppress("DEPRECATION")
            info.signatures?.toList()
        }
        val bytes = signers?.firstOrNull()?.toByteArray() ?: return null
        return MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { byte -> "%02X".format(byte) }
    }

    private fun versionCode(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= 28) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }

    private fun saveIcon(packageName: String, drawable: Drawable): String {
        val iconDirectory = File(workspaceRoot, "icons").apply { mkdirs() }
        val fileName = packageName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val iconFile = File(iconDirectory, "$fileName.png")
        val bitmap = drawable.toBitmap(96, 96, Bitmap.Config.ARGB_8888)
        iconFile.outputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "تعذر حفظ أيقونة التطبيق."
            }
        }
        bitmap.recycle()
        return relativeToWorkspace(iconFile)
    }

    private fun copyWithLimit(input: InputStream, output: java.io.OutputStream, maxBytes: Long) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var copied = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            copied += read
            require(copied <= maxBytes) { "حجم APK أكبر من الحد الآمن للاستيراد." }
            output.write(buffer, 0, read)
        }
    }

    private fun resolveWorkspacePath(relativePath: String): File {
        val root = workspaceRoot.canonicalFile
        val target = File(root, relativePath).canonicalFile
        require(target == root || target.path.startsWith(root.path + File.separator)) {
            "المسار خارج مساحة OVELIN SPACE."
        }
        return target
    }

    private fun relativeToWorkspace(file: File): String =
        file.canonicalFile.relativeTo(workspaceRoot.canonicalFile).invariantSeparatorsPath

    private fun validateName(name: String): String {
        val safeName = name.trim()
        require(
            safeName.isNotEmpty() &&
                safeName != "." &&
                safeName != ".." &&
                !safeName.contains('/') &&
                !safeName.contains('\\'),
        ) { "اكتب اسمًا صالحًا من دون فواصل مسارات." }
        return safeName
    }

    private fun uniqueSibling(source: File, label: String): File {
        var index = 1
        var candidate = File(source.parentFile ?: workspaceRoot, "$label $index - ${source.name}")
        while (candidate.exists()) {
            index++
            candidate = File(source.parentFile ?: workspaceRoot, "$label $index - ${source.name}")
        }
        return candidate
    }

    private fun uniqueSiblingInRoot(name: String): File {
        var index = 0
        var candidate = File(workspaceRoot, name)
        while (candidate.exists()) {
            index++
            candidate = File(workspaceRoot, if (index == 1) "نقل - $name" else "نقل $index - $name")
        }
        return candidate
    }

    private fun directoryBytes(directory: File): Long =
        directory.walkTopDown().filter(File::isFile).sumOf(File::length)

    private companion object {
        const val MAX_APK_BYTES = 512L * 1024 * 1024
        const val MAX_SEARCH_RESULTS = 300
    }
}
