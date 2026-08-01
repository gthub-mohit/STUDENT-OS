package com.studentos.core.database.relation

import androidx.room.ColumnInfo

/**
 * DailyBriefSummary — Projection data class for brief history lists (omitting heavy JSON blob columns).
 */
data class DailyBriefSummary(
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "score_target")
    val scoreTarget: Int,

    @ColumnInfo(name = "score_actual")
    val scoreActual: Int,

    @ColumnInfo(name = "guidance_source")
    val guidanceSource: String,

    @ColumnInfo(name = "generated_at")
    val generatedAt: Long,

    @ColumnInfo(name = "llm_guidance")
    val llmGuidance: String? = null
)
