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

**`repository/`** — 9 interface + implementation pairs:

| Interface | Impl | Owns |
|-----------|------|------|
| `AccountRepository` | `AccountRepositoryImpl` | Accounts |
| `CardRepository` | `CardRepositoryImpl` | Credit cards; `getById` also loads the card's reward rules via `RewardRepository` |
| `RewardRepository` | `RewardRepositoryImpl` | Reward rules per card |
| `TransactionRepository` | `TransactionRepositoryImpl` | Transactions + category/date aggregate queries |
| `ReceiptRepository` | `ReceiptRepositoryImpl` | Scanned receipts |
| `FeedRepository` | `FeedRepositoryImpl` | Feed posts |
| `ThresholdRepository` | `ThresholdRepositoryImpl` | Per-category spending thresholds |
| `SettingsRepository` | `SettingsRepositoryImpl` | The single `UserSettings` row |
| `BackupRepository` | `BackupRepositoryImpl` | `exportAll()` / `importAll(archive, replaceExisting)` across every table |

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
- **Do not leak SQLDelight types out of this module.** Anything returned to `core:domain`
  must already be a `core:model` type.

## Tests

`src/commonTest/` is configured but currently empty — repository behaviour is exercised
through `core:domain` tests against the fakes in `:core:testing`. When you add a
repository, add its fake there too.

```bash
./gradlew :core:data:jvmTest
```
