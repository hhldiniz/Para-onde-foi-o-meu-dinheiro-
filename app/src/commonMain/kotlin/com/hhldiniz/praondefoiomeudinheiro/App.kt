package com.hhldiniz.praondefoiomeudinheiro

import androidx.compose.runtime.Composable
import com.hhldiniz.praondefoiomeudinheiro.presentation.navigation.AppNavigation
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.PraOndeFoiOMeuDinheiroTheme

/**
 * The whole UI, shared by both platforms. Android hosts it in `MainActivity`
 * via `setContent`, iOS in a `ComposeUIViewController`.
 */
@Composable
fun App() {
    PraOndeFoiOMeuDinheiroTheme {
        AppNavigation()
    }
}
