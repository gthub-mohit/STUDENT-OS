package com.studentos.feature.intelligence.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studentos.feature.intelligence.presentation.state.HomeUiState
import com.studentos.feature.intelligence.presentation.viewmodel.DailyBriefViewModel
import com.studentos.feature.intelligence.presentation.viewmodel.DailyScoreViewModel

/**
 * HomeRoute — wires [DailyBriefViewModel] and [DailyScoreViewModel]
 * to the [HomeScreen] composable.
 *
 * Derives a lightweight [HomeUiState] from both ViewModels' states
 * so the Home Screen only consumes the minimal data it needs.
 */
@Composable
fun HomeRoute(
    onDailyBriefClick: () -> Unit,
    onNavigate: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    briefViewModel: DailyBriefViewModel = hiltViewModel(),
    scoreViewModel: DailyScoreViewModel = hiltViewModel()
) {
    val briefState by briefViewModel.uiState.collectAsStateWithLifecycle()
    val scoreState by scoreViewModel.uiState.collectAsStateWithLifecycle()

    val homeUiState = remember(briefState, scoreState) {
        val hasBrief = briefState.dailyBrief != null
        val goalSummary = when {
            briefState.isLoading || briefState.isGenerating -> "Loading your daily brief…"
            !hasBrief -> "Generate your daily brief to get started"
            scoreState.targetScore > 0 && scoreState.remainingScore > 0 ->
                "Complete ${scoreState.remainingScore} more points to reach your goal"
            scoreState.targetScore > 0 && scoreState.remainingScore <= 0 ->
                "You've reached your goal today! 🎉"
            else -> "Tap to see today's plan"
        }
        HomeUiState(
            isLoading = briefState.isLoading || scoreState.isLoading,
            hasBrief = hasBrief,
            currentScore = scoreState.currentScore,
            targetScore = scoreState.targetScore,
            progressBarValue = scoreState.progressBarValue,
            todayGoalSummary = goalSummary,
            topRecommendations = briefState.recommendations.take(3),
            errorMessage = briefState.errorMessage ?: scoreState.errorMessage
        )
    }

    val isRefreshing = briefState.isGenerating

    HomeScreen(
        uiState = homeUiState,
        isRefreshing = isRefreshing,
        onRefresh = { briefViewModel.generateTodayBrief() },
        onHeroClick = onDailyBriefClick,
        onRecommendationClick = onNavigate,
        onSettingsClick = onSettingsClick,
        onQuickNavClick = onNavigate,
        modifier = modifier
    )
}
