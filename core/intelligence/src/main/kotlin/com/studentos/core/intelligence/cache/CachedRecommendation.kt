package com.studentos.core.intelligence.cache

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CachedRecommendation(
    @SerialName("snapshot_hash") val snapshotHash: String,
    @SerialName("llm_response") val llmResponse: String,
    @SerialName("provider") val provider: String = "DEEPSEEK",
    @SerialName("created_at") val createdAt: Long,
    @SerialName("token_count") val tokenCount: Int = 0
)
