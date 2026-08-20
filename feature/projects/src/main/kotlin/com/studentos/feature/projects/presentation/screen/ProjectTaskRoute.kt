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
        onCreateTaskClick = { viewModel.openCreateTaskDialog() },
        onEditTaskClick = { viewModel.openEditTaskDialog(it) },
        onToggleTaskCompletion = { viewModel.toggleTaskCompletion(it) },
        onDeleteTaskClick = { viewModel.deleteTask(it) },
        onFilterClick = { viewModel.openFilterSheet() },
        onFilterDismiss = { viewModel.dismissFilterSheet() },
        onFilterApply = { viewModel.setStatusFilter(it) },
        onClearFilters = { viewModel.clearFilters() },
        onDismissDialog = { viewModel.dismissDialog() },
        onConfirmDialog = { title, dependencyTaskId, priority, deadline ->
            viewModel.saveTask(title, dependencyTaskId, priority, deadline)
        },
        onRetryClick = { viewModel.observeProjectAndTasks() },
        modifier = modifier
    )
}
