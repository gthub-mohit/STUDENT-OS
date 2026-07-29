package com.studentos.feature.attendance.presentation.state

import com.studentos.core.database.relation.SubjectAttendanceSummary

/**
 * AnalyticsUiState — Sealed interface representing the UI state of AttendanceAnalyticsScreen.
 */
sealed interface AnalyticsUiState {
    object Loading : AnalyticsUiState
    data class Success(
        val summaries: List<SubjectAttendanceSummary>,
        val overallPercentage: Double,
        val totalHeldCount: Int,
        val totalPresentCount: Int,
        val isBelowThreshold: Boolean,
        val threshold: Int = 75
    ) : AnalyticsUiState
    data class Error(val message: String) : AnalyticsUiState
}
