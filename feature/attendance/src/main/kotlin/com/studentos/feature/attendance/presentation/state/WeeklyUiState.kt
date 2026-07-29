package com.studentos.feature.attendance.presentation.state

import com.studentos.core.database.entity.ClassEventEntity
import com.studentos.core.database.entity.SubjectEntity

/**
 * WeeklyUiState — Sealed interface representing the UI state of WeeklyViewScreen.
 */
sealed interface WeeklyUiState {
    object Loading : WeeklyUiState
    data class Success(
        val selectedDayOfWeek: Int, // 1 = Mon, ..., 7 = Sun
        val dayEvents: List<ClassEventEntity>,
        val subjects: List<SubjectEntity>,
        val overallAttendancePercentage: Double,
        val isBelowThreshold: Boolean,
        val threshold: Int = 75,
        val selectedSubjectId: Long? = null
    ) : WeeklyUiState
    data class Error(val message: String) : WeeklyUiState
}
