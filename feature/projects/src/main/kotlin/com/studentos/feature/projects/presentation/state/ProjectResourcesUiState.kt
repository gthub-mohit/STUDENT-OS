package com.studentos.feature.projects.presentation.state

import com.studentos.feature.projects.domain.model.ProjectDomain
import com.studentos.feature.projects.domain.model.ProjectResourceDomain

data class ProjectResourcesUiState(
    val isLoading: Boolean = true,
    val project: ProjectDomain? = null,
    val resources: List<ProjectResourceDomain> = emptyList(),
    val isCreateDialogOpen: Boolean = false,
    val resourceToEdit: ProjectResourceDomain? = null,
    val errorMessage: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && resources.isEmpty()
}
