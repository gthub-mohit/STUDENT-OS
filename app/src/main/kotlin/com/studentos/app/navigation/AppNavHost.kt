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

/**
 * Default feature module navigation graph implementations for Student OS.
 * These establish the initial module graphs registered into [ModuleRegistry].
 */
private fun initDefaultModuleRegistry() {
    if (ModuleRegistry.graphs.isNotEmpty()) return

    // 1. Intelligence / Daily Brief
    ModuleRegistry.register(object : ModuleNavGraph {
        override val baseRoute = "intelligence"
        override val navItem = NavigationItem(
            route = "intelligence/daily-brief",
            title = "Daily Brief",
            icon = Icons.Default.Home
        )
        override fun registerGraph(builder: NavGraphBuilder, navController: NavHostController) {
            builder.composable("intelligence/daily-brief") {
                PlaceholderScreen(
                    title = "Daily Brief Screen",
                    subtitle = "Route: intelligence/daily-brief",
                    onAction = { navController.navigate("intelligence/history") },
                    actionLabel = "Go to History"
                )
            }
            builder.composable("intelligence/history") {
                PlaceholderScreen(
                    title = "Brief History Screen",
                    subtitle = "Route: intelligence/history"
                )
            }
        }
    })

    // 2. Attendance Module
    ModuleRegistry.register(object : ModuleNavGraph {
        override val baseRoute = "attendance"
        override val navItem = NavigationItem(
            route = "attendance/weekly",
            title = "Attendance",
            icon = Icons.Default.DateRange
        )
        override fun registerGraph(builder: NavGraphBuilder, navController: NavHostController) {
            builder.composable("attendance/weekly") {
                PlaceholderScreen(
                    title = "Attendance Weekly Screen",
                    subtitle = "Route: attendance/weekly",
                    onAction = { navController.navigate("attendance/calendar") },
                    actionLabel = "Go to Calendar"
                )
            }
            builder.composable("attendance/calendar") {
                PlaceholderScreen(
                    title = "Attendance Calendar Screen",
                    subtitle = "Route: attendance/calendar"
                )
            }
        }
    })

    // 3. Assignments Module
    ModuleRegistry.register(object : ModuleNavGraph {
        override val baseRoute = "assignments"
        override val navItem = NavigationItem(
            route = "assignments/list",
            title = "Assignments",
            icon = Icons.Default.Edit
        )
        override fun registerGraph(builder: NavGraphBuilder, navController: NavHostController) {
            builder.composable("assignments/list") {
                PlaceholderScreen(
                    title = "Assignments List Screen",
                    subtitle = "Route: assignments/list",
                    onAction = { navController.navigate("assignments/detail/1") },
                    actionLabel = "View Assignment #1"
                )
            }
            builder.composable("assignments/detail/{id}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: "0"
                PlaceholderScreen(
                    title = "Assignment Detail #$id",
                    subtitle = "Route: assignments/detail/$id"
                )
            }
        }
    })

    // 4. Coding Module
    ModuleRegistry.register(object : ModuleNavGraph {
        override val baseRoute = "coding"
        override val navItem = NavigationItem(
            route = "coding/dashboard",
            title = "Coding",
            icon = Icons.Default.Build
        )
        override fun registerGraph(builder: NavGraphBuilder, navController: NavHostController) {
            builder.composable("coding/dashboard") {
                PlaceholderScreen(
                    title = "Coding Dashboard Screen",
                    subtitle = "Route: coding/dashboard",
                    onAction = { navController.navigate("coding/knowledge-tree") },
                    actionLabel = "View Knowledge Tree"
                )
            }
            builder.composable("coding/knowledge-tree") {
                PlaceholderScreen(
                    title = "DSA Knowledge Tree",
                    subtitle = "Route: coding/knowledge-tree"
                )
            }
        }
    })

    // 5. Projects Module
    ModuleRegistry.register(object : ModuleNavGraph {
        override val baseRoute = "projects"
        override val navItem = NavigationItem(
            route = "projects/list",
            title = "Projects",
            icon = Icons.Default.List
        )
        override fun registerGraph(builder: NavGraphBuilder, navController: NavHostController) {
            builder.composable("projects/list") {
                PlaceholderScreen(
                    title = "Projects List Screen",
                    subtitle = "Route: projects/list",
                    onAction = { navController.navigate("projects/detail/101") },
                    actionLabel = "View Project #101"
                )
            }
            builder.composable("projects/detail/{projectId}") { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: "0"
                PlaceholderScreen(
                    title = "Project Detail #$projectId",
                    subtitle = "Route: projects/detail/$projectId"
                )
            }
        }
    })

    // 6. Settings Module (Not in bottom navigation)
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
 * PlaceholderScreen — reusable temporary composable for task 0.4 verification.
 */
@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String,
    onAction: (() -> Unit)? = null,
    actionLabel: String = ""
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
