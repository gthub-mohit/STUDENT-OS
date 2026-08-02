package com.studentos.feature.projects.presentation.state

import com.studentos.feature.projects.domain.model.MilestoneDomain
import com.studentos.feature.projects.domain.model.ProjectDomain

data class MilestoneUiState(
    val isLoading: Boolean = true,
    val project: ProjectDomain? = null,
    val milestones: List<MilestoneDomain> = emptyList(),
    val isCreateDialogOpen: Boolean = false,
    val milestoneToEdit: MilestoneDomain? = null,
    val errorMessage: String? = null
) {
    val completedCount: Int
        get() = milestones.count { it.isDone }

    val totalCount: Int
        get() = milestones.size

    val progressPercentage: Float
        get() = if (totalCount > 0) (completedCount.toFloat() / totalCount.toFloat() * 100f).coerceIn(0f, 100f) else 0f

    val isEmpty: Boolean
        get() = !isLoading && milestones.isEmpty()
}
