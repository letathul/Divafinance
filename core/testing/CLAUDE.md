# core/testing

**Purpose:** Shared test fixtures — in-memory repository fakes, sample-data builders, and
the test Main-dispatcher helpers. Consumed by other modules' test source sets.

**Gradle:** `:core:testing` · `diva.kmp.library`
**Depends on:** `:core:model` (`api`), `:core:data` (`api` — the fakes implement its
interfaces), `kotlinx-coroutines-test` (`api`)
**Depended on by (as a `commonTest`/`jvmTest` dependency only):** `:core:domain`,
`:feature:cards`, `:feature:feed`, `:feature:transactions`, `:feature:quickadd`,
`:feature:onboarding`

## Key files

`src/commonMain/kotlin/com/divafinance/core/testing/`

| File | What it does |
|------|--------------|
| `MainDispatcherTest.kt` | `installTestMainDispatcher()` / `resetTestMainDispatcher()`. Call the first from `@BeforeTest` and the second from `@AfterTest` in any test that touches a ViewModel. |
| `fake/TestData.kt` | Builder functions (`card()`, `transaction()`, …) with sensible defaults and named overrides, so a test only states the fields it cares about |
| `fake/FakeAccountRepository.kt` | In-memory `AccountRepository` |
| `fake/FakeCardRepository.kt` | In-memory `CardRepository` |
| `fake/FakeRewardRepository.kt` | In-memory `RewardRepository` |
| `fake/FakeTransactionRepository.kt` | In-memory `TransactionRepository` |
| `fake/FakeReceiptRepository.kt` | In-memory `ReceiptRepository` |
| `fake/FakeReceiptFileStore.kt` | `ReceiptFileStore` that tracks deletions instead of performing them. `seed(...)` makes the "file already gone" case testable: an unseeded path reports `false`, as the real filesystem does. |
| `fake/FakeFeedRepository.kt` | In-memory `FeedRepository` |
| `fake/FakeThresholdRepository.kt` | In-memory `ThresholdRepository` |
| `fake/FakeSettingsRepository.kt` | In-memory `SettingsRepository` |
| `fake/FakeBackupRepository.kt` | In-memory `BackupRepository` |

## Conventions / gotchas

- **This is `commonMain`, not `commonTest`.** Gradle test fixtures aren't available for
  KMP source sets, so fixtures have to live in a main source set for other modules to
  depend on them. Nothing in production code depends on `:core:testing`, so it never
  ships — but that also means **nothing here is stripped by the compiler**; keep it small.
- Always declare it as a test-only dependency (`commonTest.dependencies` /
  `jvmTest.dependencies`), never as `implementation`.
- `installTestMainDispatcher()` uses `UnconfinedTestDispatcher`, not
  `StandardTestDispatcher`, so upstream flows emit eagerly on subscription. Without it,
  `stateIn(WhileSubscribed)` flows in a ViewModel stay on their initial value and UI
  assertions fail with an opaque timeout rather than a useful message.
- The helpers are functions, not a base class, because `kotlin.test`'s `@BeforeTest`
  annotations are only wired into test compilations and this is a main source set.
- Adding a repository to `:core:data` means adding its fake here, or the domain tests
  can't be written.

## Tests

None of its own — this module *is* the test support.
