import SwiftUI

@main
struct iOSApp: App {
    var body: some Scene {
        // No scene-level glass modifier exists; the Liquid Glass treatment is
        // applied inside ContentView via GlassEffectContainer on iOS 26+.
        WindowGroup {
            ContentView()
        }
    }
}
