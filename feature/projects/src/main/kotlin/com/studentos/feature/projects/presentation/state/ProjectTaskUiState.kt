package com.studentos.feature.projects.presentation.state

import com.studentos.feature.projects.domain.model.NextActionRecommendation
import com.studentos.feature.projects.domain.model.ProjectDomain
import com.studentos.feature.projects.domain.model.ProjectTaskDomain
import com.studentos.feature.projects.domain.model.ProjectTaskEngine
import com.studentos.feature.projects.domain.model.ProjectTaskState

enum class ProjectTaskStatusFilter {
    ALL,
    AVAILABLE,
    BLOCKED,
    COMPLETED
}

data class ProjectTaskUiState(
    val isLoading: Boolean = true,
    val project: ProjectDomain? = null,
    val tasks: List<ProjectTaskDomain> = emptyList(),
    val isParallelMode: Boolean = false,
    val statusFilter: ProjectTaskStatusFilter = ProjectTaskStatusFilter.ALL,
    val isCreateTaskDialogOpen: Boolean = false,
    val isFilterSheetOpen: Boolean = false,
    val taskToEdit: ProjectTaskDomain? = null,
    val dialogError: String? = null,
    val errorMessage: String? = null,
    val currentTimeMs: Long = System.currentTimeMillis()
) {
    val tasksMap: Map<Long, ProjectTaskDomain>
        get() = tasks.associateBy { it.id }

    val availableTasks: List<ProjectTaskDomain>
        get() {
            val map = tasksMap
            return tasks.filter { !it.isCompleted && ProjectTaskEngine.getTaskState(it, map) == ProjectTaskState.AVAILABLE }
        }

    val blockedTasks: List<ProjectTaskDomain>
        get() {
            val map = tasksMap
            return tasks.filter { !it.isCompleted && ProjectTaskEngine.getTaskState(it, map) == ProjectTaskState.BLOCKED }
        }

    val completedTasks: List<ProjectTaskDomain>
        get() = tasks.filter { it.isCompleted }

    val pendingTasks: List<ProjectTaskDomain>
        get() = tasks.filter { !it.isCompleted }

    val nextAction: NextActionRecommendation
        get() = ProjectTaskEngine.computeNextAction(tasks, currentTimeMs)

    val activeFilterCount: Int
        get() = if (statusFilter != ProjectTaskStatusFilter.ALL) 1 else 0

    val filterButtonLabel: String
        get() = if (activeFilterCount > 0) "Filters ($activeFilterCount)" else "Filters"

    val filteredTasks: List<ProjectTaskDomain>
        get() = when (statusFilter) {
            ProjectTaskStatusFilter.ALL -> tasks
            ProjectTaskStatusFilter.AVAILABLE -> availableTasks
            ProjectTaskStatusFilter.BLOCKED -> blockedTasks
            ProjectTaskStatusFilter.COMPLETED -> completedTasks
        }

    val hasNoTasksInProject: Boolean
        get() = !isLoading && tasks.isEmpty()

    val hasNoFilteredResults: Boolean
        get() = !isLoading && tasks.isNotEmpty() && filteredTasks.isEmpty()

    val isEmpty: Boolean
        get() = hasNoTasksInProject
}
