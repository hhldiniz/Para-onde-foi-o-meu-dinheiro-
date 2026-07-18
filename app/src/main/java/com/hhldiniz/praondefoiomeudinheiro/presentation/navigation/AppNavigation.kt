package com.hhldiniz.praondefoiomeudinheiro.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hhldiniz.praondefoiomeudinheiro.PraondefoiomeudinheiroApp
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.addentry.AddEntryScreen
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home.HomeScreen
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.landing.LandingScreen
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.settings.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val app = context.applicationContext as PraondefoiomeudinheiroApp
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val count = withContext(Dispatchers.IO) {
            app.database.importedEntryDao().count()
        }
        startDestination = if (count > 0) Screen.Home.route else Screen.Landing.route
    }

    val destination = startDestination
    if (destination == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(
        navController = navController,
        startDestination = destination
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
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToAddEntry = { navController.navigate(Screen.AddEntry.route) },
                refreshKey = refreshKey,
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddEntry.route) {
            AddEntryScreen(
                onNavigateBack = {
                    refreshKey++
                    navController.popBackStack()
                }
            )
        }
    }
}
