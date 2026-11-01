package com.ovelin.space.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "imported_apps",
    indices = [Index(value = ["packageName"], unique = true)],
)
data class ImportedAppEntity(
    @PrimaryKey val id: String,
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val apkPaths: String,
    val iconPath: String?,
    val signerSha256: String?,
    val sourceType: String,
    val importedAt: Long,
)

@Entity(
    tableName = "virtual_instances",
    indices = [Index("appId")],
)
data class VirtualInstanceEntity(
    @PrimaryKey val id: String,
    val appId: String,
    val displayName: String,
    val instanceNumber: Int,
    val dataPath: String,
    val state: String,
    val createdAt: Long,
    val lastLaunchedAt: Long?,
)

@Entity(tableName = "workspace_settings")
data class WorkspaceSettingEntity(
    @PrimaryKey val settingKey: String,
    val value: String,
    val updatedAt: Long,
)

@Entity(
    tableName = "storage_logs",
    indices = [Index("sampledAt")],
)
data class StorageLogEntity(
    @PrimaryKey val id: String,
    val instanceId: String?,
    val path: String,
    val bytesUsed: Long,
    val sampledAt: Long,
)

@Entity(
    tableName = "import_logs",
    indices = [Index("createdAt")],
)
data class ImportLogEntity(
    @PrimaryKey val id: String,
    val packageName: String?,
    val sourceType: String,
    val createdAt: Long,
    val succeeded: Boolean,
    val detail: String,
)
