# DivaFinance

Kotlin Multiplatform personal-finance app. Tracks credit cards, reward rules, and
transactions, and recommends the best card to pay with for a given spending category.
Everything is local-first: an on-device SQLite database plus an optional embedded Ktor
server that exposes a web UI to other devices on the same LAN.

**27 Gradle modules:** 8 `core`, 13 `feature`, 4 `dynamic`, plus `composeApp` and `server`.

## Tech stack

Versions live in `gradle/libs.versions.toml` — that file is the single source of truth,
read it rather than trusting this table if they disagree.

| | |
|---|---|
| Kotlin | 2.2.10 |
| AGP | 8.13.2 |
| Compose Multiplatform | 1.9.3 |
| SQLDelight | 2.0.2 |
| Ktor (CIO) | 3.0.3 |
| Koin | 4.1.1 |
| navigation-compose | 2.9.0 |
| kotlinx: coroutines / serialization / datetime | 1.9.0 / 1.9.0 / 0.7.1 |
| Android | compileSdk 35, minSdk 26, targetSdk 35 |
| JVM | toolchain 17, jvmTarget 17 |

Targets: `androidTarget`, `jvm`, `iosX64`, `iosArm64`, `iosSimulatorArm64`.
**iOS is live**, not commented out.

## Architecture

Clean Architecture + MVVM. Dependencies point strictly downward:

```
core:model                       (pure data, zero deps)
   ↑
core:common   core:database      (expect/actual utils · SQLDelight)
   ↑
core:data                        (repository interfaces + impls, mappers)
   ↑
core:domain                      (use cases + RewardRecommendationEngine)
   ↑
feature:*                        (screens + ViewModels; also depend on core:ui)
   ↑
composeApp                       (nav graph + Koin wiring; depends on everything)
```

Rules that matter when adding dependencies:

- A `feature/*` module depends on `:core:domain`, `:core:ui`, `:core:common`, and
  optionally `:core:model`. It must **not** depend on `:core:database` — SQLDelight
  generated types stop at `:core:data`.
- Three features break the "no feature→feature edge" rule deliberately:
  `:feature:onboarding` and `:feature:settings` both depend on `:feature:demo`.
  `:feature:settings` is also the only feature depending on `:server`.
- Three features declare `:core:data` directly (`quickadd`, `settings`, `demo`) because
  they write through repositories that have no use-case wrapper yet.
- ViewModels are constructed in `composeApp/src/commonMain/.../di/ViewModelModule.kt`,
  never by the feature module itself.

## Source sets

Standard KMP layout plus one custom group. `diva.kmp.library` defines a **`jvmShared`**
source set (`androidMain` + `jvmMain`) via `applyDefaultHierarchyTemplate`. Actuals that
are plain Java — crypto, dispatchers — live in `src/jvmSharedMain/` once instead of being
duplicated. Only code touching `android.content.Context` belongs in `androidMain`.

The `jvm()` target is **not shipped**. It exists so Compose UI tests have a host that can
actually run: `runComposeUiTest` on `androidUnitTest` fails with `Build.FINGERPRINT is
null`, while the desktop Skiko backend needs no device. This is why UI tests live in
`src/jvmTest/`, not `src/androidUnitTest/`.

See `gradle/build-logic/CLAUDE.md` for the full reasoning.

## Build & test

```bash
./gradlew build                          # everything
./gradlew :composeApp:assembleDebug      # Android APK
./gradlew :core:domain:jvmTest           # one module's unit tests
./gradlew :feature:cards:jvmTest         # one module's Compose UI tests
./gradlew jvmTest                        # all JVM tests across all modules
```

Configuration cache and parallel builds are on (`gradle.properties`). Some modules put
tests in `commonTest` and others in `jvmTest` — each module's `CLAUDE.md` says which.

Known constraint: `dl.google.com` returns 403 through some CI proxies, so Google Maven
may be unreachable remotely and the build has to be verified locally.

## Module index

**Core**
- [`core/model`](core/model/CLAUDE.md) — entities + enums, zero dependencies
- [`core/common`](core/common/CLAUDE.md) — expect/actual utilities (crypto, dispatchers, filesystem, UUIDs)
- [`core/database`](core/database/CLAUDE.md) — SQLDelight schema + drivers
- [`core/data`](core/data/CLAUDE.md) — repositories + mappers
- [`core/domain`](core/domain/CLAUDE.md) — use cases + reward recommendation engine
- [`core/network`](core/network/CLAUDE.md) — DTOs + API route constants
- [`core/ui`](core/ui/CLAUDE.md) — design system (theme + shared components)
- [`core/testing`](core/testing/CLAUDE.md) — fake repositories + test data

**Feature**
- [`feature/onboarding`](feature/onboarding/CLAUDE.md) — 7-step setup wizard
- [`feature/dashboard`](feature/dashboard/CLAUDE.md) — home screen
- [`feature/cards`](feature/cards/CLAUDE.md) — card CRUD, reward rules, best-card
- [`feature/transactions`](feature/transactions/CLAUDE.md) — transaction list/detail/add
- [`feature/quickadd`](feature/quickadd/CLAUDE.md) — bottom-sheet entry with calculator keypad
- [`feature/graphs`](feature/graphs/CLAUDE.md) — charts + spending thresholds
- [`feature/feed`](feature/feed/CLAUDE.md) — activity feed + daily bot insight
- [`feature/settings`](feature/settings/CLAUDE.md) — settings + embedded-server control
- [`feature/map`](feature/map/CLAUDE.md) — spending map (dynamic)
- [`feature/scanner`](feature/scanner/CLAUDE.md) — receipt OCR (dynamic)
- [`feature/backup`](feature/backup/CLAUDE.md) — export/import archive
- [`feature/automation`](feature/automation/CLAUDE.md) — platform shortcuts/intents
- [`feature/demo`](feature/demo/CLAUDE.md) — seeded demo dataset + guided tour (dynamic)

**App, server, delivery**
- [`composeApp`](composeApp/CLAUDE.md) — app shell, navigation graph, Koin wiring
- [`server`](server/CLAUDE.md) — embedded Ktor server + LAN web UI
- [`dynamic/map_dynamic`](dynamic/map_dynamic/CLAUDE.md) · [`scanner_dynamic`](dynamic/scanner_dynamic/CLAUDE.md) · [`server_dynamic`](dynamic/server_dynamic/CLAUDE.md) · [`demo_dynamic`](dynamic/demo_dynamic/CLAUDE.md) — Android on-demand delivery
- [`gradle/build-logic`](gradle/build-logic/CLAUDE.md) — convention plugins

`iosApp/` is an Xcode wrapper (`iOSApp.swift`, `ContentView.swift`) that hosts the
`ComposeApp` framework. It is not a Gradle module, so it has no `CLAUDE.md`.

`IMPLEMENTATION_STATUS.md` is a chronological build log of which increments are done —
useful for "is X finished?", not for orientation. Use the module docs above for that.
