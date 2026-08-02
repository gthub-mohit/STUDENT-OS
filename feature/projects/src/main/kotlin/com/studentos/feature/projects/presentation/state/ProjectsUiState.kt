package com.studentos.feature.projects.presentation.state

import com.studentos.feature.projects.domain.model.ProjectDomain

data class ProjectsUiState(
    val isLoading: Boolean = true,
    val activeProjects: List<ProjectDomain> = emptyList(),
    val archivedProjects: List<ProjectDomain> = emptyList(),
    val showArchivedTab: Boolean = false,
    val isCreateDialogOpen: Boolean = false,
    val projectToEdit: ProjectDomain? = null,
    val errorMessage: String? = null
) {
    val displayedProjects: List<ProjectDomain>
        get() = if (showArchivedTab) archivedProjects else activeProjects

    val isEmpty: Boolean
        get() = !isLoading && displayedProjects.isEmpty()
}
