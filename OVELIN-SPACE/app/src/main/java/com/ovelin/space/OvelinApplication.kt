package com.ovelin.space

import android.app.Application
import android.content.Context
import android.util.Log
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.app.configuration.AppLifecycleCallback
import top.niunaijun.blackbox.app.configuration.ClientConfiguration
import top.niunaijun.blackbox.fake.device.DeviceSpoofManager

/**
 * Runs in the host process AND in every virtual-app process, so engine start-up must stay
 * defensive: a failure here must never take the host down.
 */
class OvelinApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        runCatching { BlackBoxCore.get().onBeforeMainApplicationAttach(this, base) }
            .onFailure { Log.e(TAG, "onBeforeMainApplicationAttach failed", it) }
        runCatching {
            BlackBoxCore.get().doAttachBaseContext(
                base,
                object : ClientConfiguration() {
                    override fun getHostPackageName(): String = base.packageName
                    override fun isHideRoot(): Boolean = false
                    override fun isUseVpnNetwork(): Boolean = false
                },
            )
            // Default device identity (Pixel 6) or the custom profile saved for the virtual user.
            BlackBoxCore.get().addAppLifecycleCallback(object : AppLifecycleCallback() {
                override fun beforeCreateApplication(
                    packageName: String?,
                    processName: String?,
                    context: Context?,
                    userId: Int,
                ) {
                    DeviceSpoofManager.applyToCurrentProcess(userId)
                }
            })
        }.onFailure { Log.e(TAG, "doAttachBaseContext failed", it) }
        runCatching { BlackBoxCore.get().onAfterMainApplicationAttach(this, base) }
            .onFailure { Log.e(TAG, "onAfterMainApplicationAttach failed", it) }
    }

    override fun onCreate() {
        super.onCreate()
        runCatching { BlackBoxCore.get().doCreate() }
            .onFailure { Log.e(TAG, "doCreate failed", it) }
    }

    private companion object {
        const val TAG = "OvelinApplication"
    }
}
