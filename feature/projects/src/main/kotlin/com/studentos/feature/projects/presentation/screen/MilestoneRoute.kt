package com.studentos.feature.projects.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studentos.feature.projects.presentation.viewmodel.MilestoneViewModel

@Composable
fun MilestoneRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MilestoneViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MilestoneScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onCreateMilestoneClick = { viewModel.openCreateDialog() },
        onEditMilestoneClick = { viewModel.openEditDialog(it) },
        onToggleMilestoneCompletion = { viewModel.toggleMilestoneCompletion(it) },
        onDeleteMilestoneClick = { viewModel.deleteMilestone(it) },
        onDismissDialog = { viewModel.dismissDialog() },
        onConfirmDialog = { title, description, targetDate ->
            viewModel.saveMilestone(title, description, targetDate)
        },
        onRetryClick = { viewModel.observeProjectAndMilestones() },
        modifier = modifier
    )
}
