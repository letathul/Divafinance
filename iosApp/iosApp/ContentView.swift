import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // The bridge is handed in here rather than resolved from Kotlin because Apple's
        // FoundationModels framework is Swift-only and cannot be reached from Kotlin/Native.
        // It reports itself unsupported on a device without Apple Intelligence, so it is
        // always safe to pass.
        MainViewControllerKt.MainViewController(foundationModel: FoundationModelBridgeImpl())
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        #if compiler(>=6.1)
        if #available(iOS 26.0, *) {
            GlassEffectContainer {
                ComposeView()
                    .ignoresSafeArea()
            }
        } else {
            ComposeView()
                .ignoresSafeArea(.keyboard)
        }
        #else
        ComposeView()
            .ignoresSafeArea(.keyboard)
        #endif
    }
}
