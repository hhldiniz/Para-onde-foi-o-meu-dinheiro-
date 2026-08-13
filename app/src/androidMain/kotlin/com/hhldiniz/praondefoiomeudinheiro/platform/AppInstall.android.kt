package com.hhldiniz.praondefoiomeudinheiro.platform

import androidx.compose.runtime.Composable

/**
 * There is nothing to install: an APK that is running has already been
 * installed by the Play Store or the package installer, and Android offers an
 * app no way to install itself.
 */
@Composable
actual fun rememberAppInstaller(): AppInstaller = UnavailableAppInstaller
