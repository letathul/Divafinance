package com.divafinance.feature.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divafinance.core.common.UuidGenerator
import com.divafinance.core.domain.engine.CardRecommendation
import com.divafinance.core.domain.usecase.cards.AddCardUseCase
import com.divafinance.core.domain.usecase.cards.GetAllCardsUseCase
import com.divafinance.core.domain.usecase.cards.GetBestCardForCategoryUseCase
import com.divafinance.core.domain.usecase.cards.UpdateCardUseCase
import com.divafinance.core.model.CardRewardRule
import com.divafinance.core.model.CreditCard
import com.divafinance.core.model.enums.CapPeriod
import com.divafinance.core.model.enums.CardNetwork
import com.divafinance.core.model.enums.RewardType
import com.divafinance.core.model.enums.SpendingCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class CardFormState(
    val name: String = "",
    val lastFour: String = "",
    val network: CardNetwork = CardNetwork.VISA,
    val color: String = "#1E1E2E",
    val creditLimit: String = "",
    val annualFee: String = "",
    val statementDate: String = "",
    val dueDate: String = "",
    val rewardRules: List<RewardRuleFormState> = emptyList(),
    val isSaving: Boolean = false,
    val editingCardId: String? = null,
    val accountId: String = "",
) {
    val isEditing: Boolean get() = editingCardId != null
}

data class RewardRuleFormState(
    val id: String = UuidGenerator.generate(),
    val category: SpendingCategory = SpendingCategory.OTHER,
    val multiplier: String = "",
    val rewardType: RewardType = RewardType.CASHBACK,
    val capAmount: String = "",
    val capPeriod: CapPeriod? = null,
)

data class RecommendationState(
    val selectedCategory: SpendingCategory = SpendingCategory.DINING,
    val amount: String = "100",
    val recommendations: List<CardRecommendation> = emptyList(),
    val isLoading: Boolean = false,
)

class CardsViewModel(
    private val getAllCardsUseCase: GetAllCardsUseCase,
    private val addCardUseCase: AddCardUseCase,
    private val updateCardUseCase: UpdateCardUseCase,
    private val getBestCardForCategoryUseCase: GetBestCardForCategoryUseCase,
) : ViewModel() {

    val cards: StateFlow<List<CreditCard>> = getAllCardsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _formState = MutableStateFlow(CardFormState())
    val formState: StateFlow<CardFormState> = _formState.asStateFlow()

    private val _recommendationState = MutableStateFlow(RecommendationState())
    val recommendationState: StateFlow<RecommendationState> = _recommendationState.asStateFlow()

    fun resetForm() {
        _formState.value = CardFormState()
    }

    fun loadCardForEditing(card: CreditCard) {
        _formState.value = CardFormState(
            name = card.name,
            lastFour = card.lastFour ?: "",
            network = card.network,
            color = card.color,
            creditLimit = if (card.creditLimit > 0) card.creditLimit.toString() else "",
            annualFee = if (card.annualFee > 0) card.annualFee.toString() else "",
            statementDate = card.statementDate?.toString() ?: "",
            dueDate = card.dueDate?.toString() ?: "",
            rewardRules = card.rewardRules.map { rule ->
                RewardRuleFormState(
                    id = rule.id,
                    category = rule.category,
                    multiplier = rule.multiplier.toString(),
                    rewardType = rule.rewardType,
                    capAmount = rule.capAmount?.toString() ?: "",
                    capPeriod = rule.capPeriod,
                )
            },
            editingCardId = card.id,
            accountId = card.accountId,
        )
    }

    fun updateName(name: String) {
        _formState.update { it.copy(name = name) }
    }

    fun updateLastFour(lastFour: String) {
        if (lastFour.length <= 4 && lastFour.all(Char::isDigit)) {
            _formState.update { it.copy(lastFour = lastFour) }
        }
    }

    fun updateNetwork(network: CardNetwork) {
        _formState.update { it.copy(network = network) }
    }

    fun updateColor(color: String) {
        _formState.update { it.copy(color = color) }
    }

    fun updateCreditLimit(limit: String) {
        _formState.update { it.copy(creditLimit = limit) }
    }

    fun updateAnnualFee(fee: String) {
        _formState.update { it.copy(annualFee = fee) }
    }

    fun updateStatementDate(date: String) {
        if (date.isEmpty() || (date.all(Char::isDigit) && (date.toIntOrNull() ?: 0) <= 31)) {
            _formState.update { it.copy(statementDate = date) }
        }
    }

    fun updateDueDate(date: String) {
        if (date.isEmpty() || (date.all(Char::isDigit) && (date.toIntOrNull() ?: 0) <= 31)) {
            _formState.update { it.copy(dueDate = date) }
        }
    }

    fun addRewardRule() {
        _formState.update {
            it.copy(rewardRules = it.rewardRules + RewardRuleFormState())
        }
    }

    fun removeRewardRule(index: Int) {
        _formState.update {
            it.copy(rewardRules = it.rewardRules.toMutableList().apply { removeAt(index) })
        }
    }

    fun updateRewardRule(index: Int, rule: RewardRuleFormState) {
        _formState.update {
            it.copy(rewardRules = it.rewardRules.toMutableList().apply { set(index, rule) })
        }
    }

    fun saveCard(onSuccess: () -> Unit) {
        val form = _formState.value
        if (form.name.isBlank()) return

        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true) }

            val now = Clock.System.now()
            val cardId = form.editingCardId ?: UuidGenerator.generate()

            val rules = form.rewardRules.mapNotNull { ruleForm ->
                val multiplier = ruleForm.multiplier.toDoubleOrNull() ?: return@mapNotNull null
                CardRewardRule(
                    id = ruleForm.id,
                    cardId = cardId,
                    category = ruleForm.category,
                    multiplier = multiplier,
                    rewardType = ruleForm.rewardType,
                    capAmount = ruleForm.capAmount.toDoubleOrNull(),
                    capPeriod = ruleForm.capPeriod,
                )
            }

            val card = CreditCard(
                id = cardId,
                accountId = form.accountId.ifBlank { "default" },
                name = form.name,
                lastFour = form.lastFour.ifBlank { null },
                network = form.network,
                color = form.color,
                creditLimit = form.creditLimit.toDoubleOrNull() ?: 0.0,
                annualFee = form.annualFee.toDoubleOrNull() ?: 0.0,
                statementDate = form.statementDate.toIntOrNull(),
                dueDate = form.dueDate.toIntOrNull(),
                rewardRules = rules,
                createdAt = now,
                updatedAt = now,
            )

            if (form.isEditing) {
                updateCardUseCase(card)
            } else {
                addCardUseCase(card)
            }

            _formState.update { it.copy(isSaving = false) }
            onSuccess()
        }
    }

    fun updateRecommendationCategory(category: SpendingCategory) {
        _recommendationState.update { it.copy(selectedCategory = category) }
    }

    fun updateRecommendationAmount(amount: String) {
        _recommendationState.update { it.copy(amount = amount) }
    }

    fun fetchRecommendations() {
        val state = _recommendationState.value
        val amount = state.amount.toDoubleOrNull() ?: return

        viewModelScope.launch {
            _recommendationState.update { it.copy(isLoading = true) }
            val results = getBestCardForCategoryUseCase(state.selectedCategory, amount)
            _recommendationState.update {
                it.copy(recommendations = results, isLoading = false)
            }
        }
    }
}
