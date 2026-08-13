package com.divafinance.core.domain.usecase.location

import com.divafinance.core.data.repository.TransactionRepository
import com.divafinance.core.model.LocationTag

class TagTransactionLocationUseCase(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transactionId: String, location: LocationTag) {
        val transaction = transactionRepository.getById(transactionId) ?: return
        transactionRepository.update(transaction.copy(location = location))
    }
}
