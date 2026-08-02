package com.studentos.feature.projects.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.feature.projects.domain.model.BugDomain
import com.studentos.feature.projects.domain.repository.ProjectRepository
import com.studentos.feature.projects.presentation.state.BugSeverityFilter
import com.studentos.feature.projects.presentation.state.BugSortOrder
import com.studentos.feature.projects.presentation.state.BugStatusFilter
import com.studentos.feature.projects.presentation.state.BugTrackerUiState
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
class BugTrackerViewModel @Inject constructor(
    private val repository: ProjectRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val projectId: Long = savedStateHandle.get<String>("projectId")?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(BugTrackerUiState())
    val uiState: StateFlow<BugTrackerUiState> = _uiState.asStateFlow()

    init {
        if (projectId > 0L) {
            observeProjectAndBugs()
        } else {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Invalid project ID") }
        }
    }

    fun observeProjectAndBugs() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            combine(
                repository.getProjectById(projectId),
                repository.getBugsForProject(projectId)
            ) { project, bugs ->
                Pair(project, bugs)
            }
            .catch { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to load bug tracker"
                    )
                }
            }
            .collect { (project, bugs) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        project = project,
                        bugs = bugs,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun setStatusFilter(filter: BugStatusFilter) {
        _uiState.update { it.copy(statusFilter = filter) }
    }

    fun setSeverityFilter(filter: BugSeverityFilter) {
        _uiState.update { it.copy(severityFilter = filter) }
    }

    fun setSortOrder(order: BugSortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
    }

    fun openCreateDialog() {
        _uiState.update { it.copy(isCreateDialogOpen = true, bugToEdit = null) }
    }

    fun openEditDialog(bug: BugDomain) {
        _uiState.update { it.copy(isCreateDialogOpen = true, bugToEdit = bug) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(isCreateDialogOpen = false, bugToEdit = null) }
    }

    fun saveBug(description: String, severity: String) {
        val trimmed = description.trim()
        if (trimmed.isEmpty()) return

        val editBug = _uiState.value.bugToEdit

        viewModelScope.launch {
            try {
                if (editBug == null) {
                    repository.createBug(projectId, trimmed, severity)
                } else {
                    repository.updateBug(editBug.id, trimmed, severity)
                }
                dismissDialog()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Failed to save bug")
                }
            }
        }
    }

    fun toggleBugResolution(bug: BugDomain) {
        viewModelScope.launch {
            try {
                if (bug.isResolved) {
                    repository.reopenBug(bug.id)
                } else {
                    repository.resolveBug(bug.id)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Failed to update bug status")
                }
            }
        }
    }

    fun deleteBug(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteBug(id)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Failed to delete bug")
                }
            }
        }
    }
}
