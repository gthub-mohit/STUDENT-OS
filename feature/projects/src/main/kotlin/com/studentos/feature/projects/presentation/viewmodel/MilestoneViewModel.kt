package com.studentos.feature.projects.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.feature.projects.domain.model.MilestoneDomain
import com.studentos.feature.projects.domain.repository.ProjectRepository
import com.studentos.feature.projects.presentation.state.MilestoneUiState
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
class MilestoneViewModel @Inject constructor(
    private val repository: ProjectRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val projectId: Long = savedStateHandle.get<String>("projectId")?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(MilestoneUiState())
    val uiState: StateFlow<MilestoneUiState> = _uiState.asStateFlow()

    init {
        if (projectId > 0L) {
            observeProjectAndMilestones()
        } else {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Invalid project ID") }
        }
    }

    fun observeProjectAndMilestones() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            combine(
                repository.getProjectById(projectId),
                repository.getMilestonesForProject(projectId)
            ) { project, milestones ->
                Pair(project, milestones)
            }
            .catch { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to load milestones"
                    )
                }
            }
            .collect { (project, milestones) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        project = project,
                        milestones = milestones,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun openCreateDialog() {
        _uiState.update { it.copy(isCreateDialogOpen = true, milestoneToEdit = null) }
    }

    fun openEditDialog(milestone: MilestoneDomain) {
        _uiState.update { it.copy(isCreateDialogOpen = true, milestoneToEdit = milestone) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(isCreateDialogOpen = false, milestoneToEdit = null) }
    }

    fun saveMilestone(title: String, description: String?, targetDate: Long?) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return

        val editMilestone = _uiState.value.milestoneToEdit

        viewModelScope.launch {
            try {
                if (editMilestone == null) {
                    repository.createMilestone(projectId, trimmedTitle, description, targetDate)
                } else {
                    repository.updateMilestone(editMilestone.id, trimmedTitle, description, targetDate)
                }
                dismissDialog()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Failed to save milestone")
                }
            }
        }
    }

    fun toggleMilestoneCompletion(milestone: MilestoneDomain) {
        viewModelScope.launch {
            try {
                if (milestone.isDone) {
                    repository.reopenMilestone(milestone.id)
                } else {
                    repository.completeMilestone(milestone.id)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Failed to update milestone status")
                }
            }
        }
    }

    fun deleteMilestone(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteMilestone(id)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Failed to delete milestone")
                }
            }
        }
    }
}
