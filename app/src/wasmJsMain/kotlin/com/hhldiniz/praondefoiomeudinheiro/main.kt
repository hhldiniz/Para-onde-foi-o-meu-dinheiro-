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
    removeLoadingPlaceholder()
    AppInitializer.init()
    ComposeViewport(document.body!!) {
        App()
    }
}

/**
 * Drops the `#app-loading` element `index.html` paints while the wasm bundle
 * downloads. Reaching this function is the signal that it is no longer needed:
 * the bundle is loaded and Compose is about to take the viewport. It matters
 * most for an installed (PWA) launch, where the system splash screen hands over
 * to the page and a blank body would read as a broken app.
 */
private fun removeLoadingPlaceholder() {
    val placeholder = document.getElementById("app-loading") ?: return
    placeholder.parentNode?.removeChild(placeholder)
}
