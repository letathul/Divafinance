import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
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
