package com.studentos.feature.projects.presentation.state

import com.studentos.feature.projects.domain.model.ProjectDomain
import com.studentos.feature.projects.domain.model.ProjectTaskDomain

data class ProjectTaskUiState(
    val isLoading: Boolean = true,
    val project: ProjectDomain? = null,
    val tasks: List<ProjectTaskDomain> = emptyList(),
    val isParallelMode: Boolean = false,
    val isCreateTaskDialogOpen: Boolean = false,
    val taskToEdit: ProjectTaskDomain? = null,
    val errorMessage: String? = null
) {
    val pendingTasks: List<ProjectTaskDomain>
        get() = tasks.filter { !it.isCompleted }

    val completedTasks: List<ProjectTaskDomain>
        get() = tasks.filter { it.isCompleted }

    val activeNextAction: ProjectTaskDomain?
        get() = pendingTasks.firstOrNull { it.isNextAction }

    val isEmpty: Boolean
        get() = !isLoading && tasks.isEmpty()
}
