package com.studentos.feature.projects.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.feature.projects.domain.model.ProjectTaskDomain
import com.studentos.feature.projects.domain.repository.ProjectRepository
import com.studentos.feature.projects.presentation.state.ProjectTaskUiState
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
class ProjectTaskViewModel @Inject constructor(
    private val repository: ProjectRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val projectId: Long = savedStateHandle.get<String>("projectId")?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(ProjectTaskUiState())
    val uiState: StateFlow<ProjectTaskUiState> = _uiState.asStateFlow()

    init {
        if (projectId > 0L) {
            observeProjectAndTasks()
        } else {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Invalid project ID") }
        }
    }

    fun observeProjectAndTasks() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            combine(
                repository.getProjectById(projectId),
                repository.getTasksForProject(projectId)
            ) { project, tasks ->
                Pair(project, tasks)
            }
            .catch { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to load task details"
                    )
                }
            }
            .collect { (project, tasks) ->
                val isParallel = tasks.firstOrNull()?.isParallel ?: false
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        project = project,
                        tasks = tasks,
                        isParallelMode = isParallel,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun openCreateTaskDialog() {
        _uiState.update { it.copy(isCreateTaskDialogOpen = true, taskToEdit = null) }
    }

    fun openEditTaskDialog(task: ProjectTaskDomain) {
        _uiState.update { it.copy(isCreateTaskDialogOpen = true, taskToEdit = task) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(isCreateTaskDialogOpen = false, taskToEdit = null) }
    }

    fun saveTask(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return

        val editTask = _uiState.value.taskToEdit
        val currentParallel = _uiState.value.isParallelMode

        viewModelScope.launch {
            try {
                if (editTask == null) {
                    repository.createTask(projectId, trimmed, currentParallel)
                } else {
                    repository.updateTask(editTask.id, trimmed)
                }
                dismissDialog()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Failed to save task")
                }
            }
        }
    }

    fun toggleTaskCompletion(task: ProjectTaskDomain) {
        viewModelScope.launch {
            try {
                if (task.isCompleted) {
                    repository.reopenTask(task.id)
                } else {
                    repository.completeTask(task.id)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Failed to update task completion")
                }
            }
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteTask(taskId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Failed to delete task")
                }
            }
        }
    }

    fun setNextAction(taskId: Long) {
        viewModelScope.launch {
            try {
                repository.setNextAction(projectId, taskId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Failed to set next action")
                }
            }
        }
    }

    fun toggleParallelMode(isParallel: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleParallelMode(projectId, isParallel)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Failed to update mode")
                }
            }
        }
    }
}
