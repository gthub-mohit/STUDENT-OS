package com.studentos.feature.coding.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.studentos.feature.coding.presentation.screen.ContestReflectionRoute
import com.studentos.feature.coding.presentation.screen.CpDashboardRoute
import com.studentos.feature.coding.presentation.screen.KnowledgeTreeRoute

object CodingNavGraph {
    const val ROUTE_CP_DASHBOARD = "coding/cp"
    const val ROUTE_CONTEST_REFLECTION = "coding/reflection/{contestId}"
    const val ROUTE_KNOWLEDGE_TREE = "coding/knowledge-tree"

    fun contestReflectionRoute(contestId: Long) = "coding/reflection/$contestId"
}

fun NavGraphBuilder.codingNavGraph(navController: NavController) {
    composable(route = CodingNavGraph.ROUTE_CP_DASHBOARD) {
        CpDashboardRoute()
    }

    composable(
        route = CodingNavGraph.ROUTE_CONTEST_REFLECTION,
        arguments = listOf(navArgument("contestId") { type = NavType.LongType })
    ) {
        ContestReflectionRoute(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(route = CodingNavGraph.ROUTE_KNOWLEDGE_TREE) {
        KnowledgeTreeRoute(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
