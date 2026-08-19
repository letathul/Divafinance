# feature/quickadd

**Purpose:** The centre tab-bar button — a **full-screen** flow for logging a transaction,
with a calculator keypad, an account segmented control, category tiles, merchant
autocomplete, splits, and optional nearby-shop suggestions from device location.
Optimised for a few taps; everything beyond the amount has a usable default.

**Gradle:** `:feature:quickadd` · `diva.kmp.compose`
**Depends on:** `:core:model`, `:core:domain`, **`:core:data`** (reads `SettingsRepository`
for the default card directly), `:core:ui`, `:core:common`. Android adds
`androidx-activity-compose` for the permission launcher.

## Key files

`src/commonMain/kotlin/com/divafinance/feature/quickadd/`

| File | What it does |
|------|--------------|
| `QuickAddViewModel.kt` | `QuickAddUiState` holds the raw `expression` string, not a parsed amount — `previewAmount` (tolerates a dangling operator) and `committedAmount` (null unless valid and `> 0`) are derived properties over `ExpressionEvaluator` + `roundToCents()`. `onOpened()` re-reads the default card, runs `PredictCategoryUseCase`, and loads recent merchants. `QuickAddSaved` is emitted once per save so the shell can offer undo. |
| `AddExpenseScreen.kt` | `DivaRoutes.ADD_EXPENSE`. Top bar (close / title / scan), the amount, `AccountSelector`, `CategoryPickerRow`, keypad, then split, location and details sections. `WhereLine` — the caption under the amount — is the entire entry point for location. `AddExpenseContent` is the stateless body tests drive directly. |
| `LocationPermission.kt` | `fun interface LocationPermissionRequester` + `@Composable expect fun rememberLocationPermissionRequester()`. Actuals in `androidMain` (Activity result launcher), `iosMain`, and `jvmMain` (reports denial). |

The keypad now lives in **`:core:ui`** (`component/CalculatorKeypad.kt`) — the add screen
is no longer its only consumer. Its accessibility contract is load-bearing: visible labels
are typographic (`÷ × − ⌫`) while `contentDescription` is spelled out ("Divide",
"Backspace"), and the tests select on those descriptions.

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
- **Location is an optional enrichment and must never block an entry.** No permission, no
  fix, or an unsupported platform all degrade to "no nearby suggestions" — the sheet still
  saves. `rememberLocationPermissionRequester` lives here rather than in `core:common`
  next to `LocationProvider` because prompting needs an Activity result launcher, and
  `core:common` is a non-Compose module every other module depends on.
- **There is no location switch.** `WhereLine` — the "Where?" caption under the amount —
  does all three jobs from one tap, choosing by what has already been settled: opt in
  (`LocationPrompt.CHOICE`), grant (`LocationPrompt.RATIONALE`, shown *before* the OS
  dialog because that one cannot explain itself), or re-read a fix already on screen. Once
  captured, the place name replaces "Where?" and the line stays tappable.
- `UserSettings.KEY_LOCATION_CAPTURE_MODE` holds `LocationCaptureMode`; **absent is a
  third state** meaning "never asked", which is the only condition that shows the choice
  dialog. `ALWAYS` makes `onOpened()` capture without being asked; Settings can change it
  later.
- The ViewModel decides *whether* to prompt, the screen owns the launcher — Android needs
  an Activity result contract. `permissionRequestNonce` carries that decision across, and
  the screen's `LaunchedEffect` keys on it. **It must stay monotonic**: `clearedState()`
  carries it (plus the mode and the granted permission) through `reset()` and `save()`,
  because restarting at zero would make the launcher fire on a value it already handled.
- `locationNameEdited` is what stops a re-read from overwriting a name the user typed,
  while still renaming an entry whose auto-filled name is now stale.

## Tests

`src/jvmTest/` — `QuickAddViewModelTest.kt`, `AddExpenseScreenTest.kt` (Compose UI), and
`TestDoubles.kt`. Uses `:core:testing`.

```bash
./gradlew :feature:quickadd:jvmTest
```
