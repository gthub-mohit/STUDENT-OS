package com.studentos.feature.intelligence.presentation.state

data class DailyScoreUiState(
    val isLoading: Boolean = true,
    val targetScore: Int = 0,
    val currentScore: Int = 0,
    val progressPercentage: Float = 0f,
    val progressBarValue: Float = 0f,
    val remainingScore: Int = 0,
    val errorMessage: String? = null
)
