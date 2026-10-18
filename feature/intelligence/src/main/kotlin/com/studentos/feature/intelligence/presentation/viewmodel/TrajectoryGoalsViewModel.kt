package com.studentos.feature.intelligence.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.core.database.dao.ActivityLogDao
import com.studentos.core.database.dao.GoalDao
import com.studentos.core.database.entity.GoalEntity
import com.studentos.feature.intelligence.domain.usecase.CalculateGoalProgressUseCase
import com.studentos.feature.intelligence.domain.usecase.ManageGoalsUseCase
import com.studentos.feature.intelligence.domain.usecase.SaveGoalUseCase
import com.studentos.feature.intelligence.presentation.state.GoalDialogState
import com.studentos.feature.intelligence.presentation.state.GoalsTab
import com.studentos.feature.intelligence.presentation.state.TrajectoryGoalsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrajectoryGoalsViewModel @Inject constructor(
    private val goalDao: GoalDao,
    private val activityLogDao: ActivityLogDao,
    private val calculateGoalProgressUseCase: CalculateGoalProgressUseCase,
    private val saveGoalUseCase: SaveGoalUseCase,
    private val manageGoalsUseCase: ManageGoalsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrajectoryGoalsUiState(isLoading = true))
    val uiState: StateFlow<TrajectoryGoalsUiState> = _uiState.asStateFlow()

    init {
        observeGoalsAndLogs()
    }

    private fun observeGoalsAndLogs() {
        combine(
            goalDao.observeActiveGoals(),
            goalDao.observeArchivedGoals(),
            activityLogDao.observeAllLogs()
        ) { activeGoals, archivedGoals, allLogs ->
            val now = System.currentTimeMillis()
            val activeWithProgress = activeGoals.map { goal ->
                calculateGoalProgressUseCase(goal = goal, logs = allLogs, currentTimeMs = now)
            }
            val archivedWithProgress = archivedGoals.map { goal ->
                calculateGoalProgressUseCase(goal = goal, logs = allLogs, currentTimeMs = now)
            }

            activeWithProgress to archivedWithProgress
        }.onEach { (active, archived) ->
            _uiState.update {
                it.copy(
                    activeGoals = active,
                    archivedGoals = archived,
                    isLoading = false
                )
            }
        }.catch { e ->
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Failed to load goals: ${e.message}"
                )
            }
        }.launchIn(viewModelScope)
    }

    fun selectTab(tab: GoalsTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun toggleGraphExpanded(goalId: Long) {
        _uiState.update { state ->
            val newSet = if (state.expandedGraphGoalIds.contains(goalId)) {
                state.expandedGraphGoalIds - goalId
            } else {
                state.expandedGraphGoalIds + goalId
            }
            state.copy(expandedGraphGoalIds = newSet)
        }
    }

    fun openCreateGoalDialog() {
        _uiState.update { it.copy(dialogState = GoalDialogState.Create) }
    }

    fun openEditGoalDialog(goal: GoalEntity) {
        _uiState.update { it.copy(dialogState = GoalDialogState.Edit(goal)) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(dialogState = null) }
    }

    fun saveGoal(
        id: Long = 0,
        title: String,
        category: String,
        metricType: String,
        unit: String,
        startValue: Double,
        targetValue: Double,
        startDate: Long,
        targetDate: Long,
        isArchived: Boolean = false
    ) {
        viewModelScope.launch {
            val result = saveGoalUseCase(
                id = id,
                title = title,
                category = category,
                metricType = metricType,
                unit = unit,
                startValue = startValue,
                targetValue = targetValue,
                startDate = startDate,
                targetDate = targetDate,
                isArchived = isArchived
            )

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        dialogState = null,
                        userMessage = if (id == 0L) "Goal created successfully!" else "Goal updated successfully!",
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Failed to save goal.")
                }
            }
        }
    }

    fun toggleArchive(goal: GoalEntity) {
        viewModelScope.launch {
            val targetArchived = !goal.isArchived
            val result = manageGoalsUseCase.setArchived(goal.id, targetArchived)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        userMessage = if (targetArchived) "Goal moved to archive." else "Goal restored to active.",
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = "Could not update archive status: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun promptDelete(goal: GoalEntity) {
        _uiState.update { it.copy(deleteConfirmGoal = goal) }
    }

    fun dismissDeletePrompt() {
        _uiState.update { it.copy(deleteConfirmGoal = null) }
    }

    fun confirmDelete() {
        val goal = _uiState.value.deleteConfirmGoal ?: return
        viewModelScope.launch {
            val result = manageGoalsUseCase.deleteGoal(goal.id)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        deleteConfirmGoal = null,
                        userMessage = "Goal deleted. Historical activities preserved.",
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        deleteConfirmGoal = null,
                        errorMessage = "Could not delete goal: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(userMessage = null, errorMessage = null) }
    }
}
