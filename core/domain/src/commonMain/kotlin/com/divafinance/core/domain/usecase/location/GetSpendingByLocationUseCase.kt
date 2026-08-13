package com.divafinance.core.domain.usecase.location

import com.divafinance.core.data.repository.TransactionRepository
import com.divafinance.core.model.Transaction

class GetSpendingByLocationUseCase(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(): Map<String, List<Transaction>> {
        return transactionRepository.getWithLocation()
            .groupBy { it.location?.name ?: "Unknown" }
    }
}
