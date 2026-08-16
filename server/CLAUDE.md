# server

**Purpose:** The embedded Ktor (CIO) web server. Lets a browser on the same Wi-Fi read and
edit the user's data over the LAN, with PIN + session-cookie auth. Multiplatform: the
Android process lifecycle lives in `:dynamic:server_dynamic`, not here.

**Gradle:** `:server` · `diva.kmp.library` + kotlinx-serialization
**Depends on:** `:core:model`, `:core:common`, `:core:data`, `:core:network`, Ktor server
(core `api`, plus cio / content-negotiation / sessions / status-pages / cors / auth)
**Depended on by:** `:composeApp`, `:feature:settings`, `:dynamic:server_dynamic`

## Key files

`src/commonMain/kotlin/com/divafinance/server/`

| File | What it does |
|------|--------------|
| `DivaServer.kt` | Builds and owns the `embeddedServer(CIO, …)`. Installs ContentNegotiation, Sessions (cookie), Authentication (`session`), StatusPages, CORS, and mounts the routes. Exposes `state: StateFlow<ServerState>`. Takes `PinAuthProvider`, three repositories, and the web resource map. |
| `ServerState.kt` | `Stopped` / `Starting` / `Running(address, port)` / `Failed(message)`, plus `isActive` and `browserUrl` extensions |
| `ServerConfig.kt` | `port = 8080`, `host = "0.0.0.0"` |
| `ServerLauncher.kt` | `interface ServerLauncher { start(config); stop() }` + `InProcessServerLauncher`. Android substitutes `ForegroundServerLauncher` from `:composeApp`. |
| `LocalAddressResolver.kt` | `fun interface { lanAddress(): String? }` + `NoLocalAddressResolver`. Android impl in `androidMain/AndroidLocalAddressResolver.kt`. |
| `WebContent.kt` / `WebResourceProvider.kt` | The browser UI — `index.html`, `style.css`, `app.js` held as Kotlin string constants and served from a `Map<String, Pair<String, ContentType>>` |
| `auth/PinAuthProvider.kt` | Validates a PIN against the `pin_hash` / `pin_salt` settings rows using `SecurityUtils.hashPin` |
| `auth/SessionManager.kt` | `DivaSession(token, authenticated)` |
| `routing/` | `ApiRoutes.kt` (mounts the rest), `AuthRoutes.kt`, `CardRoutes.kt`, `TransactionRoutes.kt`, `GraphRoutes.kt`, `StaticRoutes.kt` |

## Conventions / gotchas

- **Binding fails *after* `start()` returns** (port taken, no permission), so failures
  arrive as `ServerState.Failed`, never as a thrown exception. Callers must observe
  `state`, not wrap `start()` in a try/catch.
- `ServerState.Running.address` is nullable — the server is running either way, there
  just may be no resolvable LAN address (cellular only). `browserUrl` falls back to
  `localhost`. Never build the URL from `ServerConfig.port` alone.
- The server binds the wildcard host but the user is shown exactly one address; a wrong
  one is as useless as none, which is what `LocalAddressResolver` exists to get right.
- **Path strings come from `core:network`'s `ApiRoutes`.** Don't hardcode paths in the
  route files — the client side reads the same constants.
- The web UI is Kotlin string constants in `WebContent.kt`, not resource files, so it
  works identically on every target. Editing the browser UI means editing those strings.

## Tests

`src/androidUnitTest/DivaServerTest.kt` — **not `commonTest`**. It runs on the JVM, where
CIO can bind a real socket, which is the only place the full request/response path can be
exercised end to end. Uses the Ktor CIO *client*.

```bash
./gradlew :server:testDebugUnitTest
```
