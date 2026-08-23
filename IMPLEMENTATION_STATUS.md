# Diva Finance — Implementation Status

> Chronological build log of which increments are done. For module-level orientation —
> what a module is for and which files matter — see the root `CLAUDE.md` and the
> per-module `CLAUDE.md` files.

## Tech Stack (Actual)

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Kotlin | 2.2.10 |
| UI | Compose Multiplatform | 1.9.3 |
| Local Server | Ktor Server (CIO engine) | 3.0.3 |
| Architecture | Clean Architecture + MVVM | — |
| Persistence | SQLDelight | 2.0.2 |
| DI | Koin | 4.1.1 |
| Navigation | Jetpack Navigation Compose for Multiplatform | 2.9.0 |
| Serialization | kotlinx.serialization | 1.9.0 |
| Coroutines | kotlinx.coroutines | 1.9.0 |
| Android Gradle Plugin | AGP | 8.13.2 |
| Security | PBKDF2 PIN hashing (expect/actual) | — |

> **Note:** `gradle/libs.versions.toml` is the single source of truth for versions — read
> it rather than this table if they disagree. The plan originally specified newer versions
> (Kotlin 2.4.10, AGP 9.0, Navigation 3, etc.); the above is what compatibility
> constraints actually allowed. Android and iOS targets are both active.

---

## Module Structure

```
Divafinance/
├── gradle/build-logic/convention/          # Convention plugins
├── core/
│   ├── model/          # Domain entities + enums
│   ├── common/         # Expect/actual utilities (UuidGenerator, SecurityUtils, etc.)
│   ├── database/       # SQLDelight schema + drivers
│   ├── data/           # Repository interfaces + implementations
│   ├── domain/         # Use cases + reward/category engines
│   ├── ui/             # Design system (DivaTheme, reusable components)
│   ├── network/        # Shared DTOs + API route constants
│   └── testing/        # Repository fakes + test data (test-only)
├── feature/
│   ├── onboarding/     # 7-step wizard
│   ├── dashboard/      # Home dashboard
│   ├── cards/          # Card CRUD + reward rules + best-card
│   ├── transactions/   # Transaction list + detail + add
│   ├── quickadd/       # Quick-add sheet with calculator keypad
│   ├── graphs/         # Financial charts + thresholds
│   ├── feed/           # Activity feed + daily bot insight
│   ├── settings/       # App settings + embedded-server control
│   ├── map/            # Spending map (+ list fallback)
│   ├── scanner/        # Receipt OCR + CSV statement import
│   ├── backup/         # Backup/restore
│   ├── automation/     # Native automations (placeholder handler)
│   └── demo/           # Demo dataset + guided tour
├── dynamic/
│   ├── map_dynamic/    # Android on-demand: Google Maps
│   ├── scanner_dynamic/# Android on-demand: ML Kit
│   ├── server_dynamic/ # Android on-demand: Ktor server + foreground service
│   └── demo_dynamic/   # Android on-demand: demo tour
├── server/             # Embedded Ktor server
├── composeApp/         # Shared app shell + navigation + DI
└── iosApp/             # Xcode project wrapper
```

---

## Implementation Sequence — Progress Tracker

### Increment 1: Project Scaffolding — DONE ✅

- [x] Root `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`
- [x] `gradle/libs.versions.toml` with all dependency versions
- [x] Convention plugins: `diva.kmp.library`, `diva.kmp.compose`, `diva.android.application`, `diva.android.dynamic-feature`, `diva.android.library`
- [x] Minimal `composeApp` + `iosApp` structure
- [x] Stub `dynamic/` modules with manifests
- [x] `.gitignore`
- [x] Gradle wrapper

### Increment 2: Core Model + Database — DONE ✅

- [x] `core/model` — all entity data classes: Account, CreditCard, Transaction, CardRewardRule, Receipt, FeedPost, LocationTag, UserSettings, BackupArchive, Currency
- [x] `core/model/enums` — SpendingCategory (12 values), CardNetwork, RewardType, TransactionType, CapPeriod, AccountType
- [x] `core/common` — DispatcherProvider, PlatformContext, UuidGenerator (expect/actual)
- [x] `core/database` — SQLDelight `.sq` schema files (8 tables), column adapters, DatabaseDriverFactory expect/actual
- [x] `core/network` — shared DTOs (CardDto, TransactionDto, DashboardDto, AuthDto) and API route constants

### Increment 3: Data Layer — DONE ✅

- [x] Repository interfaces: AccountRepository, CardRepository, TransactionRepository, RewardRepository, SettingsRepository, ReceiptRepository, FeedRepository, BackupRepository
- [x] Repository implementations backed by SQLDelight queries
- [x] Mappers between SQLDelight generated types and domain models

### Increment 4: Domain Layer — DONE ✅

- [x] Cards use cases: GetAllCardsUseCase, AddCardUseCase, UpdateCardUseCase, GetBestCardForCategoryUseCase, CalculateRewardValueUseCase
- [x] Transaction use cases: AddTransactionUseCase, GetTransactionsUseCase, GetSpendingByCategoryUseCase, GetHighImpactTransactionsUseCase
- [x] Onboarding use cases: CompleteOnboardingUseCase, InitializeDatabaseUseCase, ValidatePinUseCase
- [x] Other use cases: ExportBackupUseCase, ImportBackupUseCase, GetThresholdGraphDataUseCase, ConfigureThresholdUseCase, TagTransactionLocationUseCase, GetSpendingByLocationUseCase, GetFeedPostsUseCase, PostTransactionToFeedUseCase, GenerateDailyInsightUseCase, ParseReceiptUseCase, ImportStatementUseCase
- [x] `RewardRecommendationEngine` — ranks cards by estimated reward value per category
- [x] Unit tests: 24 test files covering all use cases and engine
- [x] 9 fake repository implementations for testing

### Increment 5: Design System + DI + Dynamic Feature Loader — DONE ✅

- [x] `core/ui/theme` — DivaTheme with light/dark color schemes, Typography, Shapes
- [x] `core/ui/component` — DivaButton, DivaOutlinedButton, DivaCard, DivaTextField, CreditCardVisual, AmountDisplay, CategoryChip, LoadingIndicator
- [x] `core/ui/util` — CurrencyFormatter, DateFormatter
- [x] Koin modules: AppModule, DataModule, DomainModule, ViewModelModule, ServerModule, DynamicFeatureModule
- [x] `DynamicFeatureLoader` expect/actual (Android: SplitInstallManager wrapper, iOS: stub)
- [x] DivaApplication (Android) + MainViewController (iOS) DI wiring
- [x] 11 Compose UI test files for feature screens

### Increment 6: Onboarding (Feature B) — DONE ✅

- [x] `SecurityUtils` expect/actual — PBKDF2WithHmacSHA256 (120k iterations, 256-bit key, 32-byte salt)
- [x] `SetPinUseCase` — generates salt + hash, stores in UserSettings
- [x] `ValidatePinUseCase` updated — salt-based PIN comparison
- [x] `OnboardingViewModel` — 6-step state machine with form state management
- [x] 6 step composables: WelcomeStep, CurrencyStep (7 currencies), LocationStep, AccountSetupStep (4 account types), CardSetupStep (5 networks), SecurityStep (PIN with confirmation)
- [x] `OnboardingScreen` — AnimatedContent with slide transitions, progress indicator
- [x] `DivaNavHost` — full route constants and navigation graph
- [x] `MainScreen` — Scaffold with bottom nav bar (Home, Transactions, Feed, Settings) + onboarding guard
- [x] `DivaTextField` extended with `visualTransformation` and `keyboardOptions` parameters
- [x] DI: SetPinUseCase in DomainModule, OnboardingViewModel in ViewModelModule

### Increment 7: Credit Card Management (Feature A) — DONE ✅

- [x] `CardsViewModel` — card list (Flow), form state for add/edit, reward rule form editor, best-card recommendation state
- [x] `CardsListScreen` — HorizontalPager card carousel, card list items, empty state, FAB
- [x] `AddEditCardScreen` — card name, last-4 digits, network chips, credit limit, annual fee, statement/due dates, inline reward rule editor (category, multiplier, reward type, cap amount/period)
- [x] `CardDetailScreen` — CreditCardVisual, info section (limit/balance/credit/fees/dates), reward rules list
- [x] `BestCardRecommendationScreen` — category picker, amount input, ranked recommendations with estimated reward values
- [x] Navigation: card detail, add, edit, best-card routes with shared ViewModel
- [x] DI: CardsViewModel registered in ViewModelModule

### Increment 8: Transactions + Dashboard — DONE ✅

- [x] `TransactionsViewModel` — Flow-based filtered list with category/type filters, 4 sort orders, text search, add transaction form with card selection
- [x] `TransactionListScreen` — search bar, category filter chips, type toggle (All/Expenses/Income), sort dropdown, color-coded transaction items, empty state, FAB
- [x] `AddTransactionScreen` — amount, expense/income toggle, category chips, merchant name, note, optional card selector with available credit display
- [x] `DashboardViewModel` — reactive income/spending totals, recent transactions, card count
- [x] `DashboardScreen` — overview card (income/spending/net), summary tiles, quick actions (My Cards, Best Card), recent transactions list
- [x] Navigation: transaction add route, dashboard callbacks for cards/transactions/best-card
- [x] DI: TransactionsViewModel and DashboardViewModel registered

### Increment 9: Financial Graphs (Feature C) — DONE ✅

- [x] Custom Canvas charts: SpendingPieChart (drawArc donut with legend), ThresholdBarChart (bars with threshold lines), TrendLineChart (drawPath with gradient fill)
- [x] GraphsDashboardScreen with tab navigation (Spending/Thresholds/Trends), time period selector (1M/3M/6M/1Y)
- [x] ThresholdConfigScreen for per-category % thresholds with add/edit/delete, over-threshold alerts
- [x] GraphsViewModel with spending slices, threshold data, trend points, configurable time periods

### Increment 10: Backup & Restore (Feature E) — DONE ✅

- [x] FileSystem expect/actual for platform file I/O (commonMain expect, androidMain actual using Context/java.io.File)
- [x] BackupFileInfo data class for backup file metadata
- [x] Export: DB → BackupArchive → JSON serialization → `.diva` file via FileSystem
- [x] Import: `.diva` file → JSON deserialization → version validation → DB import (replace or merge)
- [x] BackupViewModel with export, import (confirm dialog with replace/merge), delete, file listing
- [x] BackupRestoreScreen with export section, backup file list, import/delete actions, snackbar feedback
- [x] BackupProgressDialog with indeterminate progress indicator
- [x] Import confirmation dialog with Replace/Merge/Cancel options
- [x] Delete confirmation dialog
- [x] SettingsScreen updated with "Backup & Restore" navigation button
- [x] DI: FileSystem in PlatformModule, BackupViewModel in ViewModelModule
- [x] Navigation: backup route wired with ViewModel, settings→backup navigation

### Increment 11: Social Feed & Daily Bot (Feature G) — DONE ✅

- [x] FeedViewModel with reactive feed post collection and insight generation trigger
- [x] FeedScreen with LazyColumn timeline, empty state, TopAppBar with "Insights" action
- [x] TransactionFeedItem component — card-style post with icon, title, body, timestamp
- [x] BotInsightBubble component — distinct tertiaryContainer background, star icon
- [x] formatTimestamp utility — "Today at HH:mm" or "M/d at HH:mm"
- [x] Auto-post to feed on transaction creation via PostTransactionToFeedUseCase in TransactionsViewModel
- [x] Weekly spending insight generation (GenerateDailyInsightUseCase) triggered on feed open
- [x] DI: FeedViewModel registered in ViewModelModule, TransactionsViewModel updated with 4th dependency
- [x] Navigation: feed route wired with FeedViewModel

### Increment 12: Spending Map (Feature D) + Dynamic Delivery — DONE ✅

- [x] `PlatformMapView` expect/actual — real `GoogleMap` + markers on Android,
      `UIKitView` hosting a real `MKMapView` with `MKPointAnnotation`s on iOS. The jvm
      actual reports `isPlatformMapAvailable() = false`, so tests exercise the fallback.
- [x] `MapFallbackScreen` — grouped list, rendered inside `SpendingMapScreen`
- [x] `MapViewModel`, registered in `ViewModelModule`; `DivaRoutes.MAP` wired
- [x] Google Maps API key read from `MAPS_API_KEY` in `local.properties` (or the
      environment) into `manifestPlaceholders`, substituted into the manifest's
      `com.google.android.geo.API_KEY`. **Without a key Maps draws a grey tile grid** —
      the map is unusable and the list fallback is the intended path.

**Known gap:** `dynamic:map_dynamic` exists but achieves nothing. `maps.compose` and
`play-services-maps` are declared in `:feature:map`'s `androidMain`, so the SDK ships in
the base APK regardless; `DynamicFeatureAvailability.installMap()` has no callers and
`MapDynamicActivity` is unreachable. See "Dynamic delivery" below.

### Increment 13: Embedded Ktor Server (Feature F) + Dynamic Delivery — DONE ✅

- [x] DivaServer with CIO engine — binds off-thread and reports Stopped/Starting/Running/Failed
      through a `StateFlow`, so a refused port surfaces in the UI instead of being swallowed
- [x] PIN auth via Ktor's `Authentication` + `session<DivaSession>` provider guarding every
      `/api` route (replaces a hand-rolled routing interceptor)
- [x] API routes: `/api/cards`, `/api/cards/{id}`, `/api/transactions`, `/api/transactions/{id}`,
      `/api/graphs/spending`; `/auth/session|login|logout`
- [x] Embedded responsive HTML/JS/CSS web UI served from Kotlin constants (portable to iOS)
- [x] `LocalAddressResolver` — Settings shows the Wi-Fi address to open. Android asks
      ConnectivityManager which network is `TRANSPORT_WIFI` rather than scanning the
      interface list, where a carrier's `172.x` cellular address is enumerated first and
      reports as site-local despite being reachable from nothing (iOS returns null →
      falls back to localhost)
- [x] Wire `dynamic:server_dynamic` module — split names corrected to match the Gradle project
      names, `SplitCompat.install` added so split code is loadable without an app restart
- [x] Server foreground service (Android) with `specialUse` FGS type + live notification;
      `ForegroundServerLauncher` falls back to an in-process server when the split is absent
- [x] `DivaServerTest` (JVM): serves the UI, rejects `/api` without a session, accepts the PIN,
      unlocks on cookie, revokes on logout, and reports a bind failure

### Increment 14: Receipt Scanning & Statement Import (Feature H) + Dynamic Delivery — DONE ✅

- [x] `OcrEngine` expect/actual — ML Kit `TextRecognition` on Android, `VNRecognizeTextRequest`
      on iOS (with the bottom-left→top-left y-flip); jvm returns an empty result, which
      `ParseReceiptUseCase` records as `FAILED` rather than throwing
- [x] `ImageCapture` expect/actual on all three targets — ML Kit's document scanner,
      `VNDocumentCameraViewController` + `PHPickerViewController` on iOS, multi-page
- [x] `ScannerViewModel` + `ReceiptReviewScreen`/`ReceiptReviewViewModel`
- [x] CSV parsing in `ImportStatementUseCase` — quote-aware, header row dropped
- [x] **File picker for CSV import** — `TextFilePicker` expect/actual: `OpenDocument` on
      Android, `UIDocumentPickerViewController` on iOS (inside a security-scoped access
      window), unavailable on jvm. The paste box remains as a second way in.
- [x] **Account and card pickers** replace the free-text "Account ID"/"Card ID" fields that
      asked the user to type raw UUIDs. A single account is preselected.
- [x] Imported rows are categorised through `CategoryPredictionEngine` on the merchant
      string, instead of every row landing in `SpendingCategory.OTHER`. Falls back to
      `OTHER` for income and for a first import against an empty history.

**Known gap:** the scanner split has the same problem as the map's — ML Kit is declared in
`:feature:scanner`'s `androidMain`, `installScanner()` has no callers, and
`ScannerDynamicActivity` is unreachable.

### Increment 15: Native Automations (Feature I) — DONE ✅

- [x] `ShortcutRegistrar` interface + three implementations. Replaced
      `expect class AutomationHandler()`, whose no-argument constructor could not carry the
      Android `Context` a real implementation needs.
- [x] **Android: real dynamic app shortcuts** via `ShortcutManagerCompat.setDynamicShortcuts`,
      each carrying an `ACTION_VIEW` intent for its `divafinance://` URI, trimmed to
      `getMaxShortcutCountPerActivity`. This was previously an in-memory list that reached
      no OS API at all.
- [x] iOS: SiriKit `INShortcut` + `INVoiceShortcutCenter` suggestions
- [x] **The `divafinance://` scheme is registered on both platforms** — a second
      `<intent-filter>` on `MainActivity` (now `singleTask`) plus `CFBundleURLTypes` and
      `NSUserActivityTypes` in `Info.plist`. It previously existed on neither, so even the
      working Siri suggestion opened nothing.
- [x] **Deep links are handled.** `MainActivity.onCreate`/`onNewIntent` and the iOS
      `AppDelegate` publish into `DeepLinks`; `MainScreen` collects it and navigates via
      `DivaRoutes.forAutomationDeepLink`. Onboarding is not jumped past.
- [x] **Toggles persist** in `UserSettings.KEY_ENABLED_AUTOMATIONS` and are re-registered
      on construction — the OS keeps shortcuts across launches, so the stored set is the
      only thing that says what they should be
- [x] `AutomationScreen` + `AutomationViewModel`, reachable from the You tab and Settings

### Increment 16: Integration Testing & Polish — PARTIAL ⬛

- [x] Unit and Compose UI tests across every module with logic; production code carries no
      `TODO`/`FIXME` markers
- [ ] **No instrumented or E2E coverage at all** — there is no `androidTest`,
      `androidInstrumentedTest` or `iosTest` source set anywhere, so ML Kit, Vision,
      `MKMapView`, `GoogleMap`, `SplitInstall`, `ShortcutManagerCompat`, deep links and
      `FileProvider` are exercised only through their no-op jvm actuals
- [ ] **No tests in `core/data`, `core/database`, `core/model` or `core/network`** — the
      whole persistence layer is covered only indirectly, through `core:domain`'s fakes
- [ ] Performance profiling, memory leak testing
- [ ] Dynamic feature download/install testing
- [ ] Accessibility pass, empty/error state handling

---

## Summary

| Increment | Description | Status |
|-----------|-------------|--------|
| 1 | Project Scaffolding | ✅ Done |
| 2 | Core Model + Database | ✅ Done |
| 3 | Data Layer | ✅ Done |
| 4 | Domain Layer | ✅ Done |
| 5 | Design System + DI | ✅ Done |
| 6 | Onboarding | ✅ Done |
| 7 | Credit Card Management | ✅ Done |
| 8 | Transactions + Dashboard | ✅ Done |
| 9 | Financial Graphs | ✅ Done |
| 10 | Backup & Restore | ✅ Done |
| 11 | Social Feed & Daily Bot | ✅ Done |
| 12 | Spending Map + Dynamic Delivery | ✅ Done |
| 13 | Embedded Ktor Server + Dynamic Delivery | ✅ Done |
| 14 | Receipt Scanning + Dynamic Delivery | ✅ Done |
| 15 | Native Automations | ✅ Done |
| 16 | Integration Testing & Polish | ⬛ Partial |

**Progress: 15 / 16 increments complete (94%)**

## Known gaps

Recorded here because they are not visible from the increment list — each one is a shipped
feature with something real still missing.

### Dynamic delivery does nothing

All four `:dynamic:*` modules are declared and build, but none of them reduces the base
APK. Their heavy dependencies — `maps.compose` + `play-services-maps` in
`feature/map/build.gradle.kts`, ML Kit in `feature/scanner/build.gradle.kts` — are declared
in the **base** feature modules, so the code ships in the base APK regardless. Meanwhile
`DynamicFeatureAvailability.installMap()` / `installScanner()` have **zero callers**:
`DivaNavHost` navigates straight to the screens with no install gate, and
`MapDynamicActivity` / `ScannerDynamicActivity` are unreachable. Only the *demo* split is
genuinely wired, through `PlayDemoModuleInstaller`.

Fixing this means moving dependencies between modules and building install-gate UI, and
until it happens each `dynamic/*/CLAUDE.md`'s stated purpose is aspirational.

### Receipt attachments are captured but never displayed

Receipts are scanned, parsed and stored, but nothing outside the review form shows the
image: `TransactionDetailScreen` renders the receipt's raw UUID as a text row, and
`core:ui` has no image pipeline at all. `IMPLEMENTATION_PLAN.md` is the design for this —
**planned, not started**; none of its seven tasks are implemented.

### Premium is a boundary with nothing behind it

`PremiumGate` reads `UserSettings.KEY_IS_PREMIUM`, and nothing ever sets it. There is no
billing integration.

---

## Key Files

| File | Purpose |
|------|---------|
| `settings.gradle.kts` | All module declarations |
| `gradle/libs.versions.toml` | Version catalog (single source of truth) |
| `gradle/build-logic/convention/` | Convention plugins for KMP/Compose/Android |
| `core/database/.../DivaFinance.sq` | Database schema (8 tables) |
| `core/domain/.../engine/RewardRecommendationEngine.kt` | Card ranking algorithm |
| `composeApp/.../DivaNavHost.kt` | Navigation graph + route constants |
| `composeApp/.../MainScreen.kt` | Scaffold + bottom nav + onboarding guard |
| `composeApp/.../di/` | Koin modules (App, Data, Domain, ViewModel, Server) |

---

## Build Notes

- **Google Maven blocked in remote CI** — `dl.google.com` returns 403 through the proxy. Build must be verified locally.
- **iOS targets are live**, not commented out. `androidTarget`, `jvm`, `iosX64`, `iosArm64`
  and `iosSimulatorArm64` all build; `:composeApp:linkDebugFrameworkIosSimulatorArm64` is
  the task that catches a missing iOS actual, which never surfaces as a `commonMain`
  compile error.
- **`MAPS_API_KEY` goes in `local.properties`** (gitignored) or the environment. Absent is
  a valid build — Maps then renders a grey tile grid.
- **gradle.properties** — contains macOS Java home paths for local builds (`/Users/athulbabu/Library/Java/...`).
- **Branch**: `claude/financial-app-phase-1-nm9j5s` on `letathul/Divafinance`
