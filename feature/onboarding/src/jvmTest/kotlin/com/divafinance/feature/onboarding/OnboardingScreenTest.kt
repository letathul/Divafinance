package com.divafinance.feature.onboarding

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.divafinance.core.domain.usecase.onboarding.CompleteOnboardingUseCase
import com.divafinance.core.domain.usecase.onboarding.SetPinUseCase
import com.divafinance.core.testing.installTestMainDispatcher
import com.divafinance.core.testing.resetTestMainDispatcher
import com.divafinance.core.testing.fake.FakeAccountRepository
import com.divafinance.core.testing.fake.FakeCardRepository
import com.divafinance.core.testing.fake.FakeLedgerRepository
import com.divafinance.core.testing.fake.FakePersonRepository
import com.divafinance.core.testing.fake.FakeFeedRepository
import com.divafinance.core.testing.fake.FakeReceiptRepository
import com.divafinance.core.testing.fake.FakeRewardRepository
import com.divafinance.core.testing.fake.FakeSettingsRepository
import com.divafinance.core.testing.fake.FakeThresholdRepository
import com.divafinance.core.testing.fake.FakeTransactionRepository
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.feature.onboarding.steps.SecurityStep
import com.divafinance.feature.demo.DemoDataManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OnboardingScreenTest {

    @BeforeTest
    fun setUpMainDispatcher() = installTestMainDispatcher()

    @AfterTest
    fun tearDownMainDispatcher() = resetTestMainDispatcher()

    private val settingsRepo = FakeSettingsRepository()
    private val accountRepo = FakeAccountRepository()
    private val personRepo = FakePersonRepository()

    private fun viewModel() = OnboardingViewModel(
        CompleteOnboardingUseCase(settingsRepo, accountRepo),
        SetPinUseCase(settingsRepo),
        DemoDataManager(
            settingsRepository = settingsRepo,
            accountRepository = accountRepo,
            cardRepository = FakeCardRepository(),
            rewardRepository = FakeRewardRepository(),
            transactionRepository = FakeTransactionRepository(),
            receiptRepository = FakeReceiptRepository(),
            feedRepository = FakeFeedRepository(),
            thresholdRepository = FakeThresholdRepository(),
            personRepository = personRepo,
            ledgerRepository = FakeLedgerRepository(),
        ),
    )

    @Test
    fun startsOnTheWelcomeStep() = runComposeUiTest {
        setContent {
            DivaTheme { OnboardingScreen(viewModel = viewModel()) }
        }
        onNodeWithText("Welcome to Diva Finance").assertIsDisplayed()
    }

    @Test
    fun securityStepKeepsItsActionsWithTheFieldsOnScreen() = runComposeUiTest {
        setContent {
            DivaTheme {
                SecurityStep(
                    pin = "1234",
                    pinConfirm = "1234",
                    pinError = null,
                    isCompleting = false,
                    onPinChanged = {},
                    onPinConfirmChanged = {},
                    onComplete = {},
                    onBack = {},
                )
            }
        }
        // The step scrolls its body and pins its actions, so both the fields and the
        // way forward are on screen at once — the shape the keyboard shrinks into.
        onNodeWithText("Enter PIN").assertIsDisplayed()
        onNodeWithText("Confirm PIN").assertIsDisplayed()
        onNodeWithText("Complete Setup").assertIsDisplayed()
    }
}
