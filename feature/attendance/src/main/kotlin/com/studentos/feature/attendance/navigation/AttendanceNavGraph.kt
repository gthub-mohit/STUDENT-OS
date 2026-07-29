package com.studentos.feature.attendance.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.studentos.feature.attendance.presentation.screen.AttendanceAnalyticsScreen
import com.studentos.feature.attendance.presentation.screen.CalendarViewScreen
import com.studentos.feature.attendance.presentation.screen.OcrPreviewScreen
import com.studentos.feature.attendance.presentation.screen.WeeklyViewScreen
import com.studentos.feature.attendance.presentation.viewmodel.AttendanceAnalyticsViewModel
import com.studentos.feature.attendance.presentation.viewmodel.CalendarViewModel
import com.studentos.feature.attendance.presentation.viewmodel.OcrViewModel
import com.studentos.feature.attendance.presentation.viewmodel.WeeklyViewModel

/**
 * attendanceNavGraph — Navigation graph builder registering routes for Attendance feature.
 */
fun NavGraphBuilder.attendanceNavGraph(navController: NavHostController) {
    composable(route = "weekly") {
        val viewModel: WeeklyViewModel = hiltViewModel()
        WeeklyViewScreen(
            viewModel = viewModel,
            onNavigateToCalendar = { navController.navigate("calendar") },
            onNavigateToAnalytics = { navController.navigate("analytics") }
        )
    }

    composable(route = "calendar") {
        val viewModel: CalendarViewModel = hiltViewModel()
        CalendarViewScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(route = "analytics") {
        val viewModel: AttendanceAnalyticsViewModel = hiltViewModel()
        AttendanceAnalyticsScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(route = "ocr-preview") {
        val viewModel: OcrViewModel = hiltViewModel()
        OcrPreviewScreen(
            viewModel = viewModel,
            onImportFinished = {
                navController.popBackStack()
            }
        )
    }
}
