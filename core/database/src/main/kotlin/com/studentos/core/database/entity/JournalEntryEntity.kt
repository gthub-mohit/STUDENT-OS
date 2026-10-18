package com.studentos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * JournalEntryEntity — Daily log and reflection digest.
 *
 * Indices:
 *  - idx_journal_date on (date)
 */
@Entity(
    tableName = "journal_entries",
    indices = [
        Index(value = ["date"], name = "idx_journal_date")
    ]
)
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "date")
    val date: String, // ISO-8601 "YYYY-MM-DD"

    @ColumnInfo(name = "raw_log")
    val rawLog: String,

    @ColumnInfo(name = "structured_summary")
    val structuredSummary: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
