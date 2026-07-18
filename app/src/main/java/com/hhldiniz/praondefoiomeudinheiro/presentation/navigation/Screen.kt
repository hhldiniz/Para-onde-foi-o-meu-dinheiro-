package com.hhldiniz.praondefoiomeudinheiro.presentation.navigation

sealed class Screen(val route: String) {
    data object Landing : Screen("landing")
    data object Home : Screen("home")
    data object Settings : Screen("settings")
}
