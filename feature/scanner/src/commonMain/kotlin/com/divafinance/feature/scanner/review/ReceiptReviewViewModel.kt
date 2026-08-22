package com.divafinance.feature.scanner.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divafinance.core.common.ExpressionEvaluator
import com.divafinance.core.common.UuidGenerator
import com.divafinance.core.common.roundToCents
import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.core.domain.engine.CardRecommendation
import com.divafinance.core.domain.usecase.cards.GetAllCardsUseCase
import com.divafinance.core.domain.usecase.cards.GetBestCardForCategoryUseCase
import com.divafinance.core.domain.usecase.scanner.ConfirmReceiptUseCase
import com.divafinance.core.domain.usecase.scanner.GetReceiptUseCase
import com.divafinance.core.domain.usecase.transactions.PredictCategoryUseCase
import com.divafinance.core.model.CreditCard
import com.divafinance.core.model.Receipt
import com.divafinance.core.model.ReceiptLineItem
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.UserSettings
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import kotlin.math.abs
import kotlin.time.Clock
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** One editable row in the item list. Held as text so a half-typed price isn't discarded. */
data class LineItemDraft(
    val id: String,
    val description: String,
    val priceText: String,
) {
    val price: Double? get() = priceText.replace(",", ".").trim().toDoubleOrNull()
}

data class ReceiptReviewUiState(
    val isLoading: Boolean = true,
    val receipt: Receipt? = null,
    val merchantName: String = "",
    val amountText: String = "",
    val dateText: String = "",
    val note: String = "",
    val category: SpendingCategory = SpendingCategory.OTHER,
    val suggestedCategories: List<SpendingCategory> = emptyList(),
    val cards: List<CreditCard> = emptyList(),
    val selectedCardId: String? = null,
    val currency: String = "USD",
    val type: TransactionType = TransactionType.DEBIT,
    val items: List<LineItemDraft> = emptyList(),
    val taxAmount: Double? = null,
    val tipAmount: Double? = null,
    /** Extra pages of a multi-page scan; page 1 is `receipt.imagePath`. */
    val pagePaths: List<String> = emptyList(),
    /** The card the reward engine would have picked, when it isn't the one selected. */
    val betterCard: CardRecommendation? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    /**
     * Null unless the field holds a usable positive figure — this is what gates saving.
     * Evaluated rather than parsed so `12.40 + 3` works here as it does on the quick-add
     * keypad; a receipt total is exactly the kind of figure people arrive at by adding up.
     */
    val amount: Double?
        get() = ExpressionEvaluator.evaluate(amountText.replace(",", "."))
            ?.roundToCents()
            ?.takeIf { it > 0.0 }

    /** Null while the field is being typed into; [canSave] does not depend on it. */
    val date: LocalDate?
        get() = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()

    val hasDateError: Boolean get() = dateText.isNotBlank() && date == null

    /** What the itemised lines add up to, ignoring rows with no usable price. */
    val itemsTotal: Double?
        get() = items.mapNotNull { it.price }
            .takeIf { it.isNotEmpty() }
            ?.sum()
            ?.roundToCents()

    /**
     * True when the items plus tax and tip don't reconcile with the total. Not an error — a
     * receipt often prints a discount line this parser never sees — but it is the cheapest
     * signal available that a figure was misread, so it is worth putting in front of the user
     * rather than silently saving a wrong amount.
     */
    val itemsDisagreeWithTotal: Boolean
        get() {
            val expected = itemsTotal ?: return false
            val actual = amount ?: return false
            val extras = (taxAmount ?: 0.0) + (tipAmount ?: 0.0)
            return abs(expected + extras - actual) > RECONCILE_TOLERANCE
        }

    val canSave: Boolean get() = amount != null && !isSaving && !isLoading && !hasDateError

    private companion object {
        /** Half a cent: anything larger is a real disagreement, not rounding. */
        const val RECONCILE_TOLERANCE = 0.005
    }
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
    private val getBestCard: GetBestCardForCategoryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiptReviewUiState())
    val uiState: StateFlow<ReceiptReviewUiState> = _uiState.asStateFlow()

    /** The id of the transaction just created, for the screen to navigate away on. */
    private val _saved = MutableStateFlow<String?>(null)
    val saved: StateFlow<String?> = _saved.asStateFlow()

    private var bestCardJob: Job? = null

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
                dateText = (receipt.date ?: today()).toString(),
                category = suggestions.firstOrNull() ?: SpendingCategory.OTHER,
                suggestedCategories = suggestions,
                cards = cards,
                selectedCardId = setting(UserSettings.KEY_DEFAULT_CARD_ID)
                    ?.takeIf { id -> cards.any { it.id == id } },
                // What the receipt printed wins over the user's default: a trip abroad is
                // exactly when this field matters and exactly when the default is wrong.
                currency = receipt.currency
                    ?: setting(UserSettings.KEY_BASE_CURRENCY)
                    ?: DEFAULT_CURRENCY,
                items = receipt.lineItems.map { it.toDraft() },
                taxAmount = receipt.taxAmount,
                tipAmount = receipt.tipAmount,
                pagePaths = receipt.pagePaths,
            )
            refreshBestCard()
        }
    }

    fun onMerchantChanged(value: String) {
        _uiState.value = _uiState.value.copy(merchantName = value)
    }

    fun onAmountChanged(value: String) {
        _uiState.value = _uiState.value.copy(amountText = value)
        refreshBestCard()
    }

    fun onDateChanged(value: LocalDate) {
        _uiState.value = _uiState.value.copy(dateText = value.toString())
    }

    fun onDateTextChanged(value: String) {
        _uiState.value = _uiState.value.copy(dateText = value)
    }

    fun onNoteChanged(value: String) {
        _uiState.value = _uiState.value.copy(note = value)
    }

    fun onCurrencyChanged(value: String) {
        // Uppercased on the way in: the column feeds formatting and grouping, where "usd"
        // and "USD" would be two currencies.
        _uiState.value = _uiState.value.copy(currency = value.uppercase().take(CURRENCY_LENGTH))
    }

    fun onTypeSelected(type: TransactionType) {
        _uiState.value = _uiState.value.copy(type = type)
    }

    fun onCategorySelected(category: SpendingCategory) {
        _uiState.value = _uiState.value.copy(category = category)
        refreshBestCard()
    }

    fun onCardSelected(cardId: String?) {
        _uiState.value = _uiState.value.copy(selectedCardId = cardId)
        refreshBestCard()
    }

    // ── Line items ───────────────────────────────────────────────────────────────────

    fun onItemDescriptionChanged(id: String, value: String) {
        updateItem(id) { it.copy(description = value) }
    }

    fun onItemPriceChanged(id: String, value: String) {
        updateItem(id) { it.copy(priceText = value) }
    }

    fun onItemRemoved(id: String) {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.filterNot { it.id == id },
        )
    }

    fun onItemAdded() {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items + LineItemDraft(
                id = UuidGenerator.generate(),
                description = "",
                priceText = "",
            ),
        )
    }

    private fun updateItem(id: String, transform: (LineItemDraft) -> LineItemDraft) {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.map { if (it.id == id) transform(it) else it },
        )
    }

    /**
     * The reward engine already exists and already knows the answer; the scanner simply never
     * asked it. Only a card that beats the selected one is surfaced — telling someone they
     * picked correctly is noise.
     */
    private fun refreshBestCard() {
        // Cancel the in-flight lookup rather than letting two of them race to write the
        // result; this is re-triggered on every keystroke in the amount field.
        bestCardJob?.cancel()
        bestCardJob = viewModelScope.launch {
            val state = _uiState.value
            val amount = state.amount
            if (amount == null) {
                _uiState.value = _uiState.value.copy(betterCard = null)
                return@launch
            }
            val best = runCatching { getBestCard(state.category, amount) }
                .getOrDefault(emptyList())
                .firstOrNull()
            _uiState.value = _uiState.value.copy(
                betterCard = best?.takeIf { it.card.id != _uiState.value.selectedCardId },
            )
        }
    }

    fun save() {
        val state = _uiState.value
        val receipt = state.receipt ?: return
        val amount = state.amount ?: return
        if (state.isSaving || state.hasDateError) return

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
                    note = state.note.ifBlank { null },
                    date = state.date ?: today(),
                    type = state.type,
                    receiptId = receipt.id,
                    createdAt = Clock.System.now(),
                )
                // The corrected values are written back onto the receipt too, so re-opening it
                // from the history shows what was actually saved rather than what OCR guessed.
                confirmReceipt(
                    receipt.copy(
                        currency = state.currency,
                        lineItems = state.items.toLineItems(receipt.id),
                    ),
                    transaction,
                )
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
        const val CURRENCY_LENGTH = 3
        const val DEFAULT_CURRENCY = "USD"

        /** Guaranteed to exist — `InitializeDatabaseUseCase` creates it on first run. */
        const val DEFAULT_ACCOUNT_ID = "default"
    }
}

private fun ReceiptLineItem.toDraft() = LineItemDraft(
    id = id,
    description = description,
    priceText = totalPrice?.toString().orEmpty(),
)

/** Blank rows are dropped rather than saved: an empty item is a row the user gave up on. */
private fun List<LineItemDraft>.toLineItems(receiptId: String): List<ReceiptLineItem> =
    filter { it.description.isNotBlank() || it.price != null }
        .mapIndexed { index, draft ->
            ReceiptLineItem(
                id = draft.id,
                receiptId = receiptId,
                position = index,
                description = draft.description,
                totalPrice = draft.price,
            )
        }
