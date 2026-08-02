package com.studentos.feature.projects.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studentos.feature.projects.presentation.viewmodel.BugTrackerViewModel

@Composable
fun BugTrackerRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BugTrackerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BugTrackerScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onStatusFilterSelected = { viewModel.setStatusFilter(it) },
        onSeverityFilterSelected = { viewModel.setSeverityFilter(it) },
        onCreateBugClick = { viewModel.openCreateDialog() },
        onEditBugClick = { viewModel.openEditDialog(it) },
        onToggleBugResolution = { viewModel.toggleBugResolution(it) },
        onDeleteBugClick = { viewModel.deleteBug(it) },
        onDismissDialog = { viewModel.dismissDialog() },
        onConfirmDialog = { description, severity ->
            viewModel.saveBug(description, severity)
        },
        onRetryClick = { viewModel.observeProjectAndBugs() },
        modifier = modifier
    )
}
