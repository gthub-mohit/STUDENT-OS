package com.studentos.feature.assignments.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.studentos.feature.assignments.presentation.screen.AssignmentDetailScreen
import com.studentos.feature.assignments.presentation.screen.AssignmentListScreen
import com.studentos.feature.assignments.presentation.viewmodel.AssignmentDetailViewModel
import com.studentos.feature.assignments.presentation.viewmodel.AssignmentListViewModel

/**
 * assignmentsNavGraph — Navigation graph builder registering routes for Assignments feature.
 */
fun NavGraphBuilder.assignmentsNavGraph(navController: NavHostController) {
    composable(route = "assignments/list") {
        val viewModel: AssignmentListViewModel = hiltViewModel()
        AssignmentListScreen(
            viewModel = viewModel,
            onNavigateToDetail = { id ->
                navController.navigate("assignments/detail/$id")
            }
        )
    }

    composable(route = "assignments/detail/{id}") {
        val viewModel: AssignmentDetailViewModel = hiltViewModel()
        AssignmentDetailScreen(
            viewModel = viewModel,
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }
}
