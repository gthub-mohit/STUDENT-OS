package com.studentos.feature.projects.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.feature.projects.domain.model.ProjectDomain
import com.studentos.feature.projects.domain.repository.ProjectRepository
import com.studentos.feature.projects.presentation.state.ProjectsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val repository: ProjectRepository,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    init {
        observeProjects()
    }

    fun observeProjects() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            combine(
                repository.getActiveProjects(),
                repository.getArchivedProjects()
            ) { active, archived ->
                Pair(active, archived)
            }
            .catch { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to load projects"
                    )
                }
            }
            .collect { (active, archived) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        activeProjects = active,
                        archivedProjects = archived,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun toggleTab(showArchived: Boolean) {
        _uiState.update { it.copy(showArchivedTab = showArchived) }
    }

    fun openCreateDialog() {
        _uiState.update { it.copy(isCreateDialogOpen = true, projectToEdit = null) }
    }

    fun openEditDialog(project: ProjectDomain) {
        _uiState.update { it.copy(isCreateDialogOpen = true, projectToEdit = project) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(isCreateDialogOpen = false, projectToEdit = null) }
    }

    fun saveProject(title: String, inactivityThresholdDays: Int) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return

        val editProject = _uiState.value.projectToEdit
        val threshold = inactivityThresholdDays.coerceAtLeast(1)

        viewModelScope.launch {
            try {
                if (editProject == null) {
                    repository.createProject(trimmed, threshold)
                } else {
                    repository.updateProject(editProject.id, trimmed, threshold)
                }
                dismissDialog()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Failed to save project")
                }
            }
        }
    }

    fun archiveProject(id: Long) {
        viewModelScope.launch {
            try {
                repository.archiveProject(id)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Failed to archive project")
                }
            }
        }
    }

    fun unarchiveProject(id: Long) {
        viewModelScope.launch {
            try {
                repository.unarchiveProject(id)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Failed to restore project")
                }
            }
        }
    }

    fun getCurrentTimeMs(): Long = clock.millis()
}
