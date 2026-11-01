package com.ovelin.space.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM imported_apps ORDER BY appName COLLATE NOCASE")
    fun observeImportedApps(): Flow<List<ImportedAppEntity>>

    @Query("SELECT * FROM imported_apps WHERE id = :id LIMIT 1")
    suspend fun getImportedApp(id: String): ImportedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertImportedApp(app: ImportedAppEntity)

    @Delete
    suspend fun deleteImportedApp(app: ImportedAppEntity)

    @Query("SELECT * FROM virtual_instances ORDER BY createdAt DESC")
    fun observeInstances(): Flow<List<VirtualInstanceEntity>>

    @Query("SELECT * FROM virtual_instances")
    suspend fun getInstances(): List<VirtualInstanceEntity>

    @Query("SELECT * FROM virtual_instances WHERE id = :id LIMIT 1")
    suspend fun getInstance(id: String): VirtualInstanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInstance(instance: VirtualInstanceEntity)

    @Query("DELETE FROM virtual_instances WHERE id = :id")
    suspend fun deleteInstance(id: String)

    @Query("SELECT COUNT(*) FROM virtual_instances WHERE appId = :appId")
    suspend fun countInstances(appId: String): Int

    @Query("SELECT settingKey, value, updatedAt FROM workspace_settings WHERE settingKey = :key LIMIT 1")
    fun observeSetting(key: String): Flow<WorkspaceSettingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSetting(setting: WorkspaceSettingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStorageLog(log: StorageLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportLog(log: ImportLogEntity)
}
