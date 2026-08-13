package com.divafinance.core.domain.usecase.transactions

import com.divafinance.core.data.repository.TransactionRepository
import com.divafinance.core.model.Transaction
import kotlinx.coroutines.flow.Flow

class GetTransactionsUseCase(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(): Flow<List<Transaction>> = transactionRepository.getAll()
}
