package com.studentos.feature.assignments.presentation.state

import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.feature.assignments.domain.model.AssignmentFilter
import com.studentos.feature.assignments.domain.model.PrioritizedAssignmentGroup

sealed interface AssignmentListUiState {
    data object Loading : AssignmentListUiState

    data class Success(
        val assignments: List<AssignmentEntity>,
        val prioritizedGroups: List<PrioritizedAssignmentGroup> = emptyList(),
        val subjectsMap: Map<Long, String> = emptyMap(),
        val currentFilter: AssignmentFilter = AssignmentFilter.TODAY,
        val assignmentToDelete: AssignmentEntity? = null
    ) : AssignmentListUiState

    data class Error(val message: String) : AssignmentListUiState
}
