package com.studentos.feature.attendance.presentation.state

import com.studentos.core.database.entity.ClassEventEntity
import java.time.LocalDate

/**
 * CalendarUiState — Sealed interface representing the UI state of CalendarViewScreen.
 */
sealed interface CalendarUiState {
    object Loading : CalendarUiState
    data class Success(
        val currentMonthEpochMs: Long,
        val selectedDateEpochMs: Long,
        val monthEvents: List<ClassEventEntity>,
        val selectedDayEvents: List<ClassEventEntity>
    ) : CalendarUiState
    data class Error(val message: String) : CalendarUiState
}
