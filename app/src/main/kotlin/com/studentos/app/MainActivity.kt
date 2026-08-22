package com.studentos.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.studentos.app.navigation.AppNavHost
import com.studentos.app.navigation.BottomNavBar
import com.studentos.app.navigation.initDefaultModuleRegistry
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity — the single Activity for Student OS.
 *
 * Responsibilities:
 *  - Entry point; sets up Compose content host with Material 3 theme and Scaffold.
 *  - Annotated with @AndroidEntryPoint for Hilt field injection.
 *  - Hosts [AppNavHost] (the root Compose navigation graph) and [BottomNavBar].
 *  - Handles deep-linking and intent routing from notifications.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pre-initialize navigation modules so bottom navigation items are available immediately on first frame.
        initDefaultModuleRegistry()

        // Extract deep-link route from launch intent
        pendingRoute = extractRoute(intent)

        // Draw content edge-to-edge under system bars (Material 3 recommendation).
        enableEdgeToEdge()

        setContent {
            StudentOsAppContent(
                initialRoute = pendingRoute,
                onRouteHandled = { pendingRoute = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractRoute(intent)?.let { route ->
            pendingRoute = route
        }
    }

    private fun extractRoute(intent: Intent?): String? {
        if (intent == null) return null
        val extraRoute = intent.getStringExtra("route")
        if (!extraRoute.isNullOrBlank()) return extraRoute

        val data: Uri? = intent.data
        if (data != null && data.scheme == "studentos") {
            val path = data.toString().removePrefix("studentos://").trim()
            if (path.isNotBlank()) return path
        }
        return null
    }
}

/** Routes where the bottom navigation bar should be hidden (detail screens). */
private val detailRoutes = setOf(
    "intelligence/daily-brief?date={date}",
    "intelligence/history",
    "settings/main",
    "settings/ai-diagnostics"
)

@Composable
private fun StudentOsAppContent(
    initialRoute: String? = null,
    onRouteHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute !in detailRoutes

    LaunchedEffect(initialRoute) {
        initialRoute?.let { route ->
            navController.navigate(route)
            onRouteHandled()
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        BottomNavBar(navController = navController)
                    }
                }
            ) { innerPadding ->
                AppNavHost(
                    navController = navController,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
