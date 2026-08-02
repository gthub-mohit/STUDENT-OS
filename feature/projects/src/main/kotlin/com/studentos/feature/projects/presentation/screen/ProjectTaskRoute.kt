package com.studentos.feature.projects.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studentos.feature.projects.presentation.viewmodel.ProjectTaskViewModel

@Composable
fun ProjectTaskRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectTaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProjectTaskScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onModeToggle = { viewModel.toggleParallelMode(it) },
        onCreateTaskClick = { viewModel.openCreateTaskDialog() },
        onEditTaskClick = { viewModel.openEditTaskDialog(it) },
        onToggleTaskCompletion = { viewModel.toggleTaskCompletion(it) },
        onSetNextAction = { viewModel.setNextAction(it) },
        onDeleteTaskClick = { viewModel.deleteTask(it) },
        onDismissDialog = { viewModel.dismissDialog() },
        onConfirmDialog = { title -> viewModel.saveTask(title) },
        onRetryClick = { viewModel.observeProjectAndTasks() },
        modifier = modifier
    )
}
