package com.divafinance.core.domain.usecase.transactions

import com.divafinance.core.data.repository.TransactionRepository
import com.divafinance.core.domain.engine.CategoryPrediction
import com.divafinance.core.domain.engine.CategoryPredictionEngine
import com.divafinance.core.domain.engine.PredictionContext
import com.divafinance.core.model.enums.SpendingCategory
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Suggests the likeliest categories for the entry being made.
 *
 * Thin by design: fetching history is this class's job, ranking it is
 * [CategoryPredictionEngine]'s, so the scoring can be tested without repositories.
 *
 * Always returns [limit] categories. A prediction from a thin history is weak, but a chip
 * row that shrinks to two options on a new install is worse than one padded with common
 * defaults, so the tail is filled rather than left short.
 */
class PredictCategoryUseCase(
    private val transactionRepository: TransactionRepository,
    private val engine: CategoryPredictionEngine = CategoryPredictionEngine(),
) {
    suspend operator fun invoke(
        merchantName: String? = null,
        amount: Double? = null,
        limit: Int = 5,
    ): List<SpendingCategory> {
        val history = runCatching { transactionRepository.getAll().first() }
            .getOrDefault(emptyList())

        val zone = TimeZone.currentSystemDefault()
        val predictions: List<CategoryPrediction> = engine.predict(
            history = history,
            context = PredictionContext(
                merchantName = merchantName,
                amount = amount,
                at = Clock.System.now().toLocalDateTime(zone),
                zone = zone,
            ),
            limit = limit,
        )

        return (predictions.map { it.category } + FALLBACK_ORDER)
            .distinct()
            .take(limit)
    }

    private companion object {
        /** Used to pad a short prediction: the categories people log most often. */
        val FALLBACK_ORDER = listOf(
            SpendingCategory.GROCERIES,
            SpendingCategory.DINING,
            SpendingCategory.TRANSPORTATION,
            SpendingCategory.SHOPPING,
            SpendingCategory.ENTERTAINMENT,
        )
    }
}
