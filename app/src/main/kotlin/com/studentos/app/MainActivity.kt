package com.studentos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentOsAppContent() {
    val navController = rememberNavController()

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Student OS") },
                        actions = {
                            IconButton(onClick = { navController.navigate("settings/main") }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings"
                                )
                            }
                        }
                    )
                },
                bottomBar = {
                    BottomNavBar(navController = navController)
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
