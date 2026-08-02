package com.studentos.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
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

/**
 * Default feature module navigation graph implementations for Student OS.
 * These establish the initial module graphs registered into [ModuleRegistry].
 */
private fun initDefaultModuleRegistry() {
    if (ModuleRegistry.graphs.isNotEmpty()) return

    // 1. Intelligence / Daily Brief (Real Feature Graph Integrated)
    ModuleRegistry.register(object : ModuleNavGraph {
        override val baseRoute = "intelligence"
        override val navItem = NavigationItem(
            route = "intelligence/daily-brief",
            title = "Daily Brief",
            icon = Icons.Default.Home
        )
        override fun registerGraph(builder: NavGraphBuilder, navController: NavHostController) {
            builder.intelligenceNavGraph(navController)
        }
    })

    // 2. Attendance Module (Real Feature Graph Integrated)
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

    // 3. Assignments Module (Real Feature Graph Integrated)
    ModuleRegistry.register(object : ModuleNavGraph {
        override val baseRoute = "assignments"
        override val navItem = NavigationItem(
            route = "assignments/list",
            title = "Assignments",
            icon = Icons.Default.Edit
        )
        override fun registerGraph(builder: NavGraphBuilder, navController: NavHostController) {
            builder.assignmentsNavGraph(navController)
        }
    })

    // 4. Coding Module (Real Feature Graph Integrated)
    ModuleRegistry.register(object : ModuleNavGraph {
        override val baseRoute = "coding"
        override val navItem = NavigationItem(
            route = CodingNavGraph.ROUTE_CP_DASHBOARD,
            title = "Coding",
            icon = Icons.Default.Build
        )
        override fun registerGraph(builder: NavGraphBuilder, navController: NavHostController) {
            builder.codingNavGraph(navController)
        }
    })

    // 5. Projects Module (Placeholder - Group 7)
    ModuleRegistry.register(object : ModuleNavGraph {
        override val baseRoute = "projects"
        override val navItem = NavigationItem(
            route = "projects/list",
            title = "Projects",
            icon = Icons.Default.List
        )
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

    // 6. Settings Module (Placeholder - Group 8)
    ModuleRegistry.register(object : ModuleNavGraph {
        override val baseRoute = "settings"
        override val navItem = null
        override fun registerGraph(builder: NavGraphBuilder, navController: NavHostController) {
            builder.composable("settings/main") {
                PlaceholderScreen(
                    title = "Settings Screen",
                    subtitle = "Route: settings/main"
                )
            }
        }
    })
}

/**
 * AppNavHost — root Compose navigation graph for Student OS.
 *
 * Dynamically builds the NavHost by querying [ModuleRegistry] for all registered feature graphs.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = "intelligence/daily-brief"
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
