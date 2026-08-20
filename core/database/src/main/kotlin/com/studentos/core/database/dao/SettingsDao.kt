package com.studentos.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.studentos.core.database.entity.SettingEntity
import kotlinx.coroutines.flow.Flow

/**
 * SettingsDao — Room DAO interface for `settings` table operations.
 */
@Dao
interface SettingsDao {

    /**
     * Read the raw string value for a key.
     */
    @Query("SELECT value FROM settings WHERE key = :key")
    suspend fun get(key: String): String?

    /**
     * Retrieve all key-value pairs stored in the `settings` table.
     */
    @Query("SELECT * FROM settings")
    suspend fun getAll(): List<SettingEntity>

    /**
     * Observe all key-value pairs stored in the `settings` table.
     */
    @Query("SELECT * FROM settings")
    fun observeAll(): Flow<List<SettingEntity>>

    /**
     * Upsert (INSERT OR REPLACE) a setting entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(setting: SettingEntity)
}
