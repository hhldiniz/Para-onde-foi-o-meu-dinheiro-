package com.hhldiniz.praondefoiomeudinheiro.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Whether the app can be installed to the device from inside the app itself,
 * and how to ask for it.
 *
 * This exists for the web build, where the app runs in a browser tab until the
 * user installs it as a PWA. Android and iOS ship through a store and are
 * already installed by the time this code runs, so their implementations
 * report [canInstall] as false forever and the UI simply never offers it —
 * see [UnavailableAppInstaller].
 */
@Stable
interface AppInstaller {

    /**
     * True only while the platform will actually accept an install request.
     * On the web that means the browser has offered one (Chromium's
     * `beforeinstallprompt`): it stays false in a browser that does not
     * support installing, in one where the app is already installed, and in
     * Safari, which installs only through its own share menu.
     *
     * Read this from composition — it changes on its own, some time after the
     * app starts, when the browser gets around to offering the install.
     */
    val canInstall: Boolean

    /**
     * Asks the platform to install the app, which is the last this code hears
     * of it: the browser owns the dialog and whether the user accepts. Doing
     * nothing is a valid outcome, and calling it while [canInstall] is false
     * does nothing at all.
     */
    fun install()
}

/** The [AppInstaller] for a platform whose apps are installed by a store, not by themselves. */
internal object UnavailableAppInstaller : AppInstaller {
    override val canInstall: Boolean get() = false
    override fun install() = Unit
}

/** The current platform's [AppInstaller]; see the interface for what each one does. */
@Composable
expect fun rememberAppInstaller(): AppInstaller
