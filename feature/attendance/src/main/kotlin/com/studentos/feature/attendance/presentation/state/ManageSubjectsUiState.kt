package com.studentos.feature.attendance.presentation.state

import com.studentos.core.database.entity.SubjectEntity

/**
 * ManageSubjectsUiState — Sealed interface representing the UI state of ManageSubjectsScreen.
 */
sealed interface ManageSubjectsUiState {
    object Loading : ManageSubjectsUiState
    data class Success(
        val activeSubjects: List<SubjectEntity>,
        val archivedSubjects: List<SubjectEntity>
    ) : ManageSubjectsUiState
    data class Error(val message: String) : ManageSubjectsUiState
}
