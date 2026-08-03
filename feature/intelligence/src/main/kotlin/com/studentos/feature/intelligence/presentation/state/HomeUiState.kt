package com.studentos.feature.intelligence.presentation.state

import com.studentos.feature.intelligence.domain.model.RecommendationCard

/**
 * Lightweight UI state for the Home Screen.
 *
 * Combines score data from [DailyScoreUiState] and the top recommendations
 * from [DailyBriefUiState] into a single, home-screen-optimized state.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val hasBrief: Boolean = false,
    val currentScore: Int = 0,
    val targetScore: Int = 0,
    val progressBarValue: Float = 0f,
    val todayGoalSummary: String = "Generate your daily brief to get started",
    val topRecommendations: List<RecommendationCard> = emptyList(),
    val errorMessage: String? = null
)
