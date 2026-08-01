package com.studentos.feature.intelligence.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studentos.feature.intelligence.presentation.viewmodel.DailyBriefHistoryViewModel

@Composable
fun DailyBriefHistoryRoute(
    onBackClick: () -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DailyBriefHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DailyBriefHistoryScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onRetryClick = { viewModel.loadHistory() },
        onItemClick = onItemClick,
        modifier = modifier
    )
}
