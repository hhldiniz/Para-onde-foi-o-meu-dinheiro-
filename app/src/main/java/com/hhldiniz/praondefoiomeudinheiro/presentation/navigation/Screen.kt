package com.hhldiniz.praondefoiomeudinheiro.presentation.navigation

/** Sealed class defining the three navigation destinations for the app. */
sealed class Screen(val route: String) {
    data object Landing : Screen("landing")
    data object Home : Screen("home")
    data object Settings : Screen("settings")
}
