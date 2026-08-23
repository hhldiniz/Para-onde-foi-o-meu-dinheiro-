package com.hhldiniz.praondefoiomeudinheiro.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hhldiniz.praondefoiomeudinheiro.data.local.OnboardingHolder
import com.hhldiniz.praondefoiomeudinheiro.data.repository.ImportRepository
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.addentry.AddEntryScreen
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home.HomeScreen
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.intro.IntroCategoriesScreen
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.intro.IntroPatrimonyScreen
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.landing.LandingScreen
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.settings.SettingsScreen
import com.hhldiniz.praondefoiomeudinheiro.presentation.screen.smartimport.SmartImportScreen
import kotlinx.coroutines.withContext
import com.hhldiniz.praondefoiomeudinheiro.platform.ioDispatcher
import org.koin.compose.koinInject

@Composable
fun AppNavigation() {
    val importRepository = koinInject<ImportRepository>()
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val count = withContext(ioDispatcher) {
            importRepository.count()
        }
        // count() alone would send a user who finished onboarding (patrimony
        // + categories) but hasn't added an entry yet straight back to
        // onboarding on every reload/restart, even though that onboarding
        // data is correctly persisted.
        val onboardingDone = count > 0 || OnboardingHolder.completed.value
        startDestination = if (onboardingDone) Screen.Home.route else Screen.IntroPatrimony.route
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
        composable(Screen.IntroPatrimony.route) {
            IntroPatrimonyScreen(
                onContinue = {
                    navController.navigate(Screen.IntroCategories.route) {
                        popUpTo(Screen.IntroPatrimony.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.IntroCategories.route) {
            IntroCategoriesScreen(
                onContinue = {
                    navController.navigate(Screen.Landing.route) {
                        popUpTo(Screen.IntroCategories.route) { inclusive = true }
                    }
                }
            )
        }

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
                onNavigateToSmartImport = { navController.navigate(Screen.SmartImport.route) },
                refreshKey = refreshKey,
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onDataCleared = {
                    // Everything the user had is gone, so the whole back stack
                    // (Home included) would show stale data: start over from
                    // onboarding, exactly as a first launch does.
                    navController.navigate(Screen.IntroPatrimony.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Screen.SmartImport.route) {
            SmartImportScreen(
                onNavigateBack = {
                    // Same refresh handshake AddEntry uses: Home has no shared
                    // ViewModel with this screen, so it reloads on return.
                    refreshKey++
                    navController.popBackStack()
                }
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
