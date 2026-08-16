# core/domain

**Purpose:** Business logic. 28 use cases, each a small class with a single
`operator fun invoke(...)`, plus the `RewardRecommendationEngine` and
`CategoryPredictionEngine`. Feature modules call use cases; they never call
repositories through this module.

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
| `usecase/location/` | `TagTransactionLocationUseCase`, `GetSpendingByLocationUseCase` |
| `usecase/scanner/` | `ParseReceiptUseCase`, `ImportStatementUseCase` |

Behaviours worth knowing before touching them:

- `GetBestCardForCategoryUseCase` reads `UserSettings.KEY_POINTS_VALUE` /
  `KEY_MILES_VALUE` and constructs a **fresh engine per call** with those valuations
  (defaults 1.0¢ and 1.5¢). Card reward rules are re-hydrated from `RewardRepository`
  because `CardRepository.getAll()` returns cards without them.
- `AddTransactionUseCase` also updates the card balance — but only for `DEBIT`
  transactions on a card. Inserting a transaction through the repository directly skips
  this side effect. `DeleteTransactionUseCase` is its exact inverse and must be used for
  any removal, or the card stays permanently overstated.
- `PredictCategoryUseCase` always returns `limit` categories, padding a thin prediction
  from a fixed fallback list so a new install still gets a full chip row.

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

`src/commonTest/` — 25 test files, the densest test coverage in the repo: one per use case
(except `SetPinUseCase`) plus `RewardRecommendationEngineTest`. They run against the fakes
in `:core:testing`, which is wired as a `commonTest` dependency.

```bash
./gradlew :core:domain:jvmTest
```
