# feature/automation

**Purpose:** Lets the user enable platform shortcuts (Android app shortcuts, iOS Siri
Shortcuts) that deep-link into the app. **Largely a placeholder** — the UI and the state
machine are real, but nothing is registered with the OS yet.

**Gradle:** `:feature:automation` · `diva.kmp.compose`
**Depends on:** `:core:domain`, `:core:ui`, `:core:common`

## Key files

`src/commonMain/kotlin/com/divafinance/feature/automation/`

| File | What it does |
|------|--------------|
| `AutomationHandler.kt` | `ShortcutInfo(id, title, description, deepLinkUri)` + `expect class AutomationHandler()` with `registerShortcuts`, `unregisterShortcut`, `getRegisteredShortcuts`, `handleDeepLink` |
| `AutomationViewModel.kt` | `AutomationAction(id, title, description, platform, enabled)`, `AutomationPlatform` (`ANDROID` / `IOS` / `CROSS_PLATFORM`), seeded from `defaultActions()`. `toggleAction(id)` flips a flag then calls `syncShortcuts()`, which re-registers the enabled set. The handler is a **defaulted constructor parameter**, so it's substitutable in tests. |
| `AutomationScreen.kt` | `DivaRoutes.AUTOMATION` — the toggle list |

### Actuals

| Source set | Behaviour |
|------------|-----------|
| `jvmSharedMain/AutomationHandler.jvmShared.kt` | In-memory list only; nothing reaches the OS |
| `iosMain/AutomationHandler.ios.kt` | iOS implementation |

## Conventions / gotchas

- **The Android actual is the shared JVM one, and that is a known placeholder.** A real
  Android implementation would use `ShortcutManagerCompat`; it lives in `jvmSharedMain`
  purely because it's currently identical to the desktop one. **Move it to `androidMain`
  when implementing for real** — don't add Android APIs to `jvmSharedMain`.
- `registerShortcuts` replaces the whole set rather than appending. `syncShortcuts()`
  relies on that, so a partial update isn't possible by design.
- Enabled state lives only in `AutomationUiState` — it is not persisted. Toggles reset
  when the ViewModel is recreated. Persisting it means adding a `UserSettings` key.

## Tests

`src/commonTest/AutomationScreenTest.kt` — Compose UI test, runs on the JVM host.

```bash
./gradlew :feature:automation:jvmTest
```
