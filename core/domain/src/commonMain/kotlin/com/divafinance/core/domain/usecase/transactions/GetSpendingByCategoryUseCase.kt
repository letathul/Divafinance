package com.divafinance.core.domain.usecase.transactions

import com.divafinance.core.data.repository.TransactionRepository
import kotlinx.datetime.LocalDate

class GetSpendingByCategoryUseCase(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Map<String, Double> {
        return transactionRepository.getSpendingByCategory(startDate, endDate)
    }
}
