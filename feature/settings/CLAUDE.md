# feature/settings

**Purpose:** Hosts the **You tab** (`YouScreen`) — identity, the month's budget panel,
and the navigation rows out to cards, graphs, people, map, scanner, backup, automation
and settings. Also owns appearance (theme mode + accent), currency, and the controls for
the embedded LAN server (start/stop, port, the URL to type in a browser), plus one-way
removal of demo data.

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
- The Location section writes `KEY_LOCATION_CAPTURE_MODE` and nothing else — the OS
  permission is still requested by the add-expense sheet at the moment it needs a fix, so
  choosing "every expense" here cannot grant anything on its own. `locationCaptureMode` is
  **nullable and stays that way**: absent means the user has not been asked, which is the
  state the sheet's first-run dialog depends on, so neither segment is shown as chosen
  until they have. See `feature/quickadd`.

## Tests

`src/jvmTest/SettingsScreenTest.kt` — Compose UI test.

```bash
./gradlew :feature:settings:jvmTest
```

## The You tab and appearance (added in the 3-tab redesign)

| File | What it does |
|------|--------------|
| `YouScreen.kt` / `YouViewModel.kt` | `DivaRoutes.YOU`. Identity, stats, the budget panel, and the nav rows. Reads `GetActivityUseCase` + `GetThresholdGraphDataUseCase`. |
| `AppearanceStore.kt` | The theme preference as a **stream**. |

- **Budgets are `GraphThreshold` read in currency.** A threshold stores "this category
  should be N% of spending", which cannot render as "$107 left" without a total to take a
  percentage of. `UserSettings.KEY_MONTHLY_BUDGET` supplies it — a setting, not a new
  table. Absent simply means budgets render as shares instead of amounts.
- **`AppearanceStore` reads `SettingsRepository.getAll()`, not `get(key)`.** Only
  `getAll()` is reactive; a theme derived from the one-shot `get` would change only on the
  next launch. This is what makes the switch repaint immediately.
- `SettingsViewModel` does not update its own theme state after a write — it observes the
  same stream, so the write comes back to it.
- The currency row previously read and wrote a bare `"currency"` key that nothing else
  used, so changing it had no effect. It now uses `UserSettings.KEY_BASE_CURRENCY`, which
  is what onboarding writes and quick-add reads.
