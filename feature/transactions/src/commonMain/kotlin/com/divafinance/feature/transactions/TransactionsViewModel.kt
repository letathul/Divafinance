package com.divafinance.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divafinance.core.common.UuidGenerator
import com.divafinance.core.domain.usecase.cards.GetAllCardsUseCase
import com.divafinance.core.domain.usecase.transactions.AddTransactionUseCase
import com.divafinance.core.domain.usecase.transactions.GetTransactionsUseCase
import com.divafinance.core.model.CreditCard
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class TransactionSortOrder(val label: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    AMOUNT_DESC("Highest Amount"),
    AMOUNT_ASC("Lowest Amount"),
}

data class TransactionFilterState(
    val categoryFilter: SpendingCategory? = null,
    val typeFilter: TransactionType? = null,
    val sortOrder: TransactionSortOrder = TransactionSortOrder.DATE_DESC,
    val searchQuery: String = "",
)

data class TransactionFormState(
    val amount: String = "",
    val category: SpendingCategory = SpendingCategory.OTHER,
    val type: TransactionType = TransactionType.DEBIT,
    val merchantName: String = "",
    val note: String = "",
    val selectedCardId: String? = null,
    val isSaving: Boolean = false,
)

class TransactionsViewModel(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val getAllCardsUseCase: GetAllCardsUseCase,
) : ViewModel() {

    private val _filterState = MutableStateFlow(TransactionFilterState())
    val filterState: StateFlow<TransactionFilterState> = _filterState.asStateFlow()

    private val _formState = MutableStateFlow(TransactionFormState())
    val formState: StateFlow<TransactionFormState> = _formState.asStateFlow()

    val cards: StateFlow<List<CreditCard>> = getAllCardsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allTransactions = getTransactionsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTransactions: StateFlow<List<Transaction>> =
        combine(allTransactions, _filterState) { transactions, filter ->
            transactions
                .filter { tx ->
                    (filter.categoryFilter == null || tx.category == filter.categoryFilter) &&
                        (filter.typeFilter == null || tx.type == filter.typeFilter) &&
                        (filter.searchQuery.isBlank() || matchesSearch(tx, filter.searchQuery))
                }
                .let { list ->
                    when (filter.sortOrder) {
                        TransactionSortOrder.DATE_DESC -> list.sortedByDescending { it.date }
                        TransactionSortOrder.DATE_ASC -> list.sortedBy { it.date }
                        TransactionSortOrder.AMOUNT_DESC -> list.sortedByDescending { it.amount }
                        TransactionSortOrder.AMOUNT_ASC -> list.sortedBy { it.amount }
                    }
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun matchesSearch(tx: Transaction, query: String): Boolean {
        val q = query.lowercase()
        return (tx.merchantName?.lowercase()?.contains(q) == true) ||
            (tx.note?.lowercase()?.contains(q) == true) ||
            tx.category.displayName.lowercase().contains(q)
    }

    fun updateCategoryFilter(category: SpendingCategory?) {
        _filterState.update { it.copy(categoryFilter = category) }
    }

    fun updateTypeFilter(type: TransactionType?) {
        _filterState.update { it.copy(typeFilter = type) }
    }

    fun updateSortOrder(order: TransactionSortOrder) {
        _filterState.update { it.copy(sortOrder = order) }
    }

    fun updateSearchQuery(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun resetForm() {
        _formState.value = TransactionFormState()
    }

    fun updateAmount(amount: String) {
        _formState.update { it.copy(amount = amount) }
    }

    fun updateCategory(category: SpendingCategory) {
        _formState.update { it.copy(category = category) }
    }

    fun updateType(type: TransactionType) {
        _formState.update { it.copy(type = type) }
    }

    fun updateMerchantName(name: String) {
        _formState.update { it.copy(merchantName = name) }
    }

    fun updateNote(note: String) {
        _formState.update { it.copy(note = note) }
    }

    fun updateSelectedCard(cardId: String?) {
        _formState.update { it.copy(selectedCardId = cardId) }
    }

    fun saveTransaction(onSuccess: () -> Unit) {
        val form = _formState.value
        val amount = form.amount.toDoubleOrNull() ?: return

        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true) }

            val now = Clock.System.now()
            val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date

            val transaction = Transaction(
                id = UuidGenerator.generate(),
                accountId = "default",
                cardId = form.selectedCardId,
                amount = amount,
                category = form.category,
                merchantName = form.merchantName.ifBlank { null },
                note = form.note.ifBlank { null },
                date = today,
                type = form.type,
                createdAt = now,
            )

            addTransactionUseCase(transaction)
            _formState.update { it.copy(isSaving = false) }
            onSuccess()
        }
    }
}
