# feature/quickadd

**Purpose:** The centre tab-bar button — a **full-screen** flow for logging a transaction,
built to `addTransaction/add-transaction-ux-spec.md`. A flat canvas carrying an
Expense/Income toggle, the amount with a currency badge under it, three predicted category
chips plus **More**, the best-card chip, the date pill, a ghost note/tags row, and a 3-up
action row (Scan Receipt / Split / Add Location) over a pinned save. The keypad, the
calendar, the split and the location consent are all sheets. Optimised for a few taps;
everything beyond the amount has a usable default.

**Gradle:** `:feature:quickadd` · `diva.kmp.compose`
**Depends on:** `:core:model`, `:core:domain`, **`:core:data`** (reads `SettingsRepository`,
`CustomCategoryRepository`, `PersonRepository` and `LedgerRepository` directly), `:core:ui`,
`:core:common`. Android adds `androidx-activity-compose` for the permission launchers.

## Key files

`src/commonMain/kotlin/com/divafinance/feature/quickadd/`

| File | What it does |
|------|--------------|
| `QuickAddViewModel.kt` | `QuickAddUiState` holds the raw `expression` string, not a parsed amount — `previewAmount` (tolerates a dangling operator) and `committedAmount` (null unless valid and `> 0`) are derived properties over `ExpressionEvaluator` + `roundToCents()`. `onOpened()` re-reads the default card, the base currency, the custom categories and whether the platform does location at all, runs `PredictCategoryUseCase`, loads recent merchants and ranks the best card. `QuickAddSaved` is emitted once per save so the shell can offer undo. |
| `AddExpenseScreen.kt` | `DivaRoutes.ADD_EXPENSE`. Its own chrome (see below): `AddExpenseHeader`, `TypeToggle`, `AmountBlock`, `CategoryBlock`, `BestCardChip`, `CardSelector`, `DatePill`, `NoteRow`, `LocationBlock`, `SplitSummary`, then the pinned `ActionRow` + save. `AmountSheet` is the keypad; `DateSheet`, `SplitSheet` and `LocationConsentSheet` are the three `DivaBottomSheet`s. `AddExpenseContent` is the stateless body tests drive directly. |
| `CategoryPickerScreen.kt` | `DivaRoutes.ADD_CATEGORY` — search, a Recent row, the grid of built-ins plus the user's own, and the `+ Add custom` creator. Writes into the **same hoisted `QuickAddViewModel`** the add screen uses, so a selection needs no navigation-result plumbing. |
| `AddPeopleSheet.kt` | `AddPeopleBody` — the roster picker, rendered on the split sheet's own surface. Search, a **Frequent** list built from past splits, a **From contacts** list once imported, the add-by-name form with its colour swatches, the Import row, and the `N selected` / *Add to Split* footer. |
| `LocationPermission.kt` | `fun interface LocationPermissionRequester` + `@Composable expect fun rememberLocationPermissionRequester()`. Actuals in `androidMain` (Activity result launcher), `iosMain`, and `jvmMain` (reports denial). |
| `ContactPicker.kt` | `fun interface ContactImporter` + `@Composable expect fun rememberContactImporter()` and `expect fun contactsSupported()`. Actuals in `androidMain` (`READ_CONTACTS` + a `ContactsContract` query off the main thread), `iosMain` (`CNContactStore`), and `jvmMain` (reports nothing). **Names only** — no numbers, no emails, no ids. |

**This screen draws its own chrome** and is the third deliberate exception to the
`DivaScaffold` rule, alongside `OnboardingScreen` and `MapFallbackScreen`. It is opened
from the centre tab-bar button and sits outside the tab shell entirely; a nav bar over it
would be chrome for a screen with exactly one way out.

**The gradient and the floating card are gone.** The screen is now a flat `diva.canvas`
surface, matching the reference design, and it no longer uses elevation anywhere — which
puts it back in line with the rest of the design system rather than being the one
exception to "separation is tone plus a hairline, never elevation". Every accent on it
still comes from `diva.accent`, so it follows the user's chosen theme; the default accent
is `AccentTheme.EMERALD`, which is the teal the design was drawn in.

`AddExpenseContent` is a scrolling card body over a **pinned** save action, not one long
scroll: the amount is the only required field, so the commit stays reachable from the
moment it is entered.

The keypad lives in **`:core:ui`** (`component/CalculatorKeypad.kt`) and this screen takes
its `extended = true` variant — the 5×4 pad with `( ) C =`. Its accessibility contract is
load-bearing: visible labels are typographic (`÷ × − ⌫ =`) while `contentDescription` is
spelled out ("Divide", "Backspace", "Open bracket", "Clear", "Equals"), and the tests
select on those descriptions.

**Any past date, from a calendar sheet.** `QuickAddDay` is gone; the state carries a real
`LocalDate` and `DatePill` opens a `DivaBottomSheet` holding Today/Yesterday chips over
`DivaCalendar`. Selecting closes the sheet in the same gesture, and future dates are
greyed out *and* refused by `onDateChange` — a transaction that has not happened is not a
record.

The chip row offers `SUGGESTED_CHIP_COUNT = 3` predictions plus **More**, which opens
`CategoryPickerScreen`. `SUGGESTED_CATEGORY_COUNT = 5` is still what
`PredictCategoryUseCase` is asked for, because the picker's Recent row uses the longer
list.

`ScreenGutter` is applied by the screen's **outer `Column`**, so the header, the `DayChip`
and the card all share one horizontal inset. Don't re-apply it inside those children.

## Conventions / gotchas

- **The roster is a sheet of its own, on the split sheet's surface.** `AddPersonPanel` and
  `NewSplitPersonField` are gone; the dashed seat at the end of `PaidByRow` and the dashed
  row at the end of `ShareList` both call `onAddPeopleSheetOpened()`, and `SplitSheet`
  swaps its body for `AddPeopleBody`. **One `DivaBottomSheet`, two bodies** — stacking a
  second `ModalBottomSheet` over the first would put two scrims on screen and leave a back
  gesture with two things it could mean, so `onDismissRequest` closes the roster first when
  it is open and the split sheet otherwise.
  **Selection is a batch.** Ticking someone adds them to `pendingPeople`; only
  `onConfirmPeople()` folds them into `splitWith`, one at a time through `onAddSplitPerson`.
  Three people picked is the bill changing length once rather than three times, each of
  which would reset the positional share lists in turn. Dismissing discards the batch.
  Unticking someone *already on* the bill takes them off it there and then — the tick is
  one statement about whether they are on this bill, so it cannot mean "queued" in one
  direction and "added" in the other. Holding an avatar on `PaidByRow` still removes, and
  that long press keeps its `onLongClickLabel`.
  **Frequent is built from splits already made.** `peopleSplitCounts` counts a person's
  ledger entries that carry a `transactionId`, distinct by transaction — that is what ties
  a debt to the spend that created it, so a cash settlement is not a bill they were on, and
  two entries from one bill are one split. This is what makes the address book optional
  rather than the way in.
  **A person is a name and a colour.** No phone number, no email — a split is settled
  between people who already know each other, and the app is local-first, so there is
  nothing for an identifier to reach. `onCreatePerson()` writes the row immediately rather
  than at save time, so someone named for an abandoned entry is still under Frequent next
  time; typing a name that already exists recolours that person instead of making a second.
  `personColor(name, colorHex)` in `:core:ui` resolves the avatar, falling back to the name
  hash for anyone written before `Person.colorHex` existed.
  **Importing contacts is offered, never required.** `onContactsImported` fills
  `importedContacts` — a second list to pick from — rather than `pendingPeople`, because
  dumping an address book onto a bill is not a selection. The row only appears where
  `contactsSupported` is true, read in `onOpened()` the way `locationSupported` is.
  **Open follow-up:** the ViewModel still keeps the whole unnamed-count path —
  `splitWithCount`, `onSplitCountChange`, the `"Person N"` branch of `splitParticipants()`,
  and the `AddTransactionUseCase` save branch that writes `othersShare` with no ledger
  entries — along with its six tests, but **nothing calls `onSplitCountChange` any more**.
  It was left in rather than ripped out because deleting it is a refactor of `save()`, not a
  UI trim. Either give it an entry point or delete it deliberately; do not leave it
  half-alive. Note `onAddSplitPerson` still collapses `splitWithCount` onto
  `splitWith.size`, so `splitParticipants()` never has to reconcile the two — which matters
  because `SaveSplitTransactionUseCase` requires the shares it is given to add up to
  `othersShare` exactly.
- **The split sheet has three modes and a real payer.** `SplitMode` is
  `EQUALLY` / `BY_AMOUNT` / `BY_PERCENT`. By-amount goes to `SplitMethod.ByExactAmounts`,
  which *rejects* shares that do not reach the total — that rejection is what drives the
  "X of Y allocated" check and keeps `canSave` false until it balances. By-percent goes to
  `SplitMethod.ByShares` with the percentages as weights, so the engine's largest-remainder
  allocation still makes the shares add back to the exact total; percentages are
  deliberately not a `SplitMethod` of their own.
  `splitCustomAmounts` / `splitPercents` are **positional**, index 0 being you, so adding
  or removing anyone clears them and drops back to `EQUALLY`.
  **By percent has two ways in, because they answer different questions.** `PercentStepper`
  draws `− [34 %] +` and the person's live money figure beside it: the steppers are for "a
  bit more than an even share", the typed field for "she had the 60". `PercentBox` is a
  `Text` until tapped and only then a focused `BasicTextField` — three focusable fields in a
  row turns a sheet the user is reading into a form they have to tab through. The typed text
  is held locally and parsed upward, so a half-typed "3" is not rewritten as "3.0" under the
  cursor. `onSplitShareChange` **clamps the percent branch to 0..100**, not just to ≥ 0:
  the steppers would otherwise walk a share off the end of a scale where past-100 has no
  meaning the allocation check could explain. The stepper glyphs carry spelled-out
  `contentDescription`s ("One percent more for Sam") — the same contract `CalculatorKeypad`
  holds, and what the tests select on.
  The allocation line reads **"100% allocated · $57.80 of $57.80"** once a percentage split
  balances, because percentages are not what anyone owes. It falls back to
  "97% of 100% allocated" when it does not: `ByShares` hands out the whole total whatever
  the weights, so a money pair on an unbalanced split would read as settled when it plainly
  is not.
  The "X of Y allocated" check now renders in **all three** modes, not just the manual two:
  `QuickAddUiState.allocation` reports an even split as trivially balanced rather than
  returning null, because the design shows that line wherever the user is and moving it in
  and out as the mode changes makes its absence read as a problem. It is null only until
  there is an amount, where the sheet shows "Enter an amount to split." instead.
  The tip preset row went with the trim, so **`tipPercent` is always `0.0` from the UI** —
  the engine still folds a tip into the total, and `onTipPercentChange` and its test still
  work, but nothing on screen sets it.
- **Index 0 is you; `payerIndex` is who paid.** The engine used to conflate the two.
  `SplitResult.ownShareMinor` (formerly `payerShareMinor`) is always the *user's* share,
  because what you ate does not change because someone else reached for the bill, and
  `othersShare` therefore means the same thing whoever paid. What changes is the ledger:
  `SaveSplitTransactionUseCase` writes one `LENT` entry per person when you paid, and a
  single `BORROWED` entry for your own share against the payer when someone else did. It
  **asserts `cardId == null`** in that branch, and `save()` drops the card, because
  `AddTransactionUseCase` would otherwise raise a card balance for money that never left it.
- **Custom categories are a display identity, not a new dimension.** `SpendingCategory`
  is still a fixed enum of twelve and is still the axis every engine works on — reward
  rules and thresholds are keyed by it. A `CustomCategory` carries a **`parent`**
  `SpendingCategory`, and a transaction filed under one stores *both*: `category` = the
  parent, `customCategoryId` = the label. So "Ramen" earns dining's rewards, predicts from
  dining's history and lands in dining's report bucket, and only the UI resolves the
  custom name/icon/colour (via `categoryIdentity()` in `:core:ui`). Without the parent a
  user-invented category would be a hole in every engine rather than a label.
- **Icons, not emoji.** Category colour and glyph are this app's data encoding
  (`CategoryVisuals`), so the chips and pills use the same Material icons every other
  screen does.
- **The Receipt action opens the scanner** (`onOpenScanner` → `:feature:scanner`) rather
  than a receipt editor of its own. Parsing a receipt into line items is that module's
  job, and duplicating it here would be a second implementation of it. The review screen
  there saves its own transaction; it does **not** hand a total back to this form.
- **Tags are orthogonal to the category.** A transaction is filed in exactly one bucket
  because reports have to count it once; `tags` is where the facts that cut across buckets
  go. Stored newline-delimited in one column, so a tag cannot contain a newline.
- **The Location chip is the whole entry point, and `LocationBlock` is the whole of the
  rest.** There are no location dialogs on this screen. The block shows one of two things:

  | `showLocation`, and… | shows |
  |---|---|
  | no fix and no permission (`locationNeedsConsent`), or a `locationPrompt` is set | the consent card: *Not now* / *Allow* |
  | anything else | the place row, nearby chips, the editable `Place` field, the auto-capture switch, *Find me again* |

  **The first save asks, once.** `shouldAskForLocation` gates `save()` and raises
  `LocationConsentSheet` — never on launch, because the first save is the first moment the
  request has any context. It only fires when the platform supports location
  (`locationSupported`, read in `onOpened()`), nothing has been captured, and the user has
  never been asked. Its **Not now writes `LocationCaptureMode.NEVER`**, unlike the location
  block's own Not now, which records nothing: that card was asked for by tapping the chip
  and can be tapped again, whereas a sheet that arrives unasked in front of a save and
  comes back on the next entry is nagging. `LocationCaptureMode` therefore has a third
  value now, and absent still means "never asked".

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

`src/jvmTest/` — `QuickAddViewModelTest.kt`, `AddExpenseScreenTest.kt`,
`CategoryPickerScreenTest.kt` (both Compose UI), and `TestDoubles.kt`. Uses `:core:testing`.

The jvm host resolves to `MATERIAL`, so `theRosterRendersOnCupertinoToo` passes
`platform = DivaPlatform.CUPERTINO` explicitly — without it the Add People sheet would only
ever be covered on half the design system.

`QuickAddViewModelTest` saves through a `saveNow()` helper rather than `save()`: the first
save on a fresh entry raises the location sheet, and every test that is not about that gate
would otherwise have to spell out the two-step answer.

```bash
./gradlew :feature:quickadd:jvmTest
```
