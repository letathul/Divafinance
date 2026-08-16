# feature/dashboard

**Purpose:** The home screen — card summary, recent activity, and spending/income totals.
Read-only: it aggregates existing data and never writes.

**Gradle:** `:feature:dashboard` · `diva.kmp.compose`
**Depends on:** `:core:model`, `:core:domain`, `:core:ui`, `:core:common`

## Key files

`src/commonMain/kotlin/com/divafinance/feature/dashboard/`

| File | What it does |
|------|--------------|
| `DashboardViewModel.kt` | Four `StateFlow`s, each derived from `GetTransactionsUseCase` / `GetAllCardsUseCase` and published with `stateIn(viewModelScope, WhileSubscribed(5000), <initial>)`: `cards`, `recentTransactions` (5 newest by date), `totalSpending` (sum of `DEBIT`), `totalIncome` (sum of `CREDIT`) |
| `DashboardScreen.kt` | Renders those flows; entry point for `DivaRoutes.DASHBOARD` |

## Conventions / gotchas

- The ViewModel takes use cases as **constructor-val-less parameters** and derives
  everything at construction time — there is no `init` block, no `refresh()`, and no
  mutable state. Adding a derived metric means adding another `stateIn` property here, not
  computing it in the composable.
- `WhileSubscribed(5000)` means flows stop collecting 5s after the screen leaves
  composition. In tests this is why `installTestMainDispatcher()` from `:core:testing` is
  mandatory — without a Main dispatcher the flows never leave their initial value.
- Recent-transaction count (5) and the spending/income split are hardcoded here. If the
  dashboard needs date-bounded totals, add a use case in `:core:domain` rather than
  filtering more in this module.

## Tests

`src/jvmTest/DashboardScreenTest.kt` — Compose UI test. Uses `:core:testing`.

```bash
./gradlew :feature:dashboard:jvmTest
```
