package com.divafinance.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divafinance.core.domain.usecase.onboarding.CompleteOnboardingUseCase
import com.divafinance.core.domain.usecase.onboarding.SetPinUseCase
import com.divafinance.feature.demo.DemoDataManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep {
    WELCOME, CURRENCY, LOCATION, ACCOUNT_SETUP, CARD_SETUP, SECURITY, DEMO
}

data class OnboardingState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val selectedCurrency: String = "USD",
    val defaultLocation: String = "",
    val accountName: String = "",
    val accountType: String = "CHECKING",
    val cardName: String = "",
    val cardNetwork: String = "VISA",
    val pin: String = "",
    val pinConfirm: String = "",
    val pinError: String? = null,
    val isCompleting: Boolean = false,
    val isCompleted: Boolean = false,
    val demoError: String? = null,
)

class OnboardingViewModel(
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
    private val setPinUseCase: SetPinUseCase,
    private val demoDataManager: DemoDataManager,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val steps = OnboardingStep.entries

    fun nextStep() {
        _state.update { current ->
            val idx = steps.indexOf(current.currentStep)
            if (idx < steps.lastIndex) {
                current.copy(currentStep = steps[idx + 1], pinError = null)
            } else {
                current
            }
        }
    }

    fun previousStep() {
        _state.update { current ->
            val idx = steps.indexOf(current.currentStep)
            if (idx > 0) {
                current.copy(currentStep = steps[idx - 1], pinError = null)
            } else {
                current
            }
        }
    }

    fun updateCurrency(currency: String) {
        _state.update { it.copy(selectedCurrency = currency) }
    }

    fun updateLocation(location: String) {
        _state.update { it.copy(defaultLocation = location) }
    }

    fun updateAccountName(name: String) {
        _state.update { it.copy(accountName = name) }
    }

    fun updateAccountType(type: String) {
        _state.update { it.copy(accountType = type) }
    }

    fun updateCardName(name: String) {
        _state.update { it.copy(cardName = name) }
    }

    fun updateCardNetwork(network: String) {
        _state.update { it.copy(cardNetwork = network) }
    }

    fun updatePin(pin: String) {
        _state.update { it.copy(pin = pin, pinError = null) }
    }

    fun updatePinConfirm(confirm: String) {
        _state.update { it.copy(pinConfirm = confirm, pinError = null) }
    }

    /**
     * Validates the PIN and moves on to the demo offer. Onboarding is only actually
     * committed once the demo question is answered, in [finish].
     */
    fun submitPin() {
        val current = _state.value
        if (current.pin.length < 4) {
            _state.update { it.copy(pinError = "PIN must be at least 4 digits") }
            return
        }
        if (current.pin != current.pinConfirm) {
            _state.update { it.copy(pinError = "PINs do not match") }
            return
        }
        nextStep()
    }

    /**
     * Commits onboarding and answers the one-time demo offer.
     *
     * If [withDemo] is true but the demo module can't be delivered, onboarding is still
     * completed and the offer is left open, so the user can take it later rather than
     * losing it to a transient download failure.
     */
    fun finish(withDemo: Boolean) {
        val current = _state.value
        if (current.isCompleting) return

        viewModelScope.launch {
            _state.update { it.copy(isCompleting = true, demoError = null) }

            setPinUseCase(current.pin)
            completeOnboardingUseCase(
                baseCurrency = current.selectedCurrency,
                defaultLocation = current.defaultLocation.ifBlank { null },
                accountName = current.accountName,
                accountType = current.accountType,
            )

            val demoFailed = if (withDemo) {
                !demoDataManager.seed(current.selectedCurrency)
            } else {
                demoDataManager.decline()
                false
            }

            _state.update {
                it.copy(
                    isCompleting = false,
                    isCompleted = true,
                    demoError = if (demoFailed) "Couldn't load the demo data." else null,
                )
            }
        }
    }

    val stepProgress: Float
        get() {
            val idx = steps.indexOf(_state.value.currentStep)
            return (idx + 1).toFloat() / steps.size
        }
}
