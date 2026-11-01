package com.ovelin.space.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ImportedAppEntity::class,
        VirtualInstanceEntity::class,
        WorkspaceSettingEntity::class,
        StorageLogEntity::class,
        ImportLogEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class WorkspaceDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao

    companion object {
        @Volatile
        private var instance: WorkspaceDatabase? = null

        fun getInstance(context: Context): WorkspaceDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WorkspaceDatabase::class.java,
                    "ovelin-space.db",
                ).build().also { instance = it }
            }
    }
}
