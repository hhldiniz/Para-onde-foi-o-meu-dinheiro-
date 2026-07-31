import SwiftUI
import UIKit
import ComposeApp

/// Hosts the Kotlin Multiplatform Compose UI. `MainViewController()` is the
/// Kotlin entry point in `app/src/iosMain/.../MainViewController.kt`.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard)
    }
}
