package com.ovelin.space.engine

import android.content.Context
import android.os.StatFs
import com.ovelin.space.data.ImportedAppEntity
import com.ovelin.space.data.VirtualInstanceEntity
import com.ovelin.space.data.WorkspaceDao
import java.io.File
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.core.env.BEnvironment
import top.niunaijun.blackbox.utils.NativeUtils
import top.niunaijun.blackbox.fake.device.DeviceSpoofManager

/**
 * Each instance is one BlackBox virtual user that holds exactly one installed package, so a
 * user id is never shared between instances (device-identity settings are keyed by user id).
 */
class BlackBoxEngine(
    private val context: Context,
    private val dao: WorkspaceDao,
    private val workspaceRoot: File,
) : VirtualizationEngine {

    private val lock = Mutex()

    override val status: EngineStatus
        get() = EngineStatus(
            available = true,
            engineName = "BlackBox (Apache-2.0)",
            explanation = "محرك افتراضي مدمج. التوافق يختلف من تطبيق لآخر ويجب اختباره على الجهاز.",
        )

    override suspend fun remainingCapacity(): Int = withContext(Dispatchers.IO) {
        val available = StatFs(context.filesDir.absolutePath).availableBytes
        val usable = available - RESERVE_BYTES
        if (usable <= 0) 0 else (usable / ESTIMATED_INSTANCE_BYTES).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    override suspend fun createInstance(
        app: ImportedAppEntity,
        displayName: String,
        identity: DeviceIdentity?,
    ): EngineResult = lock.withLock {
        withContext(Dispatchers.IO) {
            if (remainingCapacity() <= 0) {
                return@withContext EngineResult.Unsupported("المساحة المتاحة غير كافية لإنشاء نسخة جديدة.")
            }
            val core = BlackBoxCore.get()
            val existing = dao.getInstances()
            val userId = (FIRST_USER_ID..MAX_USER_ID).firstOrNull { id -> existing.none { it.instanceNumber == id } }
                ?: return@withContext EngineResult.Unsupported("تم الوصول إلى الحد الأقصى لعدد النسخ.")

            try {
                if (core.users.none { it.id == userId }) core.createUser(userId)
                val result = if (app.sourceType == "INSTALLED") {
                    core.installPackageAsUser(app.packageName, userId)
                } else {
                    val apk = File(workspaceRoot, app.apkPaths.lineSequence().first())
                    core.installPackageAsUser(apk, userId)
                }
                if (!result.success) {
                    runCatching { core.deleteUser(userId) }
                    return@withContext EngineResult.Unsupported(result.msg ?: "فشل تثبيت النسخة داخل المحرك.")
                }
                ensureNativeLibs(app)
                if (identity != null) {
                    DeviceSpoofManager.saveUserProfile(userId, identity.manufacturer, identity.model, randomAndroidId())
                }
                dao.upsertInstance(
                    VirtualInstanceEntity(
                        id = UUID.randomUUID().toString(),
                        appId = app.id,
                        displayName = displayName,
                        instanceNumber = userId,
                        dataPath = "blackbox/user/$userId",
                        state = STATE_READY,
                        createdAt = System.currentTimeMillis(),
                        lastLaunchedAt = null,
                    ),
                )
                EngineResult.Completed("تم إنشاء النسخة: $displayName")
            } catch (error: Throwable) {
                runCatching { core.uninstallPackageAsUser(app.packageName, userId) }
                runCatching { core.deleteUser(userId) }
                DeviceSpoofManager.resetToDefaults(userId)
                EngineResult.Unsupported(error.message ?: "تعذر إنشاء النسخة.")
            }
        }
    }

    override suspend fun launchInstance(instance: VirtualInstanceEntity): EngineResult =
        withContext(Dispatchers.IO) {
            val app = dao.getImportedApp(instance.appId)
                ?: return@withContext EngineResult.Unsupported("التطبيق الأصلي المستورد غير موجود.")
            ensureNativeLibs(app)
            val launched = runCatching { BlackBoxCore.get().launchApk(app.packageName, instance.instanceNumber) }
                .getOrElse { return@withContext EngineResult.Unsupported(it.message ?: "تعذر تشغيل النسخة.") }
            if (!launched) {
                return@withContext EngineResult.Unsupported("لا توجد واجهة تشغيل لهذا التطبيق داخل المحرك.")
            }
            dao.upsertInstance(instance.copy(state = STATE_RUNNING, lastLaunchedAt = System.currentTimeMillis()))
            EngineResult.Completed("تم تشغيل ${instance.displayName}")
        }

    override suspend fun stopInstance(instance: VirtualInstanceEntity): EngineResult =
        withContext(Dispatchers.IO) {
            val app = dao.getImportedApp(instance.appId)
                ?: return@withContext EngineResult.Unsupported("التطبيق الأصلي المستورد غير موجود.")
            runCatching { BlackBoxCore.get().stopPackage(app.packageName, instance.instanceNumber) }
                .onFailure { return@withContext EngineResult.Unsupported(it.message ?: "تعذر إيقاف النسخة.") }
            dao.upsertInstance(instance.copy(state = STATE_READY))
            EngineResult.Completed("تم إيقاف ${instance.displayName}")
        }

    override suspend fun deleteInstance(instance: VirtualInstanceEntity): EngineResult = lock.withLock {
        withContext(Dispatchers.IO) {
            val app = dao.getImportedApp(instance.appId)
            val core = BlackBoxCore.get()
            runCatching {
                if (app != null) {
                    core.stopPackage(app.packageName, instance.instanceNumber)
                    core.uninstallPackageAsUser(app.packageName, instance.instanceNumber)
                }
                core.deleteUser(instance.instanceNumber)
            }.onFailure { return@withContext EngineResult.Unsupported(it.message ?: "تعذر حذف النسخة.") }
            DeviceSpoofManager.resetToDefaults(instance.instanceNumber)
            dao.deleteInstance(instance.id)
            EngineResult.Completed("تم حذف ${instance.displayName}")
        }
    }

    override fun hasIdentity(instance: VirtualInstanceEntity): Boolean =
        runCatching { DeviceSpoofManager.hasCustomProfile(instance.instanceNumber) }.getOrDefault(false)

    override suspend fun diagnostics(): String = withContext(Dispatchers.IO) {
        val out = StringBuilder()
        fun line(label: String, value: String) { out.append(label).append(": ").append(value).append('\n') }
        line("Android", "${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
        line("Device", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        line("ABIs", android.os.Build.SUPPORTED_ABIS.joinToString())
        line("Host targetSdk", context.applicationInfo.targetSdkVersion.toString())
        line(
            "Page size",
            runCatching { android.system.Os.sysconf(android.system.OsConstants._SC_PAGE_SIZE).toString() }
                .getOrElse { "?" },
        )
        val core = runCatching { BlackBoxCore.get() }.getOrNull()
        if (core == null) {
            line("Engine core", "NOT LOADED")
        } else {
            line("Engine service", runCatching { "OK (${core.users.size} virtual users)" }
                .getOrElse { "FAILED: ${it.javaClass.simpleName}: ${it.message}" })
            line("All-files access", runCatching { core.hasAllFilesAccess().toString() }.getOrElse { "?" })
        }
        line("Instances in database", runCatching { dao.getInstances().size.toString() }.getOrElse { "?" })
        line("Free capacity (est.)", runCatching { remainingCapacity().toString() }.getOrElse { "?" })

        out.append("\n--- Recent engine errors ---\n")
        val errors = runCatching {
            val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", "1500", "-v", "threadtime"))
            process.inputStream.bufferedReader().useLines { lines ->
                lines.filter { l ->
                    (" E " in l || " F " in l || "FATAL" in l || "AndroidRuntime" in l) ||
                        (" W " in l && ENGINE_TAGS.any { tag -> tag in l })
                }.toList().takeLast(120)
            }
        }.getOrElse { listOf("logcat unavailable: ${it.message}") }
        out.append(if (errors.isEmpty()) "(none)\n" else errors.joinToString("\n"))
        out.toString()
    }

    /**
     * Apps installed from the phone usually keep their native libraries inside split APKs
     * (config.arm64_v8a.apk) that Android loads straight from the zip. The virtual runtime needs
     * them as real files, otherwise the app dies with UnsatisfiedLinkError (black screen).
     * Safe to call repeatedly; files that are already extracted are skipped.
     */
    private fun ensureNativeLibs(app: ImportedAppEntity) {
        if (app.sourceType != "INSTALLED") return
        runCatching {
            val info = context.packageManager.getApplicationInfo(app.packageName, 0)
            val libDir = BEnvironment.getAppLibDir(app.packageName)
            val apks = buildList {
                add(info.sourceDir)
                info.splitSourceDirs?.let { addAll(it) }
            }
            for (path in apks) {
                runCatching { NativeUtils.copyNativeLib(File(path), libDir) }
            }
        }
    }

    private fun randomAndroidId(): String =
        (1..16).joinToString("") { Random.nextInt(16).toString(16) }

    companion object {
        const val STATE_READY = "READY"
        const val STATE_RUNNING = "RUNNING"
        private val ENGINE_TAGS = listOf(
            "BlackBox", "BActivityThread", "HCallbackProxy", "CopyExecutor", "BPackageManager",
            "IOCore", "VirtualM", "OvelinApplication", "NativeCore",
        )
        private const val FIRST_USER_ID = 1
        private const val MAX_USER_ID = 9999
        private const val RESERVE_BYTES = 1L * 1024 * 1024 * 1024
        private const val ESTIMATED_INSTANCE_BYTES = 150L * 1024 * 1024
    }
}
