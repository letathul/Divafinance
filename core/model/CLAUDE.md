# core/model

**Purpose:** The app's vocabulary — plain `@Serializable` data classes and enums with no
behaviour beyond a few companion constants. This is the bottom of the dependency graph:
it depends on nothing in the project, and everything else may depend on it.

**Gradle:** `:core:model` · `diva.kmp.library` + kotlinx-serialization
**Depends on:** nothing internal. `api`-exposes kotlinx-serialization-json and
kotlinx-datetime, because `LocalDate`/`Instant` and `@Serializable` appear in the public
signatures consumers see.
**Depended on by:** every other module (usually transitively via `api` from `core:data`,
`core:domain`, `core:ui`, or `core:testing`).

## Key files

All under `src/commonMain/kotlin/com/divafinance/core/model/`.

| File | What it holds |
|------|---------------|
| `Account.kt` | A funding account (checking, savings, …) |
| `CreditCard.kt` | Card identity, network, limits, statement dates |
| `CardRewardRule.kt` | One reward rule on a card: category, rate, type, cap |
| `RewardCategory.kt` | Category ↔ reward-rate pairing used by the recommendation engine |
| `Transaction.kt` | A spend/refund record: amount, merchant, category, card, date, `tags`, `customCategoryId` |
| `CustomCategory.kt` | A category the user invented. **A display identity, not a new dimension** — its `parent` is the built-in `SpendingCategory` it behaves as everywhere off screen, so reward rules, thresholds and prediction keep working on the twelve-value enum |
| `Receipt.kt` | Scanned-receipt record with OCR text and parse status |
| `FeedPost.kt` | An entry in the activity feed (transaction post or bot insight) |
| `LocationTag.kt` | Lat/long + label attached to a transaction |
| `GraphThreshold.kt` | A per-category spending threshold used by the graphs feature |
| `UserSettings.kt` | Currency, PIN hash + salt, onboarding-complete flag, toggles |
| `BackupArchive.kt` | Serializable envelope for the full export/import payload |
| `Currency.kt` | `Currency(code, name, symbol)` + 7 supported currencies in `Currency.supported`, plus `fromCode()` which falls back to an unknown-code currency rather than throwing |
| `enums/` | `SpendingCategory` (12 values, each with a `displayName`), `CardNetwork`, `RewardType`, `CapPeriod`, `TransactionType`, `AccountType`, `FeedPostType`, `ReceiptStatus`, `LedgerEntryKind`, `LocationCaptureMode` (`ALWAYS` / `ON_TAP` / `NEVER`, and **absent is a fourth state** meaning never asked) |

## Conventions / gotchas

- Keep this module free of logic. Validation, defaulting, and computation belong in
  `core:domain`; persistence concerns belong in `core:data`.
- Every class and enum is `@Serializable` — they cross the wire (`core:network`,
  `:server`) and go into backup archives, so adding a non-serializable field breaks both.
- Enums are persisted by name via `core:database`'s `EnumColumnAdapters`. **Renaming an
  enum constant is a data migration**, not a refactor; adding a constant is safe.
- `SpendingCategory.displayName` is what the UI shows. Change the display string freely;
  never change the constant name.

## Tests

None — there is no behaviour to test. Model types are exercised through
`core:domain`'s tests and the fixtures in `core:testing`.
