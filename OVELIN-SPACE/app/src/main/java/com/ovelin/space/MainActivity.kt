package com.ovelin.space

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OvelinSpaceTheme {
                OvelinSpaceApp(repository, engine)
            }
        }
    }
}
