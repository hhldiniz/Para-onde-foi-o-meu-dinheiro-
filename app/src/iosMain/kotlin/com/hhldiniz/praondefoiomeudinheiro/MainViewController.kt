package com.hhldiniz.praondefoiomeudinheiro

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * Entry point called from Swift (`iosApp/iosApp/ContentView.swift`). Runs the
 * same startup sequence as the Android `Application` and hosts the shared
 * [App] composable inside a `UIViewController`.
 */
fun MainViewController(): UIViewController {
    AppInitializer.init()
    return ComposeUIViewController { App() }
}
