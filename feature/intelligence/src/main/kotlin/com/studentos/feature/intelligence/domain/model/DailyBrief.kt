package com.studentos.feature.intelligence.domain.model

data class DailyBrief(
    val id: Long = 0,
    val date: String,
    val jsonSnapshot: String,
    val snapshotHash: String,
    val briefJson: String,
    val llmGuidance: String? = null,
    val guidanceSource: String = GUIDANCE_SOURCE_DETERMINISTIC,
    val scoreTarget: Int = 0,
    val scoreActual: Int = 0,
    val generatedAt: Long = System.currentTimeMillis(),
    val guidanceUpdatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val GUIDANCE_SOURCE_LLM = "LLM"
        const val GUIDANCE_SOURCE_DETERMINISTIC = "DETERMINISTIC"
    }
}
