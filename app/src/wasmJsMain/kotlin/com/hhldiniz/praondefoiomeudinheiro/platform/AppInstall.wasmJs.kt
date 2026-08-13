package com.hhldiniz.praondefoiomeudinheiro.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import kotlinx.browser.window

/**
 * `window.praOndeCanInstall`, defined by `pwa.js`: true while the browser has
 * an install prompt on hold for this page. The `typeof` guard is the same one
 * the PDF and OCR bridges use — a missing script must not surface as
 * "undefined is not a function", it must simply mean "cannot install".
 */
private fun jsCanInstall(): Boolean =
    js("typeof window.praOndeCanInstall === 'function' && window.praOndeCanInstall()")

/** `window.praOndeInstall`, also from `pwa.js`: shows the prompt, returns whether there was one. */
private fun jsInstall(): Boolean =
    js("typeof window.praOndeInstall === 'function' && window.praOndeInstall()")

/**
 * The event `pwa.js` dispatches whenever the answer to [jsCanInstall] changes
 * — the browser offering a prompt, the prompt being spent, the app being
 * installed. Kotlin cannot listen for `beforeinstallprompt` itself: it fires
 * while the wasm bundle is still downloading, which is exactly why `pwa.js`
 * catches it and holds on to it.
 */
private const val AVAILABILITY_EVENT = "praonde:installavailability"

/**
 * Installing the web build as a PWA, driven by the prompt `pwa.js` is holding.
 *
 * A single long-lived object rather than per-composition state: the listener
 * is then registered exactly once for the page's lifetime (a Kotlin lambda has
 * no stable identity on the JS side, so a listener added per composition could
 * not be reliably removed again), and every screen that asks sees the same
 * answer.
 */
private object WebAppInstaller : AppInstaller {

    private val available = mutableStateOf(jsCanInstall())

    init {
        window.addEventListener(AVAILABILITY_EVENT, { available.value = jsCanInstall() })
    }

    override val canInstall: Boolean get() = available.value

    override fun install() {
        jsInstall()
        // No state update here: `pwa.js` dispatches the availability event as
        // it spends the prompt, so this goes through the same path as every
        // other change.
    }
}

@Composable
actual fun rememberAppInstaller(): AppInstaller = WebAppInstaller
