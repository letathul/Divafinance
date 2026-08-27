# core/common

**Purpose:** Platform abstractions and small utilities shared by every layer — crypto,
coroutine dispatchers, filesystem access, UUIDs, number formatting, and the app's result
type. This is where `expect`/`actual` lives for anything that isn't UI or database.

**Gradle:** `:core:common` · `diva.kmp.library`
**Depends on:** nothing internal (kotlinx-coroutines + kotlinx-datetime only).
**Depended on by:** `core:database`, `core:data`, `core:domain`, `core:ui`, `:server`,
and every `feature/*` module.

## Key files

| File | What it does |
|------|--------------|
| `commonMain/.../SecurityUtils.kt` | `expect object` — `generateSalt()` / `hashPin(pin, salt)`. PBKDF2WithHmacSHA256 in the JVM actual. |
| `commonMain/.../DispatcherProvider.kt` | `expect` wrapper over IO/Default/Main so ViewModels and use cases can be given a test dispatcher |
| `commonMain/.../FileSystem.kt` | `expect class` — backup directory, read/write text, list/delete backup files. Backs `:feature:backup`. |
| `commonMain/.../UuidGenerator.kt` | ID generation for new entities |
| `commonMain/.../ExpressionEvaluator.kt` | Evaluates `+ - * / ( )` over decimal literals with standard precedence, for the quick-add keypad (`"12+8.50"`, `"120/3"`, `"12*(3+4)"`). `2(3+4)` reads as multiplication. Returns `null` for anything not acceptable as an amount, including division by zero and an unbalanced group; `preview()` closes an unclosed group rather than falling back past it. `append(expression, key)` is the keypad's **typing** rule and lives here rather than in the ViewModel because every rule in it is a statement about the same grammar: an operator replaces a dangling one, a second decimal point is refused, `.` alone becomes `0.`, a lone leading `0` is replaced, a digit after `)` multiplies, `)` needs an open group with something in it, and a typed literal stops at two decimals. Keys with no reading are refused rather than appended — a rejected keystroke is what stops the running total from silently blanking three keys later. |
| `commonMain/.../NumberFormat.kt` | `Double.toFixed(decimals)` / `Float.toFixed(decimals)` — multiplatform stand-in for `"%.2f".format()`, which is JVM-only and breaks the iOS targets. Also `Double.roundToCents()`, half-up and sign-preserving. |
| `commonMain/.../Result.kt` | `DivaResult<T>` sealed class: `Success` / `Error` / `Loading`, with `map` and `getOrNull` |
| `commonMain/.../BackupFileInfo.kt` | Name/path/size/mtime record returned by `FileSystem.listBackupFiles()` |
| `commonMain/.../LocationProvider.kt` | `Coordinates(lat, lng)`, the `LocationSource` interface (`isAvailable`, `hasPermission`, `currentCoordinates`, `describe`), and `expect class LocationProvider : LocationSource`. Backs location capture in `:feature:quickadd`. |

### Where the actuals live

| Source set | Actuals |
|------------|---------|
| `jvmSharedMain/` | `SecurityUtils`, `DispatcherProvider` — plain Java, identical on Android and desktop |
| `androidMain/` | `FileSystem`, `LocationProvider` — need `android.content.Context` |
| `jvmMain/` | `FileSystem`, `LocationProvider` (reports unavailable) — desktop |
| `iosMain/` | `SecurityUtils`, `FileSystem`, `DispatcherProvider`, `LocationProvider` |

## Conventions / gotchas

- **Put new plain-Java actuals in `jvmSharedMain/`, not `androidMain/`.** Only code that
  touches an Android API belongs in `androidMain`. See the root `CLAUDE.md` for why the
  `jvmShared` group exists.
- Adding an `expect` declaration means adding **four** actuals (jvmShared, jvm or android
  as applicable, ios). A missing iOS actual usually surfaces as a link error in the
  framework build, not a compile error in `commonMain`.
- `DivaResult` is used at repository/use-case boundaries. Don't introduce a second result
  type; Kotlin's stdlib `Result` is deliberately not used here because `Loading` is a
  needed state.
- **Round computed amounts with `roundToCents()` before storing, not just when
  rendering.** `0.1 + 0.2` is `0.30000000000000004`; display formatting hides the drift
  but the drifted value is what gets persisted and summed.
- `LocationSource` methods never throw and are all allowed to return null — a missing
  permission, disabled provider, absent geocoder, or no fix in time must all degrade to
  "no location" rather than interrupting an entry. Callers depend on the **interface**,
  not the `expect class`, which can't be subclassed in tests.
- Location deliberately uses platform APIs, not `play-services-location`: every module
  depends on `core:common`, so a GMS dependency here would reach all of them.

## Tests

`src/commonTest/` — currently only `ExpressionEvaluatorTest.kt`, the one piece of real
logic in the module. It covers the grammar and the typing rules together, because
`append()` only earns its place here if what it builds is always something `preview()` can
read — which is what `typingNeverBuildsAnUnreadableExpression` asserts.

```bash
./gradlew :core:common:jvmTest
```
