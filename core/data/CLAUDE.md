# core/data

**Purpose:** The repository layer. Defines an interface per entity, implements each one
against SQLDelight, and maps generated row types to `core:model` classes. This is the only
module allowed to see SQLDelight-generated code.

**Gradle:** `:core:data` · `diva.kmp.library`
**Depends on:** `:core:model` (`api` — model types appear in repository signatures),
`:core:common`, `:core:database`
**Depended on by:** `:core:domain`, `:core:testing`, `:server`, and three feature modules
that write through repositories directly (`:feature:quickadd`, `:feature:settings`,
`:feature:demo`).

## Key files

All under `src/commonMain/kotlin/com/divafinance/core/data/`.

**`repository/`** — 10 interface + implementation pairs:

| Interface | Impl | Owns |
|-----------|------|------|
| `AccountRepository` | `AccountRepositoryImpl` | Accounts |
| `CardRepository` | `CardRepositoryImpl` | Credit cards; `getById` also loads the card's reward rules via `RewardRepository` |
| `RewardRepository` | `RewardRepositoryImpl` | Reward rules per card |
| `TransactionRepository` | `TransactionRepositoryImpl` | Transactions, category/date aggregates, `getWithLocation()`, and `getKnownMerchants()` (distinct merchants, most-used first — backs quick-add autocomplete) |
| `ReceiptRepository` | `ReceiptRepositoryImpl` | Scanned receipts |
| `FeedRepository` | `FeedRepositoryImpl` | Feed posts |
| `ThresholdRepository` | `ThresholdRepositoryImpl` | Per-category spending thresholds |
| `SettingsRepository` | `SettingsRepositoryImpl` | The single `UserSettings` row |
| `CustomCategoryRepository` | `CustomCategoryRepositoryImpl` | The user's own categories. A hard delete leaves transactions pointing at a missing row, which is deliberately survivable: they still carry the parent `SpendingCategory`, so they go on reporting as that built-in |
| `BackupRepository` | `BackupRepositoryImpl` | `exportAll()` / `importAll(archive, replaceExisting)` across every table |

Plus one non-database binding: **`ReceiptFileStore`**, a `fun interface` with a single
`delete(path)`. It exists because `core:common`'s `FileSystem` is an `expect class` whose
constructor differs per platform, so it can only be built inside a platform Koin module and
cannot be constructed in `commonTest` at all. `DeleteTransactionUseCase` depends on this
instead, which keeps `:core:domain` free of platform types and leaves receipt cleanup
testable against `FakeReceiptFileStore`. Bound in `DataModule` over `FileSystem::deleteFile`.

**`mapper/`** — `CardMapper`, `TransactionMapper`, `AccountMapper`. Objects with
`toDomain(...)` taking the generated row's columns as parameters, so the mapper doesn't
have to import SQLDelight types.

## Conventions / gotchas

- **Read APIs are `Flow`, writes are `suspend`.** List queries go through
  `.asFlow().mapToList(Dispatchers.Default).map { it.toDomain() }`; point lookups use
  `executeAsOneOrNull()`.
- Repository implementations take `DivaFinanceDb` in the constructor. They're wired as
  singletons in `composeApp/src/commonMain/.../di/DataModule.kt` — add both the interface
  binding and the impl there when you add a repository.
- Enum columns are stored as `.name` strings. Impls encode with `.name` and mappers decode
  with `valueOf(...)`, so an unknown value throws rather than falling back.
- Timestamps are `kotlin.time.Instant` in the model and ISO strings in the database.
- **`TransactionMapper` owns the tag encoding.** Tags are newline-delimited in one column,
  the same convention `Receipt.page_paths` uses, and null rather than `""` for the empty
  list. `tagsOf` / `encodeTags` are the only two places that know it.
- **Do not leak SQLDelight types out of this module.** Anything returned to `core:domain`
  must already be a `core:model` type.

## Tests

`src/commonTest/` is configured but currently empty — repository behaviour is exercised
through `core:domain` tests against the fakes in `:core:testing`. When you add a
repository, add its fake there too.

```bash
./gradlew :core:data:jvmTest
```
