package com.studentos.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * SettingEntity — Room entity representing a key-value entry in the `settings` table.
 *
 * Stores all Student-configurable parameters as string key-value pairs wrapped by `SettingsRepository`.
 */
@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey
    val key: String,
    val value: String
)
