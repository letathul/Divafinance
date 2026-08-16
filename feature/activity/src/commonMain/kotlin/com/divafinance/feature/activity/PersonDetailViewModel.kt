package com.divafinance.feature.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divafinance.core.common.ExpressionEvaluator
import com.divafinance.core.common.roundToCents
import com.divafinance.core.domain.usecase.people.GetPersonDetailUseCase
import com.divafinance.core.domain.usecase.people.PersonDetail
import com.divafinance.core.domain.usecase.people.SettleUpUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PersonDetailUiState(
    val detail: PersonDetail? = null,
    val settleAmount: String = "",
    val isSettling: Boolean = false,
    val error: String? = null,
) {
    /** Reuses the keypad's evaluator, so "20+5" works here too. */
    val settleAmountValue: Double?
        get() = ExpressionEvaluator.evaluate(settleAmount)?.roundToCents()?.takeIf { it > 0.0 }

    val outstanding: Double get() = detail?.balance?.balance ?: 0.0
    val canSettle: Boolean
        get() = settleAmountValue != null && !isSettling && detail?.balance?.isSettled == false
}

class PersonDetailViewModel(
    private val personId: String,
    getPersonDetailUseCase: GetPersonDetailUseCase,
    private val settleUpUseCase: SettleUpUseCase,
) : ViewModel() {

    private val local = MutableStateFlow(PersonDetailUiState())

    val uiState: StateFlow<PersonDetailUiState> = combine(
        local,
        getPersonDetailUseCase(personId),
    ) { state, detail ->
        state.copy(detail = detail)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PersonDetailUiState())

    fun onSettleAmountChange(value: String) =
        local.update { it.copy(settleAmount = value, error = null) }

    /** Pre-fills the field with everything outstanding, for the common "all of it" case. */
    fun onSettleAll() {
        val outstanding = uiState.value.detail?.balance?.balance ?: return
        local.update { it.copy(settleAmount = kotlin.math.abs(outstanding).toString(), error = null) }
    }

    fun onSettle() {
        val amount = uiState.value.settleAmountValue
        if (amount == null) {
            local.update { it.copy(error = "Enter an amount greater than zero") }
            return
        }
        if (local.value.isSettling) return

        viewModelScope.launch {
            local.update { it.copy(isSettling = true, error = null) }
            val recorded = runCatching { settleUpUseCase(personId, amount) }

            local.update {
                when {
                    recorded.isFailure -> it.copy(
                        isSettling = false,
                        error = recorded.exceptionOrNull()?.message ?: "Couldn't record that",
                    )
                    // False means the balance was already clear; saying so beats a silent no-op.
                    recorded.getOrNull() == false -> it.copy(
                        isSettling = false,
                        error = "Nothing left to settle",
                    )
                    else -> it.copy(isSettling = false, settleAmount = "")
                }
            }
        }
    }
}
