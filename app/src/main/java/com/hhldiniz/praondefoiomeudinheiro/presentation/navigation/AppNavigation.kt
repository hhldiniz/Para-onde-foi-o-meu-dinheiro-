package com.hhldiniz.praondefoiomeudinheiro.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home.HomeScreen
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.landing.LandingScreen
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.settings.SettingsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Landing.route
    ) {
        composable(Screen.Landing.route) {
            LandingScreen(
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Landing.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
