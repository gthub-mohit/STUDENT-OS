package com.studentos.feature.intelligence.presentation.state

import com.studentos.feature.intelligence.domain.model.ComingUpItem
import com.studentos.feature.intelligence.domain.model.TodayFocusItem

/**
 * UI state for the redesigned Student OS Home Screen.
 *
 * Single Source of Truth:
 * [completedPrioritiesCount], [totalPrioritiesCount], and [progressBarValue]
 * are derived directly from [focusItems].
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val isGenerating: Boolean = false,
    val hasBrief: Boolean = false,
    val completedPrioritiesCount: Int = 0,
    val totalPrioritiesCount: Int = 0,
    val progressBarValue: Float = 0f,
    val todayGoalSummary: String = "Tap to see today's plan",
    val focusItems: List<TodayFocusItem> = emptyList(),
    val comingUpItems: List<ComingUpItem> = emptyList(),
    val errorMessage: String? = null
)
