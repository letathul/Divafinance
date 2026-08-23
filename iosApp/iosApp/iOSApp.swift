import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    // A UIKit delegate rather than SwiftUI's `.onContinueUserActivity(_:)`, which matches a
    // single activity type. `IosShortcutRegistrar` sets each shortcut's type to its own id
    // — Siri needs them distinct to tell the suggestions apart — so a SwiftUI handler would
    // mean listing all seven ids here, a third place they'd have to be kept in step.
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        // No scene-level glass modifier exists; the Liquid Glass treatment is
        // applied inside ContentView via GlassEffectContainer on iOS 26+.
        WindowGroup {
            ContentView()
                // A `divafinance://` URL, which is what a shortcut tap delivers.
                .onOpenURL { url in
                    DeepLinks.shared.open(uri: url.absoluteString)
                }
        }
    }
}

final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        continue userActivity: NSUserActivity,
        restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void
    ) -> Bool {
        // The activity type *is* the shortcut id. The URI is rebuilt from it rather than
        // carried, because an NSUserActivity donated for Siri has no URL of its own.
        let id = userActivity.activityType
        guard !id.isEmpty else { return false }
        DeepLinks.shared.open(uri: "divafinance://automation/\(id)")
        return true
    }
}
