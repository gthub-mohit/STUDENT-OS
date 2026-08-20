package com.studentos.feature.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.studentos.feature.settings.presentation.screen.AiDiagnosticsRoute
import com.studentos.feature.settings.presentation.screen.SettingsRoute

fun NavGraphBuilder.settingsNavGraph(navController: NavHostController) {
    composable(route = "settings/main") {
        SettingsRoute(
            onBackClick = {
                navController.popBackStack()
            },
            onNavigateToAiDiagnostics = {
                navController.navigate("settings/ai-diagnostics")
            }
        )
    }

    composable(route = "settings/ai-diagnostics") {
        AiDiagnosticsRoute(
            onBackClick = {
                navController.popBackStack()
            }
        )
    }
}
