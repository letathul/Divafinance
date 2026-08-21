# feature/map

**Purpose:** Spending grouped by location, shown on a real map where one is available and
as a ranked list where it isn't. Also hosts the dialog for tagging a transaction with a
location.

**Gradle:** `:feature:map` · `diva.kmp.compose`
**Depends on:** `:core:domain`, `:core:ui`, `:core:common`. Android adds `maps-compose` +
`play-services-maps`.
**Paired with:** `:dynamic:map_dynamic` — the Maps SDK is delivered on demand, so the map
may genuinely be absent at runtime.

## Key files

`src/commonMain/kotlin/com/divafinance/feature/map/`

| File | What it does |
|------|--------------|
| `MapViewModel.kt` | Defines `LocationSpending` (name, `LocationTag`, transactions, total). `MapUiState.showMapView` is initialised from `isPlatformMapAvailable()`. `init` loads via `GetSpendingByLocationUseCase`; tagging goes through `TagTransactionLocationUseCase`. |
| `SpendingMapScreen.kt` | `DivaRoutes.MAP` — chooses map or fallback |
| `PlatformMapView.kt` | `expect fun PlatformMapView(...)` + `expect fun isPlatformMapAvailable()` |
| `MapFallbackScreen.kt` | The no-map path: a `LazyColumn` of locations by total spend |
| `LocationTagDialog.kt` | Attaches a `LocationTag` to a transaction |

### Actuals

| Source set | Behaviour |
|------------|-----------|
| `androidMain/PlatformMapView.android.kt` | Real Google Maps via `maps-compose` |
| `iosMain/PlatformMapView.ios.kt` | iOS implementation |
| `jvmMain/PlatformMapView.jvm.kt` | `isPlatformMapAvailable() = false`; the composable body is empty and never composed |

## Conventions / gotchas

- **`MapFallbackScreen` must not gain its own scaffold.** It renders inside
  `SpendingMapScreen`, which already provides one, and `SpendingMapScreenTest` drives it
  directly.
- **The fallback is not a stub — it's a first-class path.** A device without the on-demand
  map module takes it, and so does the desktop test host. That's deliberate: tests
  exercise a real user-facing code path rather than a placeholder.
- Always gate on `isPlatformMapAvailable()` before composing `PlatformMapView`. On the JVM
  target the actual has an empty body, so composing it directly renders nothing rather
  than failing loudly.
- Don't add Maps SDK imports to `commonMain` — they only exist on `androidMain`.

## Tests

`src/commonTest/MapViewModelTest.kt` and `src/jvmTest/SpendingMapScreenTest.kt`. The UI
test necessarily runs against the fallback path.

```bash
./gradlew :feature:map:jvmTest
```
