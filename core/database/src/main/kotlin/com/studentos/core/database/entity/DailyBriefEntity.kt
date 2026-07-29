package com.studentos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * DailyBriefEntity — Stores daily student state snapshot, brief output, and guidance.
 *
 * Indexes:
 *  - idx_brief_date (UNIQUE) on (date DESC)
 *  - idx_brief_hash on (snapshot_hash)
 * Constraints:
 *  - UNIQUE(date)
 *  - CHECK(guidance_source IN ('LLM','DETERMINISTIC'))
 */
@Entity(
    tableName = "daily_briefs",
    indices = [
        Index(value = ["date"], unique = true, name = "idx_brief_date"),
        Index(value = ["snapshot_hash"], name = "idx_brief_hash")
    ]
)
data class DailyBriefEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "json_snapshot")
    val jsonSnapshot: String,

    @ColumnInfo(name = "snapshot_hash")
    val snapshotHash: String,

    @ColumnInfo(name = "brief_json")
    val briefJson: String,

    @ColumnInfo(name = "llm_guidance")
    val llmGuidance: String? = null,

    @ColumnInfo(name = "guidance_source")
    val guidanceSource: String,

    @ColumnInfo(name = "score_target", defaultValue = "0")
    val scoreTarget: Int = 0,

    @ColumnInfo(name = "score_actual", defaultValue = "0")
    val scoreActual: Int = 0,

    @ColumnInfo(name = "generated_at")
    val generatedAt: Long,

    @ColumnInfo(name = "guidance_updated_at")
    val guidanceUpdatedAt: Long
) {
    companion object {
        const val GUIDANCE_SOURCE_LLM = "LLM"
        const val GUIDANCE_SOURCE_DETERMINISTIC = "DETERMINISTIC"
    }
}
