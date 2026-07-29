package com.studentos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * AiCallLogEntity — Append-only diagnostic log of every LLM call attempt.
 *
 * Index: idx_ai_log_date on (created_at DESC)
 * Constraints:
 *  - CHECK(was_cache_hit IN (0,1))
 *  - CHECK(was_delta IN (0,1))
 *  - CHECK(success IN (0,1))
 */
@Entity(
    tableName = "ai_call_log",
    indices = [
        Index(value = ["created_at"], name = "idx_ai_log_date")
    ]
)
data class AiCallLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "triggered_by")
    val triggeredBy: String,

    @ColumnInfo(name = "snapshot_hash")
    val snapshotHash: String,

    @ColumnInfo(name = "was_cache_hit")
    val wasCacheHit: Boolean,

    @ColumnInfo(name = "was_delta")
    val wasDelta: Boolean,

    @ColumnInfo(name = "latency_ms")
    val latencyMs: Long? = null,

    @ColumnInfo(name = "token_count", defaultValue = "0")
    val tokenCount: Int = 0,

    @ColumnInfo(name = "success")
    val success: Boolean,

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
