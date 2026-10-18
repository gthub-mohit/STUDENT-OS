package com.studentos.feature.intelligence.domain.model

import com.studentos.core.database.entity.GoalEntity

/**
 * Status indicator representing user pace against expected trajectory.
 */
enum class GoalStatus {
    COMPLETED,
    AHEAD,
    ON_TRACK,
    BEHIND,
    NOT_STARTED,
    INSUFFICIENT_DATA
}

/**
 * Chronological data point for plotting actual and expected progress curves.
 */
data class ProgressDataPoint(
    val timestamp: Long,
    val actualProgress: Double,
    val expectedProgress: Double? = null,
    val dateLabel: String = ""
)

/**
 * Comprehensive progress summary for a goal, derived reactively from confirmed logs.
 */
data class GoalProgress(
    val goal: GoalEntity,
    val actualProgress: Double,
    val expectedProgressToday: Double?,
    val progressPercentage: Double,
    val status: GoalStatus,
    val statusDescription: String,
    val historyPoints: List<ProgressDataPoint>,
    val totalLogsCount: Int,
    val remainingValue: Double,
    val daysRemaining: Long?
)

/**
 * Validation result for goal creation and editing.
 */
data class GoalValidationResult(
    val isValid: Boolean,
    val titleError: String? = null,
    val dateError: String? = null,
    val valueError: String? = null
)
