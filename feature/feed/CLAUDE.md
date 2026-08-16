# feature/feed

**Purpose:** A chronological activity feed — transactions the user has posted, plus a
generated "daily insight" from the app's bot persona. Presentation is chat-like rather
than list-like.

**Gradle:** `:feature:feed` · `diva.kmp.compose`
**Depends on:** `:core:model`, `:core:domain`, `:core:ui`, `:core:common`

## Key files

`src/commonMain/kotlin/com/divafinance/feature/feed/`

| File | What it does |
|------|--------------|
| `FeedViewModel.kt` | `feedPosts` is a `stateIn` flow off `GetFeedPostsUseCase`. `FeedUiState` tracks only insight generation (`isGeneratingInsight`, `insightGenerated`). **`init` calls `generateInsight()`** — constructing this ViewModel has a write side effect. |
| `FeedScreen.kt` | `DivaRoutes.FEED` — the feed list |
| `DailyInsightCard.kt` | The pinned daily-insight surface at the top of the feed |
| `component/TransactionFeedItem.kt` | A posted transaction rendered as a feed entry |
| `component/BotInsightBubble.kt` | A bot message bubble |
| `component/FeedTimestamp.kt` | Relative timestamp label |

## Conventions / gotchas

- **`generateInsight()` runs on construction and is re-entrancy guarded** by the
  `isGeneratingInsight` flag; failures are swallowed and just clear the flag, so a feed
  that renders with no insight is the expected degraded state, not a bug.
- Posts are written by `PostTransactionToFeedUseCase`, called from
  `:feature:transactions` and `:feature:quickadd` — not from this module. This feature
  only reads.
- `FeedPost.type` (`FeedPostType`) decides which component renders a row. Adding a post
  kind means adding the enum constant in `core:model` and a branch in `FeedScreen`.

## Tests

`src/commonTest/FeedScreenTest.kt` — Compose UI test, runs on the JVM host.

```bash
./gradlew :feature:feed:jvmTest
```
