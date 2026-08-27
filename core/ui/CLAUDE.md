# core/ui

**Purpose:** The design system. Theme (colors, typography, shapes), the shared Compose
components every feature builds screens out of, and display formatters. Contains no
screens, no ViewModels, and no business logic.

**It renders two languages from one source.** Material 3 on Android, Apple HIG on iOS —
screens are authored once in `commonMain` and the divergence is resolved here. Most of it
is tokens; the handful of components that genuinely branch live in `adaptive/`.

**Gradle:** `:core:ui` · `diva.kmp.compose`
**Depends on:** `:core:model` (`api` — components take model/enum types directly),
`:core:common`
**Depended on by:** all 13 `feature/*` modules and all 4 `dynamic/*` modules

## Key files

`src/commonMain/kotlin/com/divafinance/core/ui/`

**`theme/`**
| File | What it does |
|------|--------------|
| `Platform.kt` | `DivaPlatform` (MATERIAL/CUPERTINO), `expect fun currentPlatform()`, `LocalDivaPlatform`, and the `isCupertino` accessor. Actuals in `jvmSharedMain` (Android + the JVM test host → MATERIAL) and `iosMain` (→ CUPERTINO). |
| `DivaTheme.kt` | `DivaTheme(mode, accent, platform) { }`. `ThemeMode` (LIGHT/DARK/SYSTEM). Two schemes, both filling **every** M3 slot. Every screen and every UI test must be wrapped in this — the token `CompositionLocal` throws otherwise. |
| `Tokens.kt` | `DivaTokens` + the `diva` accessor: `accent`, `brand`, `muted`, `fgHair`, `separator`, `canvas`, `card`, `barFill`, `keyFill`, `hairline`, `cardRadius`, `rowMinHeight`, `negative`, `positive`, `categoryColor()`. Also `Pill` and the `Space` scale. |
| `Color.kt` | The two diva ramps, the Cupertino system palette, the 12-hue category ramp, the `DivaBrand*` placeholders, and `AccentTheme` (seven single-colour accents, defaulting to `EMERALD` — the teal the add-transaction design was drawn in; **the stored preference is the entry name**, so renaming one re-themes existing installs, though adding one is safe) |
| `Typography.kt` · `Shape.kt` | Per-platform `divaTypography(platform)` / `divaShapes(platform)`, plus `NumericStyle` — tabular figures, used for every figure in the app |

**`adaptive/`** — the components that branch on `DivaPlatform`.
| File | What it does |
|------|--------------|
| `DivaScaffold.kt` | `DivaScaffold` / `DivaScaffoldColumn` / `DivaListScaffold` / `DivaLargeTitle`. The shared screen chrome — a drop-in for `Scaffold(topBar = TopAppBar)`. Cupertino draws a 44dp translucent nav bar; Material an M3 `TopAppBar`. |
| `GroupedList.kt` | `DivaGroupedSection`, `DivaListRow`, `DivaListAction`, `DivaRowDivider`, `DivaGroupHeader`, `DivaCheckCircle` — the grouped inset list |
| `DivaTabBar.kt` | `DivaTab`, `DivaTabBar`, `DivaBottomBar` (the named geometry), and **`divaContentPadding()`** — the bottom inset every screen behind the bar leaves |
| `DivaSwitch.kt` | M3 `Switch` on Material; a hand-drawn 51×31 HIG control on Cupertino, with real `Role.Switch` semantics |
| `DivaChip.kt` | `DivaChip` / `DivaChipStyle`. Carries `onLongClick` — the add-expense screen needs it |
| `DivaBottomSheet.kt` | `DivaBottomSheet` + `DivaSheetHandle`. Both platforms use M3's `ModalBottomSheet` for the scrim, drag and predictive-back behaviour and diverge only on corner and grabber. It deliberately does **not** expose a `SheetState` parameter — that type is experimental, and taking it would push the opt-in onto every screen that opens a sheet |

**`component/`**
| File | What it does |
|------|--------------|
| `DivaButton.kt` | Filled + outlined primary buttons. Pill on Material, 14dp rounded rect on Cupertino |
| `DivaCard.kt` | The standard surface: card fill, platform corner, **no elevation**. Material adds a 12% hairline border; Cupertino does not need one on the grouped canvas |
| `GlassSurface.kt` | Translucent capsule with a specular top edge — the feed's icon buttons and `MomentCard`'s badges |
| `Foundations.kt` | `Meta`, `SectionHeader` (delegates to `DivaGroupHeader`), `SegmentedControl` (optional `onLongPress` per segment, for offering more than the visible options), `BudgetTrack`, `StatPill`, `Numeric`, `Hairline` |
| `CategoryVisuals.kt` | `SpendingCategory.color` / `.icon`, `CategoryTile`, `CategoryDot`, `CategoryPickerItem`. The tile and the picker item each take **either** a `SpendingCategory` or a resolved `CategoryIdentity`, so a user's own category renders through the same code path as the built-in twelve |
| `CategoryIdentity.kt` | `CategoryIdentity` (label + icon + colour), `categoryIdentity(category, custom)`, the `CategoryIconKeys` registry and `categoryIconForKey`, `parseHexColor`, and `List<CustomCategory>.byId`. A custom category falls back to its parent **per field**, so an unrecognised icon key still keeps its own name and colour |
| `DivaCalendar.kt` | A month grid and nothing else — no sheet, no confirm button, no state beyond which month is shown. Selection fires on tap, which is what lets the date sheet close in one gesture. `maxDate` greys out later days rather than hiding them, so the shape of the month stays readable |
| `LedgerRow.kt` | `DayHeader` + `TransactionRow` — the ledger's two building blocks |
| `MomentCard.kt` | The feed's full-width "post" for a split or notable spend |
| `Avatar.kt` | `Avatar`, `AvatarRing` (accent-ringed), `initialsOf()` |
| `PersonVisuals.kt` | `personColor(name, colorHex)`, the six-swatch `PersonPalette` the add-person form offers, and `Color.toHex()`. The stored colour wins; absent falls back to a stable hash of the name, so a person written before `Person.colorHex` existed keeps the avatar they always had. The palette is deliberately **not** the category ramp — those twelve hues encode *what* was spent, and a person wearing one would make the same colour mean two things on one screen |
| `CalculatorKeypad.kt` | The 4×4 arithmetic pad — circular keys on Cupertino, rounded rects on Material. `extended = true` swaps in the 5×4 pad the add-expense amount sheet uses, adding `( ) C` and `=` (`onGroup` / `onClear` / `onEquals`). **The grid is uniform and spans the pad on both platforms**: the four columns divide the full width, and key *height* follows that width only until `MaxKeyHeight` (68dp), so five rows still fit under a display and a button. A key wider than it is tall comes out as a stadium on Cupertino, because `CircleShape` takes its 50% corner off the smaller side. Every press ticks (`HapticFeedbackType.KeyboardTap`) and dips the key; **holding ⌫ clears**; `onEquals` returns whether it folded, so `=` on an unfinished expression answers `Reject` rather than nothing at all. Visible labels are typographic (`÷ × − ⌫`), `contentDescription` is spelled out (`Open bracket`, `Clear`, `Equals`, …) — tests select on the descriptions, and the semantics sit on the *clickable* node so the node found by description is the one that acts. |
| `chart/` | `SpendingPieChart`, `ThresholdBarChart`, `TrendLineChart`, and `Sparkline` (the bare inline line, no axes or empty state) over neutral `ChartSlice`/`ChartPoint`/`ChartBar` types |
| `DivaLogo.kt` | The brand wordmark, drawn in type. No asset pipeline — replacing it is this file plus the two `DivaBrand*` colours |
| `DivaTextField.kt` | Themed text field; supports `visualTransformation` and `keyboardOptions` (used for PIN and amount entry) |
| `CreditCardVisual.kt` | The card-face rendering used in carousels and detail screens |
| `AmountDisplay.kt` | Formatted currency amount with sign/colour treatment |
| `CategoryChip.kt` | `SpendingCategory` chip |
| `LoadingIndicator.kt` | Shared spinner |

**`util/`**
| File | What it does |
|------|--------------|
| `CurrencyFormatter.kt` | `formatCurrency(amount, currency)` → symbol + 2dp, via `toFixed` from `core:common`. Takes `abs(amount)` — **the sign is dropped**, callers render it themselves. Falls back to the currency code when the symbol is unknown. |
| `DateFormatter.kt` | Display formatting for `Instant`/`LocalDate`, plus `formatShortDate` (`"Aug 21"`) for chips and captions |

## Conventions / gotchas

- Components are named `Diva*` when they wrap a Material3 primitive. Reach for one of
  these before writing a raw `Button`/`Card`/`TextField` in a feature module.
- **`primary` means "the tinted affordance", and it resolves differently per platform**:
  the neutral foreground on Material (which is what has kept the accent to twice a
  screen), the user's accent on Cupertino (because HIG tints every button and link).
  Route anything tinted through `colorScheme.primary` and it comes out right on both
  without branching. Reserve `diva.accent` for the few things that stay branded on *both*
  — the story rings, the avatar ring, the raised compose button, and the selected
  segment of `SegmentedControl`, where the selection *is* the control and a neutral
  fill reads as disabled beside the unselected segments.
- **The sweep gradient is gone**, along with `AccentTheme.colors`, the `DivaShapes` /
  `DivaTypography` aliases and the unused `chart/ChartCanvas.kt`. `AccentTheme` is one
  colour per scheme now. Use `diva.accent`, or `colorScheme.primary` for anything that
  should stay neutral on Android.
- The category ramp is data encoding, not decoration — tiles, dots and chart series only,
  never chrome.
- Separation is tone plus a hairline, never elevation: a shadow is invisible on the dark
  canvas and reads as a box on the light one. Note `diva.fgHair` (the 12% rule around a
  card) and `diva.separator` (the rule *between* rows in a grouped list) are different
  tokens — on Cupertino the latter is a real system colour at 0.5dp.
- There is **no backdrop blur** on any Compose target. `GlassSurface`, the tab bar and
  the Cupertino nav bar all ship translucency plus a hairline edge instead, which does
  most of the perceptual work. Do not add a dependency chasing real blur.
- Compose has **no continuous-corner (squircle) shape**, so every Cupertino corner is a
  circular-arc approximation. Visible side by side with a real iOS app; accepted.
- Never use `String.format` or `java.text.NumberFormat` here — both are JVM-only and break
  the iOS targets. Use `toFixed` from `core:common`.
- Feature-specific composables belong in that feature's `component/` package, not here.
  A component earns a place in `core:ui` once a second feature needs it.
- The seven currency symbols in `CurrencyFormatter` duplicate `Currency.supported` in
  `core:model` — keep them in sync when adding a currency.
- No custom font is bundled, so text renders in the platform face. Figures are the
  exception and must use `NumericStyle` so columns of money stay aligned. **`NumericStyle`
  is a composable getter, not a `val`** — `FontFamily.Monospace` resolves to Courier on
  iOS, so the Cupertino branch uses SF with the `tnum` feature instead. Read it inside a
  composable, which every existing call site already does.
- **A new adaptive component gets a test that runs both branches.** The `jvm` host is
  `MATERIAL`, so a test that does not pass `platform = CUPERTINO` covers half the system.
  See `src/jvmTest/.../AdaptiveComponentsTest.kt` and `component/CalculatorKeypadTest.kt`.

## Tests

`src/jvmTest/.../adaptive/AdaptiveComponentsTest.kt` and
`src/jvmTest/.../component/CalculatorKeypadTest.kt` — every case in both runs once per
`DivaPlatform`. Everything else is covered indirectly by the feature modules' Compose UI
tests. `diva.kmp.compose` wires the Compose test host into `jvmTest`, so any test added
here goes in `src/jvmTest/`.

```bash
./gradlew :core:ui:jvmTest
```
