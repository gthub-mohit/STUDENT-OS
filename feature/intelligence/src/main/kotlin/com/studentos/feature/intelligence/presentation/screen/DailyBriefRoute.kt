package com.studentos.feature.intelligence.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studentos.feature.intelligence.presentation.viewmodel.DailyBriefViewModel

@Composable
fun DailyBriefRoute(
    onHistoryClick: () -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DailyBriefViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DailyBriefScreen(
        uiState = uiState,
        onGenerateClick = { viewModel.generateTodayBrief() },
        onRetryClick = { viewModel.loadTodayBrief() },
        onHistoryClick = onHistoryClick,
        onNavigate = onNavigate,
        modifier = modifier
    )
}
