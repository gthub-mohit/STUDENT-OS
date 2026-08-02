package com.studentos.feature.attendance.presentation.state

import com.studentos.core.database.entity.ClassEventEntity
import com.studentos.core.database.entity.SubjectEntity
import com.studentos.core.database.entity.TimetableSlotEntity

/**
 * WeeklyUiState — Sealed interface representing the UI state of WeeklyViewScreen.
 */
sealed interface WeeklyUiState {
    object Loading : WeeklyUiState
    data class Success(
        val selectedDayOfWeek: Int, // 1 = Mon, ..., 7 = Sun
        val weekOffset: Int = 0, // 0 = current week, -1 = prev week, +1 = next week
        val weekLabel: String = "This Week",
        val dayEvents: List<ClassEventEntity>,
        val subjects: List<SubjectEntity>,
        val timetableSlots: List<TimetableSlotEntity> = emptyList(),
        val overallAttendancePercentage: Double,
        val isBelowThreshold: Boolean,
        val threshold: Int = 75,
        val selectedSubjectId: Long? = null
    ) : WeeklyUiState
    data class Error(val message: String) : WeeklyUiState
}
