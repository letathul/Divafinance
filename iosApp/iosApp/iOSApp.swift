import SwiftUI

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        #if compiler(>=6.1)
        .defaultGlassEffect()
        #endif
    }
}
