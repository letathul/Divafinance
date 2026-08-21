# feature/onboarding

**Purpose:** The 7-step first-run wizard: currency, default location, first account, first
card, PIN, and an optional demo-data offer. Writing `UserSettings.onboardingComplete` here
is what flips the app from the wizard to the dashboard.

**Gradle:** `:feature:onboarding` · `diva.kmp.compose`
**Depends on:** `:core:domain`, `:core:ui`, `:core:common`, **`:feature:demo`** (the demo
is offered as the final step)

## Key files

`src/commonMain/kotlin/com/divafinance/feature/onboarding/`

| File | What it does |
|------|--------------|
| `OnboardingViewModel.kt` | `OnboardingStep` enum (`WELCOME, CURRENCY, LOCATION, ACCOUNT_SETUP, CARD_SETUP, SECURITY, DEMO`) + a single `OnboardingState` data class holding every field across all steps. `nextStep()`/`previousStep()` walk `OnboardingStep.entries` by index. Takes `CompleteOnboardingUseCase`, `SetPinUseCase`, and `DemoDataManager`. |
| `OnboardingScreen.kt` | Host: `AnimatedContent` with slide transitions between steps, plus the progress indicator |
| `steps/OnboardingStepLayout.kt` | The frame the steps share: title + subtitle, a scrolling body, an action block pinned to the bottom, and tap-outside-to-dismiss |
| `steps/WelcomeStep.kt` | Intro (the one step that does not use the layout — it is centred, not top-aligned) |
| `steps/CurrencyStep.kt` | Picks from `Currency.supported` (7 options) |
| `steps/LocationStep.kt` | Default location for transaction tagging |
| `steps/AccountSetupStep.kt` | First account: name + `AccountType` |
| `steps/CardSetupStep.kt` | First card: name + `CardNetwork` |
| `steps/SecurityStep.kt` | PIN + confirmation. Validation error surfaces as `state.pinError`. |
| `steps/DemoStep.kt` | Offers to seed demo data via `DemoDataManager`; failures surface as `state.demoError` |

## Conventions / gotchas

- **Adding a step is two edits:** add the constant to `OnboardingStep` *in the right
  position* (order is the enum's declaration order) and add the branch in
  `OnboardingScreen`. Any new field goes on the single `OnboardingState`. Build the step
  itself out of `OnboardingStepLayout` rather than a bare `Column`.
- **Keyboard handling lives in exactly two places.** `OnboardingScreen` applies
  `imePadding()` once at the root, so the whole wizard — progress bar included — shortens
  to the space above the keyboard; `OnboardingStepLayout` then scrolls the body and keeps
  the actions on the bottom edge of whatever is left. A step that lays itself out with
  `Spacer(Modifier.weight(1f))` in a wrap-content `Column` puts its buttons back under
  the keyboard, which is what made SECURITY unpassable on iOS.
- Steps must not nest a lazy list in the body — it is already inside a `verticalScroll`.
  `CurrencyStep` renders its seven options as `chunked(2)` rows for that reason.
- iOS has no return key on a numeric keypad, so the PIN step cannot rely on an IME action
  alone to dismiss: `OnboardingStepLayout`'s tap-to-dismiss is the way out.
- The PIN is never stored in plaintext — `SetPinUseCase` generates a salt and a PBKDF2
  hash via `SecurityUtils`, and both go into `UserSettings`.
- Navigation out of onboarding is owned by `composeApp`: `DivaNavHost` pops
  `DivaRoutes.ONBOARDING` inclusively on completion, and `MainScreen` holds the guard that
  routes first-run users here. Don't add a second guard in this module.
- `DemoStep` can fail (the demo is a dynamic feature that may need downloading) — treat
  demo failure as non-fatal, onboarding must still complete.

## Tests

`src/jvmTest/OnboardingScreenTest.kt` — Compose UI test. Uses `:core:testing`.

```bash
./gradlew :feature:onboarding:jvmTest
```
