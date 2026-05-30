package com.codespace.ide.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.codespace.ide.ui.screens.AuthScreen
import com.codespace.ide.ui.screens.HomeScreen
import com.codespace.ide.ui.screens.ProjectShellScreen
import com.codespace.ide.ui.screens.SettingsScreen

/** Top-level routes. */
object Routes {
    const val AUTH = "auth"
    const val HOME = "home"
    const val PROJECT = "project/{projectId}"
    const val SETTINGS = "settings"
    fun project(id: String) = "project/$id"
}

@Composable
fun CodeSpaceApp() {
    // Theme toggle is persisted via DataStore in the real app; mirrored here for preview.
    var darkOverride by remember { mutableStateOf<Boolean?>(null) }
    val dark = darkOverride ?: isSystemInDarkTheme()

    CodeSpaceTheme(darkTheme = dark) {
        val nav = rememberNavController()
        NavHost(navController = nav, startDestination = Routes.AUTH) {
            composable(Routes.AUTH) {
                AuthScreen(onAuthenticated = {
                    nav.navigate(Routes.HOME) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                })
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenProject = { id -> nav.navigate(Routes.project(id)) },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.PROJECT) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId").orEmpty()
                ProjectShellScreen(
                    projectId = projectId,
                    isDark = dark,
                    onToggleTheme = { darkOverride = !dark },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    isDark = dark,
                    onToggleTheme = { darkOverride = !dark },
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}
