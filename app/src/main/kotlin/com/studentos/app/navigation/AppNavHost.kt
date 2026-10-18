package com.studentos.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.studentos.feature.assignments.navigation.assignmentsNavGraph
import com.studentos.feature.attendance.navigation.attendanceNavGraph
import com.studentos.feature.coding.navigation.CodingNavGraph
import com.studentos.feature.coding.navigation.codingNavGraph
import com.studentos.feature.intelligence.navigation.intelligenceNavGraph

import com.studentos.feature.settings.navigation.settingsNavGraph

import androidx.compose.material.icons.filled.Star

/**
 * Initializes the default module registry graphs and bottom navigation destinations.
 * Safe to call multiple times (idempotent).
 */
fun initDefaultModuleRegistry() {
    if (ModuleRegistry.graphs.isNotEmpty()) return

    // 1. Focus & Journal (Tab 1 — Start Destination)
    ModuleRegistry.register(object : ModuleNavGraph {
        override val baseRoute = "intelligence"
        override val navItem = NavigationItem(
            route = "focus",
            title = "Focus",
            icon = Icons.Default.Edit
        )
        override fun registerGraph(builder: NavGraphBuilder, navController: NavHostController) {
            builder.intelligenceNavGraph(navController)
        }
    })

    // 2. Trajectory & Goals (Tab 2)
    ModuleRegistry.register(object : ModuleNavGraph {
        override val baseRoute = "trajectory"
        override val navItem = NavigationItem(
            route = "trajectory",
            title = "Goals",
            icon = Icons.Default.Star
        )
        override fun registerGraph(builder: NavGraphBuilder, navController: NavHostController) {
            builder.composable("trajectory") {
                com.studentos.feature.intelligence.presentation.screen.TrajectoryGoalsRoute(
                    onSettingsClick = { navController.navigate("settings/main") }
                )
            }
        }
    })

    // 3. Attendance Module (Tab 3 — Preserved)
    ModuleRegistry.register(object : ModuleNavGraph {
        override val baseRoute = "attendance"
        override val navItem = NavigationItem(
            route = "weekly",
            title = "Attendance",
            icon = Icons.Default.DateRange
        )
        override fun registerGraph(builder: NavGraphBuilder, navController: NavHostController) {
            builder.attendanceNavGraph(navController)
        }
    })

    // 4. Assignments Module (Background graph)
    ModuleRegistry.register(object : ModuleNavGraph {
        override val baseRoute = "assignments"
        override val navItem = null
        override fun registerGraph(builder: NavGraphBuilder, navController: NavHostController) {
            builder.assignmentsNavGraph(navController)
        }
    })

    // 5. Coding Module (Background graph)
    ModuleRegistry.register(object : ModuleNavGraph {
        override val baseRoute = "coding"
        override val navItem = null
        override fun registerGraph(builder: NavGraphBuilder, navController: NavHostController) {
            builder.codingNavGraph(navController)
        }
    })

    // 6. Projects Module (Background graph)
    ModuleRegistry.register(object : ModuleNavGraph {
        override val baseRoute = "projects"
        override val navItem = null
        override fun registerGraph(builder: NavGraphBuilder, navController: NavHostController) {
            builder.composable("projects/list") {
                com.studentos.feature.projects.presentation.screen.ProjectsRoute(
                    onProjectClick = { projectId ->
                        navController.navigate("projects/detail/$projectId")
                    }
                )
            }
            builder.composable("projects/detail/{projectId}") {
                com.studentos.feature.projects.presentation.screen.ProjectTaskRoute(
                    onBackClick = { navController.popBackStack() }
                )
            }
            builder.composable("projects/milestones/{projectId}") {
                com.studentos.feature.projects.presentation.screen.MilestoneRoute(
                    onBackClick = { navController.popBackStack() }
                )
            }
            builder.composable("projects/bugs/{projectId}") {
                com.studentos.feature.projects.presentation.screen.BugTrackerRoute(
                    onBackClick = { navController.popBackStack() }
                )
            }
            builder.composable("projects/resources/{projectId}") {
                com.studentos.feature.projects.presentation.screen.ProjectResourcesRoute(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    })

    // 7. Settings Module (Accessible via top bar)
    ModuleRegistry.register(object : ModuleNavGraph {
        override val baseRoute = "settings"
        override val navItem = null
        override fun registerGraph(builder: NavGraphBuilder, navController: NavHostController) {
            builder.settingsNavGraph(navController)
        }
    })
}

/**
 * AppNavHost — root Compose navigation graph for Student OS.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = "focus"
) {
    initDefaultModuleRegistry()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        ModuleRegistry.graphs.forEach { graph ->
            graph.registerGraph(this, navController)
        }
    }
}

/**
 * Temporary UI placeholder screen for unbuilt feature modules.
 */
@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String,
    onAction: (() -> Unit)? = null,
    actionLabel: String = "Action"
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAction) {
                Text(text = actionLabel)
            }
        }
    }
}
