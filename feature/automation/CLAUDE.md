# feature/automation

**Purpose:** Lets the user enable platform shortcuts (Android app shortcuts, iOS Siri
Shortcuts) that deep-link into the app. Both platforms register with the OS for real.

**Gradle:** `:feature:automation` · `diva.kmp.compose`
**Depends on:** `:core:domain`, **`:core:data`** (persists the enabled set through
`SettingsRepository`), `:core:model`, `:core:ui`, `:core:common`. Android adds
`androidx-core-ktx` for `ShortcutManagerCompat`.

## Key files

`src/commonMain/kotlin/com/divafinance/feature/automation/`

| File | What it does |
|------|--------------|
| `AutomationHandler.kt` | `ShortcutInfo(id, title, description, deepLinkUri)`, `interface ShortcutRegistrar` (`registerShortcuts`, `unregisterShortcut`, `getRegisteredShortcuts`, `handleDeepLink`), and `InMemoryShortcutRegistrar` |
| `AutomationViewModel.kt` | `AutomationAction(id, title, description, platform, enabled)`, `AutomationPlatform` (`ANDROID` / `IOS` / `CROSS_PLATFORM`), seeded from `defaultActions()`. Reads the enabled set in `init` and re-registers; `toggleAction(id)` flips a flag, calls `syncShortcuts()` and persists. |
| `AutomationScreen.kt` | `DivaRoutes.AUTOMATION` — the toggle list |

### Implementations

| Source set | Class | Behaviour |
|------------|-------|-----------|
| `commonMain` | `InMemoryShortcutRegistrar` | Records without publishing — the desktop/test host, and what the ViewModel tests use |
| `androidMain` | `AndroidShortcutRegistrar` | `ShortcutManagerCompat.setDynamicShortcuts`, each with an `ACTION_VIEW` intent for its URI |
| `iosMain` | `IosShortcutRegistrar` | `INShortcut` via `INVoiceShortcutCenter.setShortcutSuggestions` |

Bound per platform in `composeApp`'s `platformModule()`, as `FileSystem` is.

## Conventions / gotchas

- **`ShortcutRegistrar` is an interface, not an `expect class`.** It was the latter, with a
  no-argument constructor — which cannot carry the Android `Context` that
  `ShortcutManagerCompat` needs, and which meant common code constructed the handler
  itself. Binding per platform in `platformModule()` solves both and leaves the ViewModel
  testable against `InMemoryShortcutRegistrar`.
- `registerShortcuts` replaces the whole set rather than appending. `syncShortcuts()`
  relies on that, so a partial update isn't possible by design.
- **The enabled set is re-registered in `init`, not only on toggle.** The OS keeps
  shortcuts across launches and upgrades, so `UserSettings.KEY_ENABLED_AUTOMATIONS` is the
  only thing that says what they should be — and a reinstall drops them while the setting
  survives.
- **The URI format is stated twice**: minted here as `divafinance://automation/{id}`, and
  matched in `DivaRoutes.forAutomationDeepLink`. This module sits below `composeApp` and
  cannot import `DivaRoutes`; `DivaRoutesTest` pins the other side.
- The scheme must stay registered in **both** manifests — `composeApp`'s
  `AndroidManifest.xml` (`ACTION_VIEW` + `BROWSABLE` on a `singleTask` `MainActivity`) and
  `iosApp/iosApp/Info.plist` (`CFBundleURLTypes` **and** `NSUserActivityTypes`, since iOS
  won't hand back an undeclared activity type). A shortcut whose URI opens nothing is
  worse than no shortcut.
- **Adding an action means adding its route mapping.** `DivaRoutes.forAutomationDeepLink`
  returns null for an id it doesn't know, so a new `defaultActions()` entry silently does
  nothing until it's mapped.

## Tests

`src/commonTest/AutomationScreenTest.kt`. Covers the enum shape, registration/deregistration
through `InMemoryShortcutRegistrar`, persistence across a new ViewModel, and deep-link
resolution. The ViewModel reads and writes through `viewModelScope`, so every test needs
`installTestMainDispatcher()`.

```bash
./gradlew :feature:automation:jvmTest
```
