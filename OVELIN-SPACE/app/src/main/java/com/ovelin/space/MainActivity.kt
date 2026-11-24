package com.ovelin.space

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.ovelin.space.data.WorkspaceDatabase
import com.ovelin.space.data.WorkspaceRepository
import com.ovelin.space.engine.BlackBoxEngine
import com.ovelin.space.engine.VirtualizationEngine
import java.io.File
import com.ovelin.space.ui.OvelinSpaceApp
import com.ovelin.space.ui.OvelinSpaceTheme

class MainActivity : ComponentActivity() {
    private val dao by lazy { WorkspaceDatabase.getInstance(applicationContext).workspaceDao() }
    private val repository by lazy { WorkspaceRepository(applicationContext, dao) }
    private val engine: VirtualizationEngine by lazy {
        BlackBoxEngine(applicationContext, dao, File(applicationContext.filesDir, "ovelin-space"))
    }

    private val allFilesAccessLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    private val legacyStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestStorageAccessIfNeeded()
        setContent {
            OvelinSpaceTheme {
                OvelinSpaceApp(repository, engine)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The "All files access" grant happens in a separate Settings screen; re-check when the
        // user comes back instead of making them restart the app.
        requestStorageAccessIfNeeded()
    }

    /**
     * Every cloned app's gallery picker, "save photo", and "set as profile picture" reads real
     * Android storage permissions checked against THIS app's identity (a clone runs under
     * Iskander's own UID). Without this granted, those always come back empty — not a bug in the
     * cloned app, a missing permission in the host. Asks once; harmless to call repeatedly.
     */
    private fun requestStorageAccessIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                runCatching {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:$packageName"),
                    )
                    allFilesAccessLauncher.launch(intent)
                }.recoverCatching {
                    allFilesAccessLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            }
        } else {
            val needed = listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ).filter {
                ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            if (needed.isNotEmpty()) {
                legacyStoragePermissionLauncher.launch(needed.toTypedArray())
            }
        }
    }
}
