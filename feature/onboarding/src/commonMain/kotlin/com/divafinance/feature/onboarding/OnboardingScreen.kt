package com.divafinance.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.divafinance.feature.onboarding.steps.AccountSetupStep
import com.divafinance.feature.onboarding.steps.CardSetupStep
import com.divafinance.feature.onboarding.steps.CurrencyStep
import com.divafinance.feature.onboarding.steps.DemoStep
import com.divafinance.feature.onboarding.steps.LocationStep
import com.divafinance.feature.onboarding.steps.SecurityStep
import com.divafinance.feature.onboarding.steps.WelcomeStep
import com.divafinance.core.ui.component.StatusBarSpacer
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit = {},
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    if (state.isCompleted) {
        onOnboardingComplete()
        return
    }

    val stepIndex = OnboardingStep.entries.indexOf(state.currentStep)
    val progress = (stepIndex + 1).toFloat() / OnboardingStep.entries.size

    Column(modifier = Modifier.fillMaxSize()) {
        StatusBarSpacer()

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        AnimatedContent(
            targetState = state.currentStep,
            transitionSpec = {
                val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                slideInHorizontally { it * direction } togetherWith
                    slideOutHorizontally { -it * direction }
            },
            modifier = Modifier.weight(1f),
        ) { step ->
            when (step) {
                OnboardingStep.WELCOME -> WelcomeStep(
                    onNext = viewModel::nextStep,
                )

                OnboardingStep.CURRENCY -> CurrencyStep(
                    selectedCurrency = state.selectedCurrency,
                    onCurrencySelected = viewModel::updateCurrency,
                    onNext = viewModel::nextStep,
                    onBack = viewModel::previousStep,
                )

                OnboardingStep.LOCATION -> LocationStep(
                    location = state.defaultLocation,
                    onLocationChanged = viewModel::updateLocation,
                    onNext = viewModel::nextStep,
                    onBack = viewModel::previousStep,
                )

                OnboardingStep.ACCOUNT_SETUP -> AccountSetupStep(
                    accountName = state.accountName,
                    accountType = state.accountType,
                    onAccountNameChanged = viewModel::updateAccountName,
                    onAccountTypeChanged = viewModel::updateAccountType,
                    onNext = viewModel::nextStep,
                    onBack = viewModel::previousStep,
                )

                OnboardingStep.CARD_SETUP -> CardSetupStep(
                    cardName = state.cardName,
                    cardNetwork = state.cardNetwork,
                    onCardNameChanged = viewModel::updateCardName,
                    onCardNetworkChanged = viewModel::updateCardNetwork,
                    onNext = viewModel::nextStep,
                    onBack = viewModel::previousStep,
                )

                OnboardingStep.SECURITY -> SecurityStep(
                    pin = state.pin,
                    pinConfirm = state.pinConfirm,
                    pinError = state.pinError,
                    isCompleting = state.isCompleting,
                    onPinChanged = viewModel::updatePin,
                    onPinConfirmChanged = viewModel::updatePinConfirm,
                    onComplete = viewModel::submitPin,
                    onBack = viewModel::previousStep,
                )

                OnboardingStep.DEMO -> DemoStep(
                    isCompleting = state.isCompleting,
                    onUseDemo = { viewModel.finish(withDemo = true) },
                    onSkipDemo = { viewModel.finish(withDemo = false) },
                    onBack = viewModel::previousStep,
                )
            }
        }
    }
}
