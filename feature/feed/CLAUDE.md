# feature/feed

**Purpose:** The **Feed tab** — the app's primary surface, and deliberately the one screen
that carries almost everything: what today cost and how it compares to pace, tappable
day/month/year summaries, the latest generated insight, and the whole ledger grouped by
day with notable spends given the full width.

**Gradle:** `:feature:feed` · `diva.kmp.compose`
**Depends on:** `:core:model`, `:core:domain`, `:core:ui`, `:core:common`

## Key files

`src/commonMain/kotlin/com/divafinance/feature/feed/`

| File | What it does |
|------|--------------|
| `FeedViewModel.kt` | Builds the entire read model from **one** flow — `GetActivityUseCase`, which already merges transactions, ledger entries and insight posts with people resolved. `FeedUiState` carries the today card, the week sparkline, the three `PeriodSummary` cards, the `DaySection` ledger and the latest insight. **`init` calls `generateInsight()`** — constructing this ViewModel has a write side effect. |
| `FeedScreen.kt` | `DivaRoutes.FEED`. App bar, today card, period cards, insight, day-grouped ledger. Navigation is hoisted as `onOpenReport` / `onOpenTransaction` / `onOpenSearch` / `onOpenInsights` / `onOpenProfile`. |
| `component/BotInsightBubble.kt` | The generated insight, as a card |
| `component/FeedTimestamp.kt` | Relative timestamp label |

## Conventions / gotchas

- **Everything derives from one flow.** The day headers, the period cards and the ledger
  are all computed from the same `GetActivityUseCase` emission, which is what stops them
  ever disagreeing with each other. Do not add a second source for a total here.
- **Spending subtracts `othersShare`.** A split bill charges the full amount to the card
  but only the user's share is consumption. `LedgerItem.ownShare` is the figure every
  total on this screen uses; using `transaction.amount` would inflate the feed against
  the graphs.
- A `LedgerItem` renders as a `MomentCard` rather than a row when it has people on it —
  a split carries information (who owes what) that a single line cannot hold.
- **Pace is the month's own running average**, not a budget: `spent this month ÷ days
  elapsed`. That makes the today card meaningful before any budget is configured.
- `generateInsight()` is re-entrancy guarded; a feed that renders with no insight is the
  expected degraded state, not a bug.
- Posts are written by `PostTransactionToFeedUseCase` from `:feature:quickadd` — not from
  this module. This feature only reads.
- The tab capsule overlays the list, so the screen's `contentPadding` leaves room at the
  bottom rather than a `Scaffold` inset.

## Tests

```bash
./gradlew :feature:feed:jvmTest
```
