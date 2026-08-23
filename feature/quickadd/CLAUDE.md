# feature/quickadd

**Purpose:** The centre tab-bar button — a **full-screen** flow for logging a transaction:
a floating white card on an accent gradient, carrying the amount, a currency picker, an
account segmented control, category pills, and four disclosure chips (note, location,
split, receipt) that each reveal their block in place. The keypad is a bottom sheet behind
the amount. Optimised for a few taps; everything beyond the amount has a usable default.

**Gradle:** `:feature:quickadd` · `diva.kmp.compose`
**Depends on:** `:core:model`, `:core:domain`, **`:core:data`** (reads `SettingsRepository`
for the default card directly), `:core:ui`, `:core:common`. Android adds
`androidx-activity-compose` for the permission launcher.

## Key files

`src/commonMain/kotlin/com/divafinance/feature/quickadd/`

| File | What it does |
|------|--------------|
| `QuickAddViewModel.kt` | `QuickAddUiState` holds the raw `expression` string, not a parsed amount — `previewAmount` (tolerates a dangling operator) and `committedAmount` (null unless valid and `> 0`) are derived properties over `ExpressionEvaluator` + `roundToCents()`. `onOpened()` re-reads the default card **and the base currency**, runs `PredictCategoryUseCase`, and loads recent merchants. `QuickAddSaved` is emitted once per save so the shell can offer undo. |
| `AddExpenseScreen.kt` | `DivaRoutes.ADD_EXPENSE`. Its own chrome (see below): header, `DayChip`, then the card — `AmountBlock`, `TypeAndAccountRow`, `CategoryBlock`, `DetailChips`, and the three blocks behind them (`NoteBlock`, `LocationBlock`, `SplitBlock`), over a pinned save. `AmountSheet` is the keypad. `AddExpenseContent` is the stateless body tests drive directly. |
| `LocationPermission.kt` | `fun interface LocationPermissionRequester` + `@Composable expect fun rememberLocationPermissionRequester()`. Actuals in `androidMain` (Activity result launcher), `iosMain`, and `jvmMain` (reports denial). |

**This screen draws its own chrome** and is the third deliberate exception to the
`DivaScaffold` rule, alongside `OnboardingScreen` and `MapFallbackScreen`. It is a
full-bleed gradient with a floating card, opened from the centre tab-bar button and
outside the tab shell entirely; a nav bar over it would be chrome for a screen with
exactly one way out. `addExpenseGradient()` is built from `diva.accent` rather than fixed
hexes, so the one full-bleed colour surface in the app follows the user's accent instead
of clashing with every other screen. The card is the **one** place this design system uses
elevation — a hairline has nothing to separate it from on a saturated gradient.

`AddExpenseContent` is a scrolling card body over a **pinned** save action, not one long
scroll: the amount is the only required field, so the commit stays reachable from the
moment it is entered.

The keypad lives in **`:core:ui`** (`component/CalculatorKeypad.kt`) and this screen takes
its `extended = true` variant — the 5×4 pad with `( ) C =`. Its accessibility contract is
load-bearing: visible labels are typographic (`÷ × − ⌫ =`) while `contentDescription` is
spelled out ("Divide", "Backspace", "Open bracket", "Clear", "Equals"), and the tests
select on those descriptions.

`QuickAddDay` offers `TODAY` / `YESTERDAY` only — backdating further is the full form's
job, and a date picker here would be a modal in front of a modal. `+ New` expands the
remaining categories past the `SUGGESTED_CATEGORY_COUNT = 5` predicted ones.

`ScreenGutter` is applied by the screen's **outer `Column`**, so the header, the `DayChip`
and the card all share one horizontal inset. Don't re-apply it inside those children.

## Conventions / gotchas

- **Two split paths, never mixed.** `SplitCountSelector` is the quick one: the same segmented
  control the account row uses, labelled `Just me` / `Split with N`, with a **long press**
  on any segment opening the full range (`SplitCountDialog`, 0–12). It sets
  `splitWithCount` and needs no names — unnamed heads owe nothing back, so the save goes
  through `AddTransactionUseCase` with `othersShare` set and writes no ledger entries. The
  slow path is naming people in `SplitPeopleRow`, which does create debts. Naming someone
  collapses `splitWithCount` onto `splitWith.size`, and picking a count that disagrees with
  the named list replaces it — `splitParticipants()` therefore never has to reconcile the
  two, which matters because `SaveSplitTransactionUseCase` requires the shares it is given
  to add up to `othersShare` exactly. `splitOthers` is the one number the UI reads. Both
  live inside `SplitBlock`, which the Split chip opens — so the count control is not
  reachable until splitting is on, and `onSplitToggled(true)` is what turns it on.
- **The bill is always split evenly, and the payer is always you.** The design's
  *Split evenly / Custom* control and its *Who paid?* row have nothing behind them here —
  `BillSplitEngine` divides evenly and `SplitParticipant` index 0 is the payer — so the
  block carries the participants, the tip presets and the live breakdown instead. Custom
  shares would need per-person inputs that add up to `othersShare` exactly.
- **Custom categories do not exist.** `SpendingCategory` is a fixed enum of **twelve** with
  a colour ramp and an icon per member, so `+ New` expands the seven left after the five
  suggested ones rather than opening the design's emoji-and-name creator.
- **Icons, not emoji.** Category colour and glyph are this app's data encoding
  (`CategoryVisuals`), so the chips and pills use the same Material icons every other
  screen does.
- **The Receipt chip opens the scanner** (`onOpenScanner` → `:feature:scanner`) rather
  than a receipt editor of its own. Parsing a receipt into line items is that module's
  job, and duplicating it here would be a second implementation of it.
- **The Location chip is the whole entry point, and `LocationBlock` is the whole of the
  rest.** There are no location dialogs on this screen. The block shows one of two things:

  | `showLocation`, and… | shows |
  |---|---|
  | no fix and no permission (`locationNeedsConsent`), or a `locationPrompt` is set | the consent card: *Not now* / *Allow* |
  | anything else | the place row, nearby chips, the editable `Place` field, the auto-capture switch, *Find me again* |

  **Consent is one card, not two dialogs.** The card *is* the rationale the OS prompt
  cannot give, so `onLocationAllowed()` records the opt-in (`ON_TAP`) and bumps
  `permissionRequestNonce` in the same move. The older two-step flow
  (`onLocationCaptureModeChosen` / `onLocationRationaleAccepted` /
  `onLocationPromptDismissed`, and `LocationPrompt.CHOICE` vs `.RATIONALE`) is **gone** —
  it had no callers once `LocationBlock` rendered the identical card for either value.
  `LocationPrompt` has one value, `CONSENT`. The auto-capture switch goes through
  `onAutoCaptureChanged`, **not** `onLocationAllowed`, which would re-request a permission
  already granted. Nearby shops are `SuggestNearbyPlacesUseCase` output as chips; picking
  one fills the merchant in too. *Find me again* is `onWhereTapped`, which captures when
  permission is held and raises the consent card when it isn't — so a permission revoked
  since capture is re-asked for rather than silently failing. `onLocationCleared`
  closes the block and cancels `locationJob`, because a fix landing a moment later would
  otherwise put back the place the user just removed.
- **Currency is per entry.** `onOpened()` seeds `currency` from
  `UserSettings.KEY_BASE_CURRENCY` and `save()` writes `state.currency` — picking one from
  the pill's list denominates *that* entry without re-labelling every report. `save()` no
  longer reads the setting itself, so a ViewModel test that skips `onOpened()` gets `USD`.
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
- `UserSettings.KEY_LOCATION_CAPTURE_MODE` holds `LocationCaptureMode`; **absent is a
  third state** meaning "never asked", and `onLocationAllowed` is what first writes it.
  Note `locationNeedsConsent` does **not** read it — it is
  `showLocation && location == null && !locationPermissionGranted && !isLocatingNow`, so
  the consent card is driven by the permission, not by the stored mode. `ALWAYS` makes `onOpened()` capture without being asked *and* open
  the location block, because a place acquired invisibly is a place the user never saw the
  entry acquire; Settings can change it later.
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
