package com.studentos.feature.intelligence.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.studentos.feature.intelligence.presentation.screen.DailyBriefHistoryRoute
import com.studentos.feature.intelligence.presentation.screen.DailyBriefRoute
import com.studentos.feature.intelligence.presentation.screen.HomeRoute

fun NavGraphBuilder.intelligenceNavGraph(navController: NavHostController) {

    // ── Home Screen (start destination) ────────────────────────────────────
    composable(route = "home") {
        HomeRoute(
            onDailyBriefClick = {
                navController.navigate("intelligence/daily-brief")
            },
            onNavigate = { route ->
                navController.navigate(route)
            },
            onSettingsClick = {
                navController.navigate("settings/main")
            }
        )
    }

    // ── Daily Brief Detail Screen ──────────────────────────────────────────
    composable(
        route = "intelligence/daily-brief?date={date}",
        arguments = listOf(
            navArgument("date") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) {
        DailyBriefRoute(
            onBackClick = {
                navController.popBackStack()
            },
            onHistoryClick = {
                navController.navigate("intelligence/history")
            },
            onNavigate = { route ->
                navController.navigate(route)
            }
        )
    }

    // ── Brief History Screen ───────────────────────────────────────────────
    composable(route = "intelligence/history") {
        DailyBriefHistoryRoute(
            onBackClick = {
                navController.popBackStack()
            },
            onItemClick = { date ->
                navController.navigate("intelligence/daily-brief?date=$date")
            }
        )
    }
}
