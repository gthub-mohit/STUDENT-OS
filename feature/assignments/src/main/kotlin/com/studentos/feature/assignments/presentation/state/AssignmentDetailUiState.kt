package com.studentos.feature.assignments.presentation.state

import com.studentos.core.database.entity.AssignmentEntity

sealed interface AssignmentDetailUiState {
    data object Loading : AssignmentDetailUiState

    data class Success(
        val assignment: AssignmentEntity,
        val subjectName: String = "Subject",
        val showDeleteConfirmation: Boolean = false
    ) : AssignmentDetailUiState

    data class Error(val message: String) : AssignmentDetailUiState

    data object Deleted : AssignmentDetailUiState
}
