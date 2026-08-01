package com.studentos.feature.intelligence.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.studentos.feature.intelligence.presentation.screen.DailyBriefHistoryRoute
import com.studentos.feature.intelligence.presentation.screen.DailyBriefRoute

fun NavGraphBuilder.intelligenceNavGraph(navController: NavHostController) {
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
            onHistoryClick = {
                navController.navigate("intelligence/history")
            },
            onNavigate = { route ->
                navController.navigate(route)
            }
        )
    }

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
