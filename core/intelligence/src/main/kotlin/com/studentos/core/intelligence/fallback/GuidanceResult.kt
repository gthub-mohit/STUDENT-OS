package com.studentos.core.intelligence.fallback

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GuidanceResult(
    @SerialName("summary") val summary: String,
    @SerialName("recommendations") val recommendations: List<GuidanceItem>,
    @SerialName("source") val source: GuidanceSource = GuidanceSource.OFFLINE
)

@Serializable
data class GuidanceItem(
    @SerialName("priority") val priority: Int,
    @SerialName("category") val category: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String
)

@Serializable
enum class GuidanceSource {
    OFFLINE,
    AI
}
