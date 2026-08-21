package com.divafinance.feature.scanner.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divafinance.core.common.UuidGenerator
import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.core.domain.usecase.cards.GetAllCardsUseCase
import com.divafinance.core.domain.usecase.scanner.ConfirmReceiptUseCase
import com.divafinance.core.domain.usecase.scanner.GetReceiptUseCase
import com.divafinance.core.domain.usecase.transactions.PredictCategoryUseCase
import com.divafinance.core.model.CreditCard
import com.divafinance.core.model.Receipt
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.UserSettings
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class ReceiptReviewUiState(
    val isLoading: Boolean = true,
    val receipt: Receipt? = null,
    val merchantName: String = "",
    val amountText: String = "",
    val date: LocalDate? = null,
    val category: SpendingCategory = SpendingCategory.OTHER,
    val suggestedCategories: List<SpendingCategory> = emptyList(),
    val cards: List<CreditCard> = emptyList(),
    val selectedCardId: String? = null,
    val currency: String = "USD",
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    /** Null unless the field holds a usable positive figure — this is what gates saving. */
    val amount: Double?
        get() = amountText.replace(",", ".").trim().toDoubleOrNull()?.takeIf { it > 0.0 }

    val canSave: Boolean get() = amount != null && !isSaving && !isLoading
}

/**
 * The review step between a scan and a transaction.
 *
 * Its only input is [receiptId] and every field is seeded from the stored row, which is what
 * makes resuming a pending receipt from the scan history identical to reviewing a fresh scan —
 * and why a review survives process death without any draft state.
 */
class ReceiptReviewViewModel(
    private val receiptId: String,
    private val getReceipt: GetReceiptUseCase,
    private val confirmReceipt: ConfirmReceiptUseCase,
    private val predictCategory: PredictCategoryUseCase,
    private val getAllCards: GetAllCardsUseCase,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiptReviewUiState())
    val uiState: StateFlow<ReceiptReviewUiState> = _uiState.asStateFlow()

    /** The id of the transaction just created, for the screen to navigate away on. */
    private val _saved = MutableStateFlow<String?>(null)
    val saved: StateFlow<String?> = _saved.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val receipt = getReceipt(receiptId)
            if (receipt == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "That receipt is no longer available",
                )
                return@launch
            }

            val cards = runCatching { getAllCards().first() }.getOrDefault(emptyList())
            val suggestions = predictCategory(
                merchantName = receipt.merchantName,
                amount = receipt.totalAmount,
                limit = SUGGESTED_CATEGORY_COUNT,
            )

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                receipt = receipt,
                merchantName = receipt.merchantName.orEmpty(),
                amountText = receipt.totalAmount?.toString().orEmpty(),
                date = receipt.date ?: today(),
                category = suggestions.firstOrNull() ?: SpendingCategory.OTHER,
                suggestedCategories = suggestions,
                cards = cards,
                selectedCardId = setting(UserSettings.KEY_DEFAULT_CARD_ID)
                    ?.takeIf { id -> cards.any { it.id == id } },
                currency = setting(UserSettings.KEY_BASE_CURRENCY) ?: "USD",
            )
        }
    }

    fun onMerchantChanged(value: String) {
        _uiState.value = _uiState.value.copy(merchantName = value)
    }

    fun onAmountChanged(value: String) {
        _uiState.value = _uiState.value.copy(amountText = value)
    }

    fun onDateChanged(value: LocalDate) {
        _uiState.value = _uiState.value.copy(date = value)
    }

    fun onCategorySelected(category: SpendingCategory) {
        _uiState.value = _uiState.value.copy(category = category)
    }

    fun onCardSelected(cardId: String?) {
        _uiState.value = _uiState.value.copy(selectedCardId = cardId)
    }

    fun save() {
        val state = _uiState.value
        val receipt = state.receipt ?: return
        val amount = state.amount ?: return
        if (state.isSaving) return

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            try {
                // Minted here rather than returned by the insert, so both sides of the
                // receipt↔transaction link can be written with the same id.
                val transaction = Transaction(
                    id = UuidGenerator.generate(),
                    accountId = setting(UserSettings.KEY_DEFAULT_ACCOUNT_ID) ?: DEFAULT_ACCOUNT_ID,
                    cardId = state.selectedCardId,
                    amount = amount,
                    currency = state.currency,
                    category = state.category,
                    merchantName = state.merchantName.ifBlank { null },
                    date = state.date ?: today(),
                    type = TransactionType.DEBIT,
                    receiptId = receipt.id,
                    createdAt = Clock.System.now(),
                )
                confirmReceipt(receipt, transaction)
                _saved.value = transaction.id
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                // Clearing isSaving matters as much as reporting: leaving it set locks the
                // save button and strands a receipt the user can still legitimately save.
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Couldn't save that transaction",
                )
            }
        }
    }

    private suspend fun setting(key: String): String? =
        runCatching { settingsRepository.get(key) }.getOrNull()?.takeIf { it.isNotBlank() }

    private fun today(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    private companion object {
        const val SUGGESTED_CATEGORY_COUNT = 5

        /** Guaranteed to exist — `InitializeDatabaseUseCase` creates it on first run. */
        const val DEFAULT_ACCOUNT_ID = "default"
    }
}
