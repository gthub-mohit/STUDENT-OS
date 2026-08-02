package com.studentos.feature.projects.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studentos.feature.projects.presentation.viewmodel.ProjectsViewModel

@Composable
fun ProjectsRoute(
    onProjectClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTimeMs = viewModel.getCurrentTimeMs()

    ProjectsScreen(
        uiState = uiState,
        currentTimeMs = currentTimeMs,
        onTabSelected = { viewModel.toggleTab(it) },
        onCreateClick = { viewModel.openCreateDialog() },
        onEditClick = { viewModel.openEditDialog(it) },
        onArchiveClick = { viewModel.archiveProject(it) },
        onUnarchiveClick = { viewModel.unarchiveProject(it) },
        onProjectClick = onProjectClick,
        onDismissDialog = { viewModel.dismissDialog() },
        onConfirmDialog = { title, threshold -> viewModel.saveProject(title, threshold) },
        onRetryClick = { viewModel.observeProjects() },
        modifier = modifier
    )
}
