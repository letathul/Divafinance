# feature/cards

**Purpose:** Everything about credit cards — list, detail, add/edit, the reward-rule
editor, and the "which card should I pay with?" recommendation screen. The largest feature
module.

**Gradle:** `:feature:cards` · `diva.kmp.compose`
**Depends on:** `:core:model`, `:core:domain`, `:core:ui`, `:core:common`

## Key files

`src/commonMain/kotlin/com/divafinance/feature/cards/`

| File | What it does |
|------|--------------|
| `CardsViewModel.kt` | One ViewModel for all five screens. Holds three state shapes: `CardFormState` (add/edit, with `isEditing` derived from `editingCardId`), `RewardRuleFormState` (one per rule row, id pre-seeded from `UuidGenerator`), and `RecommendationState` (selected category, amount, results). Card list is a `stateIn` flow off `GetAllCardsUseCase`. |
| `CardsListScreen.kt` | `DivaRoutes.CARDS` — the card list |
| `CardDetailScreen.kt` | `DivaRoutes.CARD_DETAIL` (`cards/{cardId}`) |
| `AddEditCardScreen.kt` | `DivaRoutes.CARD_ADD` / `CARD_EDIT` — same screen both ways |
| `RewardMapperScreen.kt` | Reward-rule editor: category → multiplier, `RewardType`, cap amount + `CapPeriod` |
| `BestCardRecommendationScreen.kt` | `DivaRoutes.BEST_CARD` — pick a category and amount, get ranked cards |
| `component/CardCarousel.kt` | Horizontal card-face pager, built on `CreditCardVisual` from `core:ui` |
| `component/CardRecommendationCard.kt` | One row of the ranked recommendation list |
| `component/RewardCategoryList.kt` | Reward rules displayed on the detail screen |

## Conventions / gotchas

- All five screens use `DivaScaffold`; `CardsListScreen`'s add/best-card/rules affordances
  are bar `actions` rather than a FAB, because the shell already owns the bottom-right
  corner and two floating buttons on one screen collide.
- **Form numerics are `String`, not `Double`.** `creditLimit`, `annualFee`, `multiplier`,
  `capAmount`, `statementDate`, `dueDate` are all strings in form state so partial input
  (`"12."`) doesn't get clobbered; parsing happens at save.
- Add and edit share `AddEditCardScreen` and `CardFormState`. `editingCardId == null`
  means add — check `isEditing` rather than branching on the route.
- `CardsViewModel` is created **once in `DivaNavHost`** and passed to all five screens, so
  form state survives navigation between them. Don't call `koinViewModel<CardsViewModel>()`
  inside a screen.
- Recommendations come from `GetBestCardForCategoryUseCase`, which builds a fresh
  `RewardRecommendationEngine` per call using the user's configured points/miles
  valuations. Ranking logic belongs in `:core:domain`, never here.

## Tests

`src/jvmTest/CardsListScreenTest.kt` — Compose UI test. Uses `:core:testing`.

```bash
./gradlew :feature:cards:jvmTest
```
