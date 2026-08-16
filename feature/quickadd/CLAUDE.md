# feature/quickadd

**Purpose:** The fast path for logging a transaction — a bottom sheet with a calculator
keypad, predicted category chips, and merchant autocomplete, reachable from anywhere in
the app. Optimised for a few taps; anything needing the full form goes to
`:feature:transactions`.

**Gradle:** `:feature:quickadd` · `diva.kmp.compose`
**Depends on:** `:core:model`, `:core:domain`, **`:core:data`** (reads `SettingsRepository`
for the default card directly), `:core:ui`, `:core:common`

## Key files

`src/commonMain/kotlin/com/divafinance/feature/quickadd/`

| File | What it does |
|------|--------------|
| `QuickAddViewModel.kt` | `QuickAddUiState` holds the raw `expression` string, not a parsed amount — `previewAmount` (tolerates a dangling operator) and `committedAmount` (null unless valid and `> 0`) are derived properties over `ExpressionEvaluator` + `roundToCents()`. `onOpened()` re-reads the default card, runs `PredictCategoryUseCase`, and loads recent merchants. `QuickAddSaved` is emitted once per save so the sheet can offer undo. |
| `QuickAddSheet.kt` | The sheet UI: amount display, keypad, category chips, expandable details |
| `CalculatorKeypad.kt` | `Key` sealed interface (`Digit` / `Operator` / `Backspace`) over a 4×4 `KEY_ROWS` grid. Replaces the soft keyboard entirely. Operator labels are typographic (`÷ × −`) while the emitted chars are ASCII (`/ * -`). |

`QuickAddDay` offers `TODAY` / `YESTERDAY` only — backdating further is the full form's
job. `SUGGESTED_CATEGORY_COUNT = 5` chips before the full list expands.

## Conventions / gotchas

- **This ViewModel deliberately does not reuse `TransactionsViewModel`.** That one is
  hoisted at NavHost scope and shares form state between the list and full-add screens,
  relying on callers to `resetForm()` first. The sheet is reachable from anywhere, so it
  owns and clears its own state.
- `onOpened()` exists instead of `init` so a default changed in settings takes effect
  without the ViewModel being recreated. Call it every time the sheet opens.
- Merchant autocomplete cancels `suggestionJob` on each keystroke so only the latest query
  lands. Typing a merchant also re-runs category prediction — a known shop is the
  strongest available signal.
- Switching type to `CREDIT` clears `selectedCardId`; a card only makes sense for money
  going out.
- Undo works by calling `DeleteTransactionUseCase`, which reverses the card-balance
  side effect of `AddTransactionUseCase`. Don't delete through the repository directly.

## Tests

`src/jvmTest/` — `QuickAddViewModelTest.kt` and `QuickAddSheetTest.kt` (Compose UI). Uses
`:core:testing`.

```bash
./gradlew :feature:quickadd:jvmTest
```
