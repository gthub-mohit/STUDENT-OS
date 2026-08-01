package com.studentos.feature.intelligence.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RecommendationCard(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val priority: Int,
    val actionRoute: String? = null
)
