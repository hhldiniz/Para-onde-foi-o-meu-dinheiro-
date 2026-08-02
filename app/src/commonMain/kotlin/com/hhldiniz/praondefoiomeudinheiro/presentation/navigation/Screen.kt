package com.hhldiniz.praondefoiomeudinheiro.presentation.navigation

/** Sealed class defining the navigation destinations for the app. */
sealed class Screen(val route: String) {
    data object IntroPatrimony : Screen("intro_patrimony")
    data object IntroCategories : Screen("intro_categories")
    data object Landing : Screen("landing")
    data object Home : Screen("home")
    data object Settings : Screen("settings")
    data object AddEntry : Screen("add_entry")
    data object SmartImport : Screen("smart_import")
}
