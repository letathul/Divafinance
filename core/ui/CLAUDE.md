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
| `DivaTheme.kt` | The `DivaTheme { }` wrapper. Light/dark `ColorScheme`s selected by `isSystemInDarkTheme()`. Every screen and every UI test must be wrapped in this. |
| `Color.kt` | Named palette — `DivaGold` (primary in both schemes), `DivaBlue`, `DivaDark`, `DivaWhite`, `DivaGray`, `DivaLightGray`, `DivaRed`, `DivaDarkSurface` |
| `Typography.kt` · `Shape.kt` | Material3 `Typography` and `Shapes` overrides |

**`component/`**
| File | What it does |
|------|--------------|
| `DivaButton.kt` | Filled + outlined primary buttons |
| `DivaCard.kt` | The standard surface container |
| `DivaTextField.kt` | Themed text field; supports `visualTransformation` and `keyboardOptions` (used for PIN and amount entry) |
| `CreditCardVisual.kt` | The card-face rendering used in carousels and detail screens |
| `AmountDisplay.kt` | Formatted currency amount with sign/colour treatment |
| `CategoryChip.kt` | `SpendingCategory` chip |
| `DivaBottomNav.kt` | Bottom navigation bar, driven by `MainScreen` in `:composeApp` |
| `LoadingIndicator.kt` | Shared spinner |

**`util/`**
| File | What it does |
|------|--------------|
| `CurrencyFormatter.kt` | `formatCurrency(amount, currency)` → symbol + 2dp, via `toFixed` from `core:common`. Takes `abs(amount)` — **the sign is dropped**, callers render it themselves. Falls back to the currency code when the symbol is unknown. |
| `DateFormatter.kt` | Display formatting for `Instant`/`LocalDate` |

## Conventions / gotchas

- Components are named `Diva*` when they wrap a Material3 primitive. Reach for one of
  these before writing a raw `Button`/`Card`/`TextField` in a feature module.
- Never use `String.format` or `java.text.NumberFormat` here — both are JVM-only and break
  the iOS targets. Use `toFixed` from `core:common`.
- Feature-specific composables belong in that feature's `component/` package, not here.
  A component earns a place in `core:ui` once a second feature needs it.
- The seven currency symbols in `CurrencyFormatter` duplicate `Currency.supported` in
  `core:model` — keep them in sync when adding a currency.

## Tests

None in this module; components are covered indirectly by the feature modules' Compose UI
tests. Note that `diva.kmp.compose` wires the Compose test host into `jvmTest`, so any
test added here goes in `src/jvmTest/`.
