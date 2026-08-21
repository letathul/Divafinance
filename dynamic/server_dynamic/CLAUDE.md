# dynamic/server_dynamic

**Purpose:** Android on-demand delivery for the embedded LAN server. Unlike the other
dynamic modules this one carries real behaviour: a **foreground service** that keeps the
Ktor server alive while the app is backgrounded.

**Gradle:** `:dynamic:server_dynamic` · `diva.android.dynamic-feature` · namespace
`com.divafinance.dynamic.server`. Registered in `composeApp`'s `android.dynamicFeatures`.
**Depends on:** `:composeApp`, `:core:ui`, `:server`, `koin-android`,
`kotlinx-coroutines-android`
**Android only.**

## Key files

| File | What it does |
|------|--------------|
| `src/main/kotlin/.../ServerForegroundService.kt` | Injects `DivaServer` via Koin, posts the notification in `onCreate`, starts the server in `onStartCommand` with a port from `EXTRA_PORT`, and collects `ServerState` to keep the notification text honest about the bound address and bind failures. `START_STICKY`; `onDestroy` stops the server. |
| `src/main/kotlin/.../ServerDynamicActivity.kt` | Entry Activity for the module |
| `src/androidMain/AndroidManifest.xml` | `<dist:module>` on-demand, `fusing include=true`; declares `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` and the service with `foregroundServiceType="specialUse"` |
| `src/androidMain/res/values/strings.xml` | Notification channel + status strings |

## Conventions / gotchas

- **`startForeground` must be called within seconds of `startForegroundService()`** or the
  OS kills the process. The notification goes up in `onCreate`, *before* the server is
  asked to bind — don't move it after the bind.
- The service type is `specialUse` with the subtype `local_web_server` (declared as an
  `android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE` property). A local web server matches
  none of the predefined foreground-service types, and API 34+ requires the declaration.
  Play review asks about `specialUse` — keep the subtype accurate.
- The notification reflects `ServerState`, including "no network address available". Bind
  failures surface as state, not exceptions.
- **Sources live in `src/main/kotlin/`, manifest and resources in `src/androidMain/`.**
- Server logic itself belongs in `:server` (which is multiplatform); this module only
  supplies the Android process lifecycle. `:composeApp` also has a
  `ForegroundServerLauncher` that talks to this service.

## Tests

None here — the server is tested in `:server`.

```bash
./gradlew :dynamic:server_dynamic:assembleDebug
```
