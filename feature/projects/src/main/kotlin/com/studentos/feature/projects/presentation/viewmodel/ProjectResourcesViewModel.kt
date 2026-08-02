package com.studentos.feature.projects.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.feature.projects.domain.model.ProjectResourceDomain
import com.studentos.feature.projects.domain.repository.ProjectRepository
import com.studentos.feature.projects.presentation.state.ProjectResourcesUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectResourcesViewModel @Inject constructor(
    private val repository: ProjectRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val projectId: Long = savedStateHandle.get<String>("projectId")?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(ProjectResourcesUiState())
    val uiState: StateFlow<ProjectResourcesUiState> = _uiState.asStateFlow()

    init {
        if (projectId > 0L) {
            observeProjectAndResources()
        } else {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Invalid project ID") }
        }
    }

    fun observeProjectAndResources() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            combine(
                repository.getProjectById(projectId),
                repository.getResourcesForProject(projectId)
            ) { project, resources ->
                Pair(project, resources)
            }
            .catch { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to load project resources"
                    )
                }
            }
            .collect { (project, resources) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        project = project,
                        resources = resources,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun openCreateDialog() {
        _uiState.update { it.copy(isCreateDialogOpen = true, resourceToEdit = null) }
    }

    fun openEditDialog(resource: ProjectResourceDomain) {
        _uiState.update { it.copy(isCreateDialogOpen = true, resourceToEdit = resource) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(isCreateDialogOpen = false, resourceToEdit = null) }
    }

    fun saveResource(url: String, label: String?, type: String) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isEmpty()) return

        val editResource = _uiState.value.resourceToEdit

        viewModelScope.launch {
            try {
                if (editResource == null) {
                    repository.createResource(projectId, trimmedUrl, label, type)
                } else {
                    repository.updateResource(editResource.id, trimmedUrl, label, type)
                }
                dismissDialog()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Failed to save resource")
                }
            }
        }
    }

    fun deleteResource(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteResource(id)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Failed to delete resource")
                }
            }
        }
    }
}
