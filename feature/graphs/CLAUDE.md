# feature/graphs

**Purpose:** Financial charts — category spending, per-category threshold progress, and
month-over-month trends — plus the screen for configuring those thresholds. Charts are
drawn with raw Compose `Canvas`; there is no charting library.

**Gradle:** `:feature:graphs` · `diva.kmp.compose`
**Depends on:** `:core:model`, `:core:domain`, `:core:ui`, `:core:common`

## Key files

`src/commonMain/kotlin/com/divafinance/feature/graphs/`

| File | What it does |
|------|--------------|
| `GraphsViewModel.kt` | `GraphsUiState` (selected tab, selected period, the three datasets, total, loading). `ChartTab` = `PIE` ("Spending") / `BAR` ("Thresholds") / `LINE` ("Trends"); `TimePeriod` = `1M/3M/6M/1Y` with a `months` value used to compute the range. Also owns `ThresholdFormState`. Uses `GetSpendingByCategoryUseCase`, `GetThresholdGraphDataUseCase`, `ConfigureThresholdUseCase`. |
| `GraphsDashboardScreen.kt` | `DivaRoutes.GRAPHS` — tab host |
| `ThresholdConfigScreen.kt` | `DivaRoutes.THRESHOLD_CONFIG` (`graphs/thresholds`) — add/edit a category threshold percentage |
| `component/ChartCanvas.kt` | Thin `Canvas` wrapper with a `chartHeight` default of 200.dp. **Every chart builds on this** rather than calling `Canvas` directly. |
| `component/SpendingPieChart.kt` | Renders `List<SpendingSlice>` (category, amount, percent) |
| `component/ThresholdBarChart.kt` | Renders `List<CategoryThresholdData>` from the domain layer |
| `component/TrendLineChart.kt` | Renders `List<TrendPoint>` (month label, amount) |

## Conventions / gotchas

- The view-model-local types (`SpendingSlice`, `TrendPoint`) exist so charts take flat
  render-ready shapes. `CategoryThresholdData` comes straight from
  `core:domain`'s `GetThresholdGraphDataUseCase` and is passed through unchanged.
- **This state is loaded imperatively, not derived.** Unlike most features here,
  `GraphsUiState` is a single `MutableStateFlow` refreshed on tab/period change, with an
  explicit `isLoading` flag — changing the period has to trigger a reload.
- New charts go through `ChartCanvas`. Colours come from `MaterialTheme.colorScheme` via
  `DivaTheme`, not hardcoded — the charts must work in both light and dark.
- Threshold percentages are stored as strings in `ThresholdFormState` and parsed on save.

## Tests

`src/commonTest/` (runs on the JVM host) — `GraphsViewModelTest.kt` and
`GraphsDashboardScreenTest.kt` (Compose UI).

```bash
./gradlew :feature:graphs:jvmTest
```
