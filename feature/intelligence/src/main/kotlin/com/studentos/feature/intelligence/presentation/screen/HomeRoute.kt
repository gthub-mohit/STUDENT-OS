package com.studentos.feature.intelligence.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studentos.feature.intelligence.presentation.viewmodel.HomeViewModel

/**
 * HomeRoute — Wires [HomeViewModel] to the [HomeScreen] composable.
 */
@Composable
fun HomeRoute(
    onDailyBriefClick: () -> Unit,
    onNavigate: (String) -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onViewAllComingUpClick: () -> Unit = { onNavigate("assignments/list") },
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        isRefreshing = uiState.isGenerating,
        onRefresh = { homeViewModel.generateTodayBrief() },
        onHeroClick = onDailyBriefClick,
        onToggleFocusItem = { item -> homeViewModel.toggleFocusItem(item) },
        onFocusItemClick = onNavigate,
        onComingUpItemClick = onNavigate,
        onViewAllComingUpClick = onViewAllComingUpClick,
        onHistoryClick = onHistoryClick,
        onSettingsClick = onSettingsClick,
        modifier = modifier
    )
}
