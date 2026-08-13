package com.hhldiniz.praondefoiomeudinheiro.platform

import androidx.compose.runtime.Composable

/**
 * Same as Android: an app running from the App Store is already installed, and
 * iOS gives it no way to install itself.
 */
@Composable
actual fun rememberAppInstaller(): AppInstaller = UnavailableAppInstaller
