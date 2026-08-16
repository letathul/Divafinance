# feature/demo

**Purpose:** The one-time demo-data offer — seeding a realistic sample dataset, tearing it
back down, and recording the one-way status that decides whether the offer is ever shown
again. Also hosts the optional guided tour screen.

**Gradle:** `:feature:demo` · `diva.kmp.compose`
**Depends on:** `:core:model`, **`:core:data`** (seeds through repositories directly, no
use-case layer), `:core:ui`, `:core:common`
**Depended on by:** `:feature:onboarding` (final step offers the demo) and
`:feature:settings` (hosts the one-way remove action)
**Paired with:** `:dynamic:demo_dynamic`

## Key files

`src/commonMain/kotlin/com/divafinance/feature/demo/`

| File | What it does |
|------|--------------|
| `DemoStatus.kt` | `UNDECIDED → ACTIVE → RETIRED`, or `UNDECIDED → RETIRED` on decline. **`RETIRED` is terminal** and nothing moves the status backwards. `fromStorage(raw)` falls back to `UNDECIDED` for unknown values. |
| `DemoDataManager.kt` | Owns the whole lifecycle: `status()`, `isOfferAvailable()`, `decline()`, `seed(currency, onProgress)`, `clear()`. Takes all 8 repositories plus a `DemoModuleInstaller`. |
| `DemoDataSet.kt` | The sample rows themselves. Every row carries a shared **id prefix** — the only thing distinguishing demo data from real data. |
| `DemoModuleInstaller.kt` | `interface { install(onProgress), uninstall() }` plus a `NoOp` object. Declared here, not in the app module, so `DemoDataManager` can drive delivery without a Play Core dependency or a dependency cycle. |
| `DemoTourScreen.kt` | The guided tour, carried by the on-demand module |

## Conventions / gotchas

- **The status machine is deliberately one-way.** `seed()` no-ops unless the status is
  `UNDECIDED`, so a repeat call can't duplicate rows or resurrect a retired demo, and it
  returns `false` in that case. Don't add a path back to `UNDECIDED`.
- **Removal keys off the id prefix in `DemoDataSet`.** Any new demo row must use it, or
  `clear()` will leave it behind permanently.
- **Dynamic-module install is best-effort and must not block seeding.** The split carries
  only the tour screen; the sample data lives in the base app. A sideloaded debug build or
  an offline device gets no split and the demo still has to work.
- The real `DemoModuleInstaller` is supplied by the app module — `PlayDemoModuleInstaller`
  in `composeApp/src/commonMain/.../di/DynamicFeatureModule.kt`. Everything here codes
  against the interface.

## Tests

`src/commonTest/` — `DemoDataManagerTest.kt` plus a module-local `FakeRepositories.kt`
(this module predates using `:core:testing` and does not depend on it).

```bash
./gradlew :feature:demo:jvmTest
```
