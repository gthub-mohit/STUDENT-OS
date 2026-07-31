package com.studentos.feature.coding.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.studentos.feature.coding.presentation.screen.CpDashboardRoute

object CodingNavGraph {
    const val ROUTE_CP_DASHBOARD = "coding/cp"
}

fun NavGraphBuilder.codingNavGraph() {
    composable(route = CodingNavGraph.ROUTE_CP_DASHBOARD) {
        CpDashboardRoute()
    }
}
