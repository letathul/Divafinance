package com.divafinance.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divafinance.core.domain.usecase.cards.GetAllCardsUseCase
import com.divafinance.core.domain.usecase.transactions.GetTransactionsUseCase
import com.divafinance.core.model.CreditCard
import com.divafinance.core.model.Transaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    getTransactionsUseCase: GetTransactionsUseCase,
    getAllCardsUseCase: GetAllCardsUseCase,
) : ViewModel() {

    val cards: StateFlow<List<CreditCard>> = getAllCardsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<Transaction>> = getTransactionsUseCase()
        .map { transactions ->
            transactions
                .sortedByDescending { it.date }
                .take(5)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSpending: StateFlow<Double> = getTransactionsUseCase()
        .map { transactions ->
            transactions
                .filter { it.type == com.divafinance.core.model.enums.TransactionType.DEBIT }
                .sumOf { it.amount }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalIncome: StateFlow<Double> = getTransactionsUseCase()
        .map { transactions ->
            transactions
                .filter { it.type == com.divafinance.core.model.enums.TransactionType.CREDIT }
                .sumOf { it.amount }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
}
