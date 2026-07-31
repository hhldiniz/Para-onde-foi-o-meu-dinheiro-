package com.hhldiniz.praondefoiomeudinheiro

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

/**
 * Browser entry point, mounted into `index.html`'s `<body>` by the Compose
 * Multiplatform web renderer. Runs the same [AppInitializer]/[App] startup
 * sequence as Android's `PraondefoiomeudinheiroApp` and iOS's
 * `MainViewController` — there is no platform Koin config to hand in here,
 * same as the iOS call site, since there is no Android `Context` equivalent.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    AppInitializer.init()
    ComposeViewport(document.body!!) {
        App()
    }
}
