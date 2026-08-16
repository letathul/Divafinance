# feature/transactions

**Purpose:** The full transaction surface — filterable/sortable list, detail view, and the
full add form. (The fast path for entry is `:feature:quickadd`, a separate module.)

**Gradle:** `:feature:transactions` · `diva.kmp.compose`
**Depends on:** `:core:model`, `:core:domain`, `:core:ui`, `:core:common`

## Key files

`src/commonMain/kotlin/com/divafinance/feature/transactions/`

| File | What it does |
|------|--------------|
| `TransactionsViewModel.kt` | Owns `TransactionFilterState` (category filter, type filter, `TransactionSortOrder`, search query) and `TransactionFormState`. The visible list is `combine(transactions, filterState)` published via `stateIn`. Takes `GetTransactionsUseCase`, `AddTransactionUseCase`, `GetAllCardsUseCase`, `PostTransactionToFeedUseCase`. |
| `TransactionListScreen.kt` | `DivaRoutes.TRANSACTIONS` |
| `TransactionDetailScreen.kt` | Single transaction view |
| `AddTransactionScreen.kt` | `DivaRoutes.TRANSACTION_ADD` — amount, category, type, merchant, note, card |

`TransactionSortOrder` has four values, each with a display `label`:
`DATE_DESC` ("Newest First"), `DATE_ASC`, `AMOUNT_DESC`, `AMOUNT_ASC`.

## Conventions / gotchas

- **Saving a transaction has two side effects beyond the insert.**
  `AddTransactionUseCase` updates the card balance (only for `DEBIT` on a card), and this
  ViewModel additionally calls `PostTransactionToFeedUseCase`. Writing through
  `TransactionRepository` directly skips both.
- Filtering and sorting happen **in the ViewModel**, over the full flow — there is no
  paging and no SQL-level filter. If the list gets large this is the thing to change, and
  the query belongs in `:core:data`.
- `amount` in `TransactionFormState` is a `String`, parsed at save time.
- `:feature:quickadd` writes transactions too. A change to how a transaction is
  constructed usually needs mirroring there.

## Tests

`src/jvmTest/TransactionListScreenTest.kt` — Compose UI test. Uses `:core:testing`.

```bash
./gradlew :feature:transactions:jvmTest
```
