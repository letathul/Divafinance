# feature/settings

**Purpose:** App settings — currency, theme, and the controls for the embedded LAN server
(start/stop, port, the URL to type in a browser) plus one-way removal of demo data.

**Gradle:** `:feature:settings` · `diva.kmp.compose`
**Depends on:** `:core:model`, `:core:domain`, **`:core:data`** (reads/writes
`SettingsRepository` directly), `:core:ui`, `:core:common`, **`:server`**, **`:feature:demo`**

This is the only feature module that depends on `:server`.

## Key files

`src/commonMain/kotlin/com/divafinance/feature/settings/`

| File | What it does |
|------|--------------|
| `SettingsViewModel.kt` | `SettingsUiState` holds `serverState: ServerState` plus port/currency/theme/demo flags, and derives `isServerRunning`, `serverUrl`, and `serverError` from it. `init` loads settings and subscribes to the server's state. Takes `SettingsRepository`, `DivaServer`, `ServerLauncher`, `DemoDataManager`. |
| `SettingsScreen.kt` | `DivaRoutes.SETTINGS` — the whole settings surface |

## Conventions / gotchas

- **`serverUrl` is non-null only once the socket is actually bound.** Don't render a URL
  from the configured port; read it off `ServerState` via the `browserUrl` extension, or
  the user gets an address that isn't listening yet.
- Server errors arrive as `ServerState.Failed`, not as thrown exceptions — surface
  `serverError`, don't try/catch around `ServerLauncher`.
- Settings are stored as **string key/value pairs** in `UserSettings`, using the
  `UserSettings.KEY_*` constants (e.g. `KEY_SERVER_PORT`, `KEY_DEFAULT_CARD_ID`). Two
  older keys — `"currency"` and `"dark_theme"` — are still string literals here. Prefer a
  named constant for anything new.
- The demo section only renders while `DemoDataManager.status() == DemoStatus.ACTIVE`.
  **Removal is one-way** — there is no re-seed from this screen.

## Tests

`src/jvmTest/SettingsScreenTest.kt` — Compose UI test.

```bash
./gradlew :feature:settings:jvmTest
```
