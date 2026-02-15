package com.pothole.detection.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.padding
import com.pothole.detection.ui.detection.DetectionScreen
import com.pothole.detection.ui.history.HistoryScreen
import com.pothole.detection.ui.settings.SettingsScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Detection : Screen("detection", "Detection", Icons.Default.Place)
    data object History : Screen("history", "History", Icons.AutoMirrored.Filled.List)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val screens = listOf(Screen.Detection, Screen.History, Screen.Settings)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Detection.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Detection.route) {
                DetectionScreen()
            }
            composable(Screen.History.route) {
                HistoryScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
