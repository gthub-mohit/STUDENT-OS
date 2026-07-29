package com.studentos.feature.attendance.presentation.state

import com.studentos.core.database.entity.SubjectEntity
import com.studentos.core.database.entity.TimetableSlotEntity

/**
 * EditTimetableUiState — Sealed interface representing the UI state of EditTimetableScreen.
 */
sealed interface EditTimetableUiState {
    object Loading : EditTimetableUiState
    data class Success(
        val slots: List<TimetableSlotEntity>,
        val subjects: List<SubjectEntity>,
        val selectedDayOfWeek: Int = 1
    ) : EditTimetableUiState
    data class Error(val message: String) : EditTimetableUiState
}
