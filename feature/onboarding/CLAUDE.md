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
| `steps/WelcomeStep.kt` | Intro |
| `steps/CurrencyStep.kt` | Picks from `Currency.supported` (7 options) |
| `steps/LocationStep.kt` | Default location for transaction tagging |
| `steps/AccountSetupStep.kt` | First account: name + `AccountType` |
| `steps/CardSetupStep.kt` | First card: name + `CardNetwork` |
| `steps/SecurityStep.kt` | PIN + confirmation. Validation error surfaces as `state.pinError`. |
| `steps/DemoStep.kt` | Offers to seed demo data via `DemoDataManager`; failures surface as `state.demoError` |

## Conventions / gotchas

- **Adding a step is two edits:** add the constant to `OnboardingStep` *in the right
  position* (order is the enum's declaration order) and add the branch in
  `OnboardingScreen`. Any new field goes on the single `OnboardingState`.
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
