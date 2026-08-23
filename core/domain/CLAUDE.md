# core/domain

**Purpose:** Business logic. 28 use cases, each a small class with a single
`operator fun invoke(...)`, plus three pure engines and the premium boundary. Feature
modules call use cases; they never call repositories through this module.

**Gradle:** `:core:domain` · `diva.kmp.library`
**Depends on:** `:core:model` (`api` — use-case return types), `:core:data`,
`:core:common`
**Depended on by:** every `feature/*` module

## Key files

`src/commonMain/kotlin/com/divafinance/core/domain/`

| Path | What it does |
|------|--------------|
| `engine/RewardRecommendationEngine.kt` | The core algorithm. `rankCards(cards, category, amount)` filters to active cards with enough available credit, picks each card's highest-multiplier active rule for the category, converts the raw reward to currency by `RewardType` (cashback 1:1, points/miles via configurable cents-per-unit), clamps to the rule's cap, and sorts descending. Returns `CardRecommendation(card, rule, estimatedRewardValue, remainingCap, availableCredit)`. |
| `engine/CategoryPredictionEngine.kt` | Ranks spending categories for a part-entered transaction. Sums per-category scores from four signals — merchant match (dominant), time-decayed recency, time-of-day/day-kind, and amount magnitude — then normalises. Pure: history is passed in. Deliberately has no location term; nothing writes coordinates during entry yet. |
| `usecase/cards/` | `GetAllCardsUseCase`, `AddCardUseCase`, `UpdateCardUseCase`, `GetBestCardForCategoryUseCase`, `CalculateRewardValueUseCase` |
| `usecase/transactions/` | `AddTransactionUseCase`, `GetTransactionsUseCase`, `DeleteTransactionUseCase`, `GetSpendingByCategoryUseCase`, `GetHighImpactTransactionsUseCase`, `PredictCategoryUseCase`, `SuggestMerchantsUseCase` |
| `usecase/onboarding/` | `CompleteOnboardingUseCase`, `InitializeDatabaseUseCase`, `SetPinUseCase`, `ValidatePinUseCase` |
| `usecase/graphs/` | `GetThresholdGraphDataUseCase`, `ConfigureThresholdUseCase` |
| `usecase/feed/` | `GetFeedPostsUseCase`, `PostTransactionToFeedUseCase`, `GenerateDailyInsightUseCase` |
| `usecase/backup/` | `ExportBackupUseCase`, `ImportBackupUseCase` |
| `usecase/location/` | `TagTransactionLocationUseCase`, `GetSpendingByLocationUseCase`, `SuggestNearbyPlacesUseCase` |
| `usecase/scanner/` | `ParseReceiptUseCase`, `ImportStatementUseCase`, `ConfirmReceiptUseCase`, `GetReceiptUseCase`, `GetReceiptsUseCase`, and `ReceiptParser` — a pure object holding all the OCR extraction (total, merchant, date). `today` is passed into `parse` so the date sanity window is deterministic; the use case persists, the parser never does. |
| `engine/NearbyPlaceEngine.kt` | `nearby(history, origin, radiusMetres, limit)` — finds shops the user has already spent at near a point, ranked by visits and distance. Their own history is the whole data source: it works offline and sends location nowhere, but a never-visited shop can't be suggested. Pure, like the other two engines. |
| `premium/PremiumGate.kt` | `interface PremiumGate { isPremium: Flow<Boolean>; isPremiumNow() }` + `SettingsPremiumGate`, reading `UserSettings.KEY_IS_PREMIUM`. **No billing integration** — nothing sets the flag yet; the boundary exists so premium features aren't written against a concrete implementation. |

Behaviours worth knowing before touching them:

- `GetBestCardForCategoryUseCase` reads `UserSettings.KEY_POINTS_VALUE` /
  `KEY_MILES_VALUE` and constructs a **fresh engine per call** with those valuations
  (defaults 1.0¢ and 1.5¢). Card reward rules are re-hydrated from `RewardRepository`
  because `CardRepository.getAll()` returns cards without them.
- `AddTransactionUseCase` also updates the card balance — but only for `DEBIT`
  transactions on a card. Inserting a transaction through the repository directly skips
  this side effect. `DeleteTransactionUseCase` is its exact inverse and must be used for
  any removal, or the card stays permanently overstated. It also removes the linked
  `Receipt` row **and its image files** (`imagePath` plus every `pagePaths` entry), through
  `ReceiptFileStore` — a receipt is found by `transaction_id`, so that step has to happen
  *before* the transaction is deleted or the row and files are stranded forever.
- `ImportStatementUseCase` categorises imported rows through `CategoryPredictionEngine`
  rather than writing `OTHER` for everything. History is read once, before the loop, so a
  300-row statement is one query and the result doesn't depend on row order.
- `PredictCategoryUseCase` always returns `limit` categories, padding a thin prediction
  from a fixed fallback list so a new install still gets a full chip row.
- `ReceiptStatus.PROCESSED` means **"linked to a transaction"** and is set only by
  `ConfirmReceiptUseCase`, which writes both sides of the receipt↔transaction link and saves
  through `AddTransactionUseCase` so the card balance stays right. `ParseReceiptUseCase` always
  stores `PENDING` (or `FAILED` when OCR returned nothing), which is what makes a scan
  resumable from the scanner's history tab.

## Conventions / gotchas

- One use case per file, named `<Verb><Noun>UseCase`, invoked as `useCase(args)` via
  `operator fun invoke`. Dependencies come in through the constructor.
- Use cases are registered as singletons in
  `composeApp/src/commonMain/.../di/DomainModule.kt`. A new use case is not reachable
  until it's added there.
- No Compose, no Android, no SQLDelight imports in this module.
- `RewardRecommendationEngine` is a plain class with no dependencies — construct it
  directly in tests rather than mocking.

## Tests

`src/commonTest/` — 30 test files, the densest test coverage in the repo: one per use case
(except `SetPinUseCase`) plus a test per engine. They run against the fakes in
`:core:testing`, which is wired as a `commonTest` dependency.

```bash
./gradlew :core:domain:jvmTest
```
