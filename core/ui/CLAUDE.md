# core/ui

**Purpose:** The design system. Theme (colors, typography, shapes), the shared Compose
components every feature builds screens out of, and display formatters. Contains no
screens, no ViewModels, and no business logic.

**Gradle:** `:core:ui` · `diva.kmp.compose`
**Depends on:** `:core:model` (`api` — components take model/enum types directly),
`:core:common`
**Depended on by:** all 13 `feature/*` modules and all 4 `dynamic/*` modules

## Key files

`src/commonMain/kotlin/com/divafinance/core/ui/`

**`theme/`**
| File | What it does |
|------|--------------|
| `DivaTheme.kt` | `DivaTheme(mode, accent) { }`. `ThemeMode` (LIGHT/DARK/SYSTEM) and `AccentTheme` (SUNSET/OCEAN/MONO). Fills **every** M3 slot. Every screen and every UI test must be wrapped in this — the token `CompositionLocal` throws otherwise. |
| `Tokens.kt` | `DivaTokens` + the `diva` accessor: `muted`, `fgHair`, `negative`, `positive`, `sweep`, `categoryColor()`. Also `Pill` and the `Space` scale. |
| `Color.kt` | The two ramps (dark + light), the 12-hue category ramp, and `AccentTheme` |
| `Typography.kt` · `Shape.kt` | Material3 overrides, plus `NumericStyle` — tabular mono, used for every figure |

**`component/`**
| File | What it does |
|------|--------------|
| `DivaButton.kt` | Filled + outlined primary buttons, pill-shaped |
| `DivaCard.kt` | The standard surface: tonal fill, large corner, 12% hairline border, **no elevation** |
| `GlassSurface.kt` | Translucent capsule with a specular top edge — the tab bar and icon buttons |
| `Foundations.kt` | `Meta`, `SectionHeader`, `SegmentedControl`, `BudgetTrack`, `StatPill`, `Numeric`, `Hairline` |
| `CategoryVisuals.kt` | `SpendingCategory.color` / `.icon`, `CategoryTile`, `CategoryDot`, `CategoryPickerItem` |
| `LedgerRow.kt` | `DayHeader` + `TransactionRow` — the ledger's two building blocks |
| `MomentCard.kt` | The feed's full-width "post" for a split or notable spend |
| `Avatar.kt` | `Avatar`, `AvatarRing` (sweep-ringed), `initialsOf()` |
| `FloatingTabBar.kt` | The shell's glass capsule: two tabs + the centre compose button |
| `CalculatorKeypad.kt` | The 4×4 arithmetic pad. Visible labels are typographic (`÷ × − ⌫`), `contentDescription` is spelled out — tests select on the descriptions. |
| `chart/` | `ChartCanvas`, `SpendingPieChart`, `ThresholdBarChart`, `TrendLineChart` over neutral `ChartSlice`/`ChartPoint`/`ChartBar` types |
| `DivaTextField.kt` | Themed text field; supports `visualTransformation` and `keyboardOptions` (used for PIN and amount entry) |
| `CreditCardVisual.kt` | The card-face rendering used in carousels and detail screens |
| `AmountDisplay.kt` | Formatted currency amount with sign/colour treatment |
| `CategoryChip.kt` | `SpendingCategory` chip |
| `LoadingIndicator.kt` | Shared spinner |

**`util/`**
| File | What it does |
|------|--------------|
| `CurrencyFormatter.kt` | `formatCurrency(amount, currency)` → symbol + 2dp, via `toFixed` from `core:common`. Takes `abs(amount)` — **the sign is dropped**, callers render it themselves. Falls back to the currency code when the symbol is unknown. |
| `DateFormatter.kt` | Display formatting for `Instant`/`LocalDate` |

## Conventions / gotchas

- Components are named `Diva*` when they wrap a Material3 primitive. Reach for one of
  these before writing a raw `Button`/`Card`/`TextField` in a feature module.
- **The accent is rationed**: `primary` is the neutral foreground, so solid buttons are
  never gradient-tinted. The sweep is reachable only as `diva.sweep`, which keeps it to
  the compose button and one earned moment per screen.
- The category ramp is data encoding, not decoration — tiles, dots and chart series only,
  never chrome.
- Separation is tone plus a 12% hairline, never elevation: a shadow is invisible on the
  dark canvas and reads as a box on the light one.
- There is **no backdrop blur** on any Compose target. `GlassSurface` ships translucency
  plus a specular edge, which does most of the perceptual work.
- Never use `String.format` or `java.text.NumberFormat` here — both are JVM-only and break
  the iOS targets. Use `toFixed` from `core:common`.
- Feature-specific composables belong in that feature's `component/` package, not here.
  A component earns a place in `core:ui` once a second feature needs it.
- The seven currency symbols in `CurrencyFormatter` duplicate `Currency.supported` in
  `core:model` — keep them in sync when adding a currency.
- No custom font is bundled, so text renders in the platform face. Figures are the
  exception and must use `NumericStyle` so columns of money stay aligned.

## Tests

None in this module; components are covered indirectly by the feature modules' Compose UI
tests. Note that `diva.kmp.compose` wires the Compose test host into `jvmTest`, so any
test added here goes in `src/jvmTest/`.
