package com.divafinance.core.domain.usecase.graphs

import com.divafinance.core.data.repository.ThresholdRepository
import com.divafinance.core.data.repository.TransactionRepository
import com.divafinance.core.model.GraphThreshold
import kotlinx.datetime.LocalDate

data class CategoryThresholdData(
    val category: String,
    val spending: Double,
    val threshold: GraphThreshold?,
    val percentOfThreshold: Double?,
    val isOverThreshold: Boolean,
)

class GetThresholdGraphDataUseCase(
    private val transactionRepository: TransactionRepository,
    private val thresholdRepository: ThresholdRepository,
) {
    suspend operator fun invoke(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<CategoryThresholdData> {
        val spending = transactionRepository.getSpendingByCategory(startDate, endDate)
        val totalSpending = spending.values.sum().takeIf { it > 0.0 } ?: return emptyList()

        return spending.map { (category, amount) ->
            val threshold = thresholdRepository.getByCategory(
                com.divafinance.core.model.enums.SpendingCategory.valueOf(category)
            )
            val percentOfTotal = (amount / totalSpending) * 100.0
            val percentOfThreshold = threshold?.let { percentOfTotal / it.thresholdPercent * 100.0 }

            CategoryThresholdData(
                category = category,
                spending = amount,
                threshold = threshold,
                percentOfThreshold = percentOfThreshold,
                isOverThreshold = threshold != null && percentOfTotal > threshold.thresholdPercent,
            )
        }.sortedByDescending { it.spending }
    }
}
