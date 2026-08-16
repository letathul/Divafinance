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
| `App.kt` | Root composable: `DivaTheme { Surface { MainScreen() } }` |
| `MainScreen.kt` | The scaffold. Runs `InitializeDatabaseUseCase` in a `LaunchedEffect` to pick the start destination (dashboard vs onboarding) behind a loading state, hosts the bottom nav (Home / Transactions / Feed / Settings, `bottomNavRoutes` also includes `CARDS`), the quick-add FAB + `QuickAddSheet`, and the undo snackbar. |
| `DivaNavHost.kt` | `DivaRoutes` — every route constant in one object, plus `cardDetail(cardId)` — and the `NavHost` graph. Hoists `CardsViewModel`, `TransactionsViewModel`, and `GraphsViewModel` here so their form state survives navigation between related screens. |
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
  `DivaRoutesTest` guards the constants.
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
