package com.studentos.feature.intelligence.presentation.state

import com.studentos.feature.intelligence.domain.model.DailyBrief
import com.studentos.feature.intelligence.domain.model.RecommendationCard

data class DailyBriefUiState(
    val isLoading: Boolean = true,
    val isGenerating: Boolean = false,
    val dailyBrief: DailyBrief? = null,
    val recommendations: List<RecommendationCard> = emptyList(),
    val errorMessage: String? = null,
    val isEmpty: Boolean = false,
    val todayDate: String = ""
)
