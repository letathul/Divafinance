package com.divafinance.core.domain.usecase.transactions

import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.core.data.repository.TransactionRepository
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.UserSettings
import com.divafinance.core.model.enums.TransactionType
import kotlinx.datetime.LocalDate

class GetHighImpactTransactionsUseCase(
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<Transaction> {
        val threshold = settingsRepository.get(UserSettings.KEY_IMPACT_THRESHOLD)
            ?.toDoubleOrNull() ?: 100.0

        return transactionRepository.getByDateRange(startDate, endDate)
            .filter { it.type == TransactionType.DEBIT && it.amount >= threshold }
            .sortedByDescending { it.amount }
    }
}
