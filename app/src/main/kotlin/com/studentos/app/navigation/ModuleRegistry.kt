package com.studentos.app.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController

/**
 * NavigationItem — metadata for a bottom navigation tab destination.
 *
 * @param route The unique destination route (e.g., "home", "weekly", "assignments/list")
 * @param title Human-readable tab label displayed in [BottomNavBar]
 * @param icon Material [ImageVector] for the tab icon
 */
data class NavigationItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

/**
 * ModuleNavGraph — contract that every feature module navigation graph implements.
 *
 * Each feature module registers its navigation routes and optional bottom navigation
 * metadata through this interface.
 */
interface ModuleNavGraph {
    val baseRoute: String
    val navItem: NavigationItem?
    fun registerGraph(builder: NavGraphBuilder, navController: NavHostController)
}

/**
 * ModuleRegistry — central registry for all feature module navigation graphs in Student OS.
 */
object ModuleRegistry {

    private val registeredGraphs = mutableListOf<ModuleNavGraph>()

    /**
     * All currently registered [ModuleNavGraph] instances.
     */
    val graphs: List<ModuleNavGraph> get() = registeredGraphs

    /**
     * List of [NavigationItem] tabs for the bottom navigation bar.
     */
    val bottomNavItems: List<NavigationItem>
        get() = registeredGraphs.mapNotNull { it.navItem }

    /**
     * Register a new [ModuleNavGraph] into the registry.
     */
    fun register(graph: ModuleNavGraph) {
        if (registeredGraphs.none { it.baseRoute == graph.baseRoute }) {
            registeredGraphs.add(graph)
        }
    }

    /**
     * Clear registered graphs.
     */
    fun clear() {
        registeredGraphs.clear()
    }
}
