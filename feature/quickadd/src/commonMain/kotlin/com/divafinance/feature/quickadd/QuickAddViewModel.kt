package com.divafinance.feature.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divafinance.core.common.ExpressionEvaluator
import com.divafinance.core.common.UuidGenerator
import com.divafinance.core.common.roundToCents
import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.core.domain.usecase.cards.GetAllCardsUseCase
import com.divafinance.core.domain.usecase.feed.PostTransactionToFeedUseCase
import com.divafinance.core.domain.usecase.transactions.AddTransactionUseCase
import com.divafinance.core.domain.usecase.transactions.DeleteTransactionUseCase
import com.divafinance.core.domain.usecase.transactions.GetTransactionsUseCase
import com.divafinance.core.model.CreditCard
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.UserSettings
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** How many category chips the sheet offers before the user has to expand the full list. */
private const val SUGGESTED_CATEGORY_COUNT = 5

/** Which day the entry is booked against. Backdating further is the full form's job. */
enum class QuickAddDay(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
}

data class QuickAddUiState(
    val expression: String = "",
    val type: TransactionType = TransactionType.DEBIT,
    val category: SpendingCategory = SpendingCategory.OTHER,
    val suggestedCategories: List<SpendingCategory> = emptyList(),
    val showAllCategories: Boolean = false,
    val merchantName: String = "",
    val note: String = "",
    val showDetails: Boolean = false,
    val selectedCardId: String? = null,
    val cards: List<CreditCard> = emptyList(),
    val day: QuickAddDay = QuickAddDay.TODAY,
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    /** Running total shown while typing; tolerates a dangling operator. */
    val previewAmount: Double? get() = ExpressionEvaluator.preview(expression)?.roundToCents()

    /** The amount that would actually be saved, or null if the entry is not valid. */
    val committedAmount: Double?
        get() = ExpressionEvaluator.evaluate(expression)?.roundToCents()?.takeIf { it > 0.0 }

    val canSave: Boolean get() = committedAmount != null && !isSaving
}

/** Emitted once per successful save so the sheet can offer an undo. */
data class QuickAddSaved(
    val transactionId: String,
    val amount: Double,
    val category: SpendingCategory,
)

/**
 * Owns the quick-add sheet.
 *
 * Separate from `TransactionsViewModel` deliberately: that one is hoisted at NavHost scope
 * and shares a single form state between the list screen and the full add screen, relying
 * on the caller to call `resetForm()` first. The sheet is reachable from anywhere, so it
 * owns its state and clears it itself.
 */
class QuickAddViewModel(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    getAllCardsUseCase: GetAllCardsUseCase,
    private val postTransactionToFeedUseCase: PostTransactionToFeedUseCase,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickAddUiState())
    val uiState: StateFlow<QuickAddUiState> = _uiState.asStateFlow()

    private val _saved = MutableStateFlow<QuickAddSaved?>(null)
    val saved: StateFlow<QuickAddSaved?> = _saved.asStateFlow()

    private val cards: StateFlow<List<CreditCard>> = getAllCardsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            cards.collect { list -> _uiState.update { it.copy(cards = list) } }
        }
    }

    /**
     * Refreshes the parts of the sheet that depend on stored state. Called when the sheet
     * opens rather than in `init` so a default changed in settings takes effect without
     * the ViewModel being recreated.
     */
    fun onOpened() {
        viewModelScope.launch {
            val defaultCardId = setting(UserSettings.KEY_DEFAULT_CARD_ID)
            val ranked = rankCategoriesByUse()
            _uiState.update {
                it.copy(
                    selectedCardId = defaultCardId,
                    suggestedCategories = ranked,
                    category = ranked.firstOrNull() ?: SpendingCategory.OTHER,
                )
            }
        }
    }

    // --- keypad ------------------------------------------------------------

    fun onDigit(char: Char) = appendToExpression(char.toString())

    fun onOperator(symbol: Char) = appendToExpression(symbol.toString())

    fun onBackspace() {
        _uiState.update { it.copy(expression = it.expression.dropLast(1), error = null) }
    }

    fun onClear() {
        _uiState.update { it.copy(expression = "", error = null) }
    }

    private fun appendToExpression(text: String) {
        _uiState.update { it.copy(expression = it.expression + text, error = null) }
    }

    // --- form ---------------------------------------------------------------

    fun onTypeChange(type: TransactionType) = _uiState.update {
        // A card only makes sense for money going out.
        it.copy(type = type, selectedCardId = if (type == TransactionType.CREDIT) null else it.selectedCardId)
    }

    fun onCategoryChange(category: SpendingCategory) =
        _uiState.update { it.copy(category = category) }

    fun onToggleAllCategories() =
        _uiState.update { it.copy(showAllCategories = !it.showAllCategories) }

    fun onToggleDetails() = _uiState.update { it.copy(showDetails = !it.showDetails) }

    fun onMerchantChange(name: String) = _uiState.update { it.copy(merchantName = name) }

    fun onNoteChange(note: String) = _uiState.update { it.copy(note = note) }

    fun onCardChange(cardId: String?) = _uiState.update { it.copy(selectedCardId = cardId) }

    fun onDayChange(day: QuickAddDay) = _uiState.update { it.copy(day = day) }

    fun reset() {
        _uiState.value = QuickAddUiState(cards = cards.value)
        _saved.value = null
    }

    fun consumeSaved() {
        _saved.value = null
    }

    // --- save / undo --------------------------------------------------------

    fun save() {
        val state = _uiState.value
        val amount = state.committedAmount
        if (amount == null) {
            _uiState.update { it.copy(error = "Enter an amount greater than zero") }
            return
        }
        if (state.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            val now = Clock.System.now()
            val transaction = Transaction(
                id = UuidGenerator.generate(),
                accountId = defaultAccountId(),
                cardId = state.selectedCardId,
                amount = amount,
                currency = setting(UserSettings.KEY_BASE_CURRENCY) ?: "USD",
                category = state.category,
                merchantName = state.merchantName.ifBlank { null },
                note = state.note.ifBlank { null },
                date = dateFor(state.day, now.toLocalDateTime(TimeZone.currentSystemDefault()).date),
                type = state.type,
                createdAt = now,
            )

            try {
                addTransactionUseCase(transaction)
            } catch (e: Exception) {
                // Previously this was unguarded, which left the form stuck on "Saving...".
                _uiState.update {
                    it.copy(isSaving = false, error = e.message ?: "Couldn't save that entry")
                }
                return@launch
            }

            // The feed is a nicety; failing to post must not lose the transaction.
            runCatching { postTransactionToFeedUseCase(transaction) }

            _saved.value = QuickAddSaved(transaction.id, transaction.amount, transaction.category)
            _uiState.value = QuickAddUiState(cards = cards.value)
            onOpened()
        }
    }

    /**
     * Reverses the last save.
     *
     * Goes through [DeleteTransactionUseCase] rather than the repository so the card
     * balance that [AddTransactionUseCase] raised is lowered again — deleting the row on
     * its own would leave the card permanently overstated.
     */
    fun undo() {
        val saved = _saved.value ?: return
        _saved.value = null
        viewModelScope.launch {
            runCatching { deleteTransactionUseCase(saved.transactionId) }
        }
    }

    // --- helpers ------------------------------------------------------------

    private suspend fun setting(key: String): String? =
        settingsRepository.get(key)?.takeIf { it.isNotBlank() }

    /**
     * Falls back to the id the old code hardcoded. `InitializeDatabaseUseCase` creates a
     * row with that id on launch, so it resolves to a real account either way.
     */
    private suspend fun defaultAccountId(): String =
        setting(UserSettings.KEY_DEFAULT_ACCOUNT_ID) ?: "default"

    private fun dateFor(day: QuickAddDay, today: LocalDate): LocalDate = when (day) {
        QuickAddDay.TODAY -> today
        QuickAddDay.YESTERDAY -> today.minusDays(1)
    }

    /**
     * Most-used categories first, so the common case is one tap.
     *
     * Frequency only — Phase 2 replaces this with a real predictor that also weighs
     * merchant, time of day and location. Padded to a fixed size so a new install still
     * gets a full row of chips instead of an empty one.
     */
    private suspend fun rankCategoriesByUse(): List<SpendingCategory> {
        val history = runCatching { getTransactionsUseCase().first() }.getOrDefault(emptyList())
        val byFrequency = history
            .groupingBy { it.category }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }

        return (byFrequency + DEFAULT_CATEGORY_ORDER)
            .distinct()
            .take(SUGGESTED_CATEGORY_COUNT)
    }

    private companion object {
        /** Fallback ordering for an empty history: the categories people log most. */
        val DEFAULT_CATEGORY_ORDER = listOf(
            SpendingCategory.GROCERIES,
            SpendingCategory.DINING,
            SpendingCategory.TRANSPORTATION,
            SpendingCategory.SHOPPING,
            SpendingCategory.ENTERTAINMENT,
        )
    }
}

/** kotlinx-datetime has no `minusDays` on LocalDate in this version. */
private fun LocalDate.minusDays(days: Int): LocalDate =
    LocalDate.fromEpochDays(toEpochDays() - days)
