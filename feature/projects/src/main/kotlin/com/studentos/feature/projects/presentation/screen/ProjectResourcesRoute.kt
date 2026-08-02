package com.studentos.feature.projects.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studentos.feature.projects.presentation.viewmodel.ProjectResourcesViewModel

@Composable
fun ProjectResourcesRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectResourcesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProjectResourcesScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onCreateResourceClick = { viewModel.openCreateDialog() },
        onEditResourceClick = { viewModel.openEditDialog(it) },
        onDeleteResourceClick = { viewModel.deleteResource(it) },
        onDismissDialog = { viewModel.dismissDialog() },
        onConfirmDialog = { url, label, type ->
            viewModel.saveResource(url, label, type)
        },
        onRetryClick = { viewModel.observeProjectAndResources() },
        modifier = modifier
    )
}
