package com.studentos.feature.intelligence.presentation.state

import com.studentos.core.database.entity.GoalEntity
import com.studentos.feature.intelligence.domain.model.GoalProgress

enum class GoalsTab {
    ACTIVE,
    ARCHIVED
}

sealed class GoalDialogState {
    object Create : GoalDialogState()
    data class Edit(val goal: GoalEntity) : GoalDialogState()
}

data class TrajectoryGoalsUiState(
    val activeGoals: List<GoalProgress> = emptyList(),
    val archivedGoals: List<GoalProgress> = emptyList(),
    val selectedTab: GoalsTab = GoalsTab.ACTIVE,
    val expandedGraphGoalIds: Set<Long> = emptySet(),
    val dialogState: GoalDialogState? = null,
    val deleteConfirmGoal: GoalEntity? = null,
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val errorMessage: String? = null
)
