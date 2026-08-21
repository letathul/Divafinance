# composeApp

**Purpose:** The application shell — navigation graph, bottom-nav scaffold, all Koin
wiring, dynamic-feature installation, and the Android/iOS entry points. This is the only
module that depends on every feature, and the only place features are composed together.

**Gradle:** `:composeApp` · applies the KMP/Android-application/Compose plugins
**directly** rather than a convention plugin. Namespace and applicationId
`com.divafinance.app`. Declares all four `:dynamic:*` modules in `android.dynamicFeatures`.
**Depends on:** all 7 non-testing `core` modules, `:server`, and all 13 `feature` modules.
**Targets:** `androidTarget` + three iOS targets (framework `ComposeApp`, static). **No
`jvm()` target** — unlike the library modules, so tests here live in `commonTest`.

## Key files

`src/commonMain/kotlin/com/divafinance/app/`

| File | What it does |
|------|--------------|
| `App.kt` | Root composable. Collects `AppearanceStore.appearance` and passes mode + accent into `DivaTheme`, so a change in Settings repaints the whole tree at once. |
| `MainScreen.kt` | The shell. Runs `InitializeDatabaseUseCase` in a `LaunchedEffect` to pick the start destination (feed vs onboarding) behind a loading state, then a `Box` holding the `NavHost`, the `DivaTabBar` and the undo snackbar. **No `Scaffold`** — the bar overlays the content rather than displacing it. |
| `DivaNavHost.kt` | `DivaRoutes` — every route constant in one object, plus `cardDetail`, `personDetail`, `transactionDetail` and `report(period, anchor)` — and the `NavHost` graph. Hoists `CardsViewModel`, `TransactionsViewModel`, and `GraphsViewModel` here so their form state survives navigation between related screens. |
| `dynamic/DynamicFeatureLoader.kt` | `DynamicModule` enum + `expect class` with `isInstalled`, `requestInstall`, `requestUninstall` |

**`di/`** — Koin. `appModules()` returns `platformModule()`, `dataModule`, `domainModule`,
`viewModelModule`, `serverModule`, `demoModule`; `initKoin()` adds `dynamicFeatureModule`.

| File | Registers |
|------|-----------|
| `KoinInit.kt` | `initKoin { }` — the single entry point |
| `AppModule.kt` | Aggregates the module list |
| `DataModule.kt` | All 9 repository interface → impl bindings |
| `DomainModule.kt` | Every use case |
| `ViewModelModule.kt` | Every feature ViewModel |
| `ServerModule.kt` | `DivaServer`, `PinAuthProvider`, `WebResourceProvider.loadResources()` |
| `DemoModule.kt` | `DemoDataManager` with all 8 repositories |
| `DynamicFeatureModule.kt` | `DynamicFeatureAvailability` and `PlayDemoModuleInstaller`, which bridges `:feature:demo`'s Play-Core-free `DemoModuleInstaller` interface onto the split loader |
| `PlatformModule.kt` (+ `.android`/`.ios`) | Driver factory, `FileSystem`, `DynamicFeatureLoader`, `ServerLauncher` |

**Platform entry points**

| File | What it does |
|------|--------------|
| `androidMain/.../DivaApplication.kt` | Calls `SplitCompat.install()` in `attachBaseContext` — without it, code from an on-demand split isn't on the classloader until the app restarts — then `initKoin`. |
| `androidMain/.../MainActivity.kt` | Sets `App()` as content |
| `androidMain/.../server/ForegroundServerLauncher.kt` | `ServerLauncher` impl. Starts the server in `server_dynamic`'s foreground service when the split is installed, **falling back to in-process** when it isn't. The service is referenced **by name, not class literal** — the base APK can't link against a module that may be absent. |
| `iosMain/.../MainViewController.kt` | iOS framework entry point |

## Conventions / gotchas

- **`DynamicModule` names must match the `:dynamic:*` directory names exactly**
  (`map_dynamic`, underscores not hyphens). Play derives the split name from the Gradle
  project name; a mismatch makes `isInstalled()` permanently false and installs fail.
- `requestUninstall` returning true means "Play accepted the request", not "already
  gone" — Play defers uninstalls to an idle moment.
- **A new use case or ViewModel isn't reachable until it's registered in `DomainModule` /
  `ViewModelModule`.** This is the most common cause of a Koin `NoDefinitionFoundException`
  at runtime.
- Add routes as constants in `DivaRoutes`, never as string literals at call sites —
  `DivaRoutesTest` guards every literal, so a rename is always a two-file change.
- **Only `FEED` and `YOU` show the tab bar** (`tabRoutes` in `MainScreen`).
  `currentBackStackEntryFlow` reports the route *pattern* (`"cards/{cardId}"`), never the
  resolved path, so membership is tested against the patterns in `DivaRoutes`.
- `QuickAddViewModel` is hoisted in `MainScreen`, not resolved inside the add destination,
  so the undo snackbar outlives the screen that produced it.
- `MainActivity`'s system bars are **fully transparent** and it sets
  `isNavigationBarContrastEnforced = false` on API 29+. The scrims used to mirror the
  theme's background hex by hand — a maintenance trap, since they resolve before Compose
  runs — and `SystemBarStyle.auto` keys off the *system* dark setting rather than the
  app's own `ThemeMode`. `DivaTabBar` draws the navigation bar's background now.
- The bottom inset is `divaContentPadding()` from `:core:ui`, not a literal. It used to
  be the number `120.dp` copied into the feed and the You tab, and `104.dp` in
  `MainScreen`'s snackbar.
- ViewModels hoisted in `DivaNavHost` must not also be resolved with `koinViewModel()`
  inside a screen, or the screen gets a second instance with empty form state.
- The iOS framework needs `linkerOpts("-lsqlite3")` for SQLDelight's SQLiter cinterop.

## Tests

`src/commonTest/DivaRoutesTest.kt`. There is no `jvm()` target here, so Compose UI tests
belong in the feature modules, not this one.

```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:testDebugUnitTest
```
