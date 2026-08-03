package com.studentos.app

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.studentos.app.navigation.AppNavHost
import com.studentos.app.navigation.BottomNavBar
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity — the single Activity for Student OS.
 *
 * Responsibilities:
 *  - Entry point; sets up Compose content host with Material 3 theme and Scaffold.
 *  - Annotated with @AndroidEntryPoint for Hilt field injection.
 *  - Hosts [AppNavHost] (the root Compose navigation graph) and [BottomNavBar].
 *
 * All routing is managed by the Navigation Component inside [AppNavHost].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draw content edge-to-edge under system bars (Material 3 recommendation).
        enableEdgeToEdge()

        setContent {
            StudentOsAppContent()
        }
    }
}

/** Routes where the bottom navigation bar should be hidden (detail screens). */
private val detailRoutes = setOf(
    "intelligence/daily-brief?date={date}",
    "intelligence/history",
    "settings/main"
)

@Composable
private fun StudentOsAppContent() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute !in detailRoutes

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

