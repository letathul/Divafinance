package com.divafinance.core.domain.engine

import com.divafinance.core.model.Transaction
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs

data class CategoryPrediction(
    val category: SpendingCategory,
    val confidence: Double,
)

/** What is known about the entry being made, at the moment it is being made. */
data class PredictionContext(
    val merchantName: String? = null,
    val amount: Double? = null,
    val at: LocalDateTime,
    /** Zone used to read the hour out of a stored `createdAt` instant. */
    val zone: TimeZone = TimeZone.currentSystemDefault(),
)

/**
 * Ranks spending categories for a part-entered transaction.
 *
 * Rule-based rather than learned: the signals available are few and legible, the training
 * set is one person's own history, and a wrong guess costs a tap. Scores from each signal
 * are summed per category and normalised, so the ordering is explainable and each signal
 * can be tested in isolation.
 *
 * Pure by design, matching [RewardRecommendationEngine] — history is passed in, so this is
 * testable without repositories or fakes.
 *
 * Location is deliberately absent. Nothing writes coordinates during entry yet, so a
 * proximity term would contribute zero to every prediction while looking like it worked.
 */
class CategoryPredictionEngine {

    fun predict(
        history: List<Transaction>,
        context: PredictionContext,
        limit: Int = 5,
    ): List<CategoryPrediction> {
        // Income has its own shape and would drag spending predictions around.
        val spending = history.filter { it.type == TransactionType.DEBIT }
        if (spending.isEmpty()) return emptyList()

        val scores = mutableMapOf<SpendingCategory, Double>()
        fun award(category: SpendingCategory, points: Double) {
            if (points > 0.0) scores[category] = (scores[category] ?: 0.0) + points
        }

        val newest = spending.maxOf { it.date.toEpochDays() }

        for (transaction in spending) {
            val category = transaction.category
            award(category, merchantScore(transaction, context.merchantName))
            award(category, recencyScore(transaction.date, newest))
            award(category, timeScore(transaction, context.at, context.zone))
            award(category, amountScore(transaction.amount, context.amount))
        }

        val total = scores.values.sum()
        if (total <= 0.0) return emptyList()

        return scores.entries
            .map { CategoryPrediction(it.key, it.value / total) }
            // Ordering must be stable for equal scores, or the chip row reshuffles
            // between recompositions for no visible reason.
            .sortedWith(compareByDescending<CategoryPrediction> { it.confidence }.thenBy { it.category.name })
            .take(limit)
    }

    /**
     * Strongest signal by a wide margin: the same shop almost always means the same
     * category. An exact match outweighs everything else combined, and a containment match
     * ("Starbucks" vs "Starbucks #123") still beats every other signal.
     */
    private fun merchantScore(transaction: Transaction, typed: String?): Double {
        val query = typed?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return 0.0
        val stored = transaction.merchantName?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
            ?: return 0.0

        return when {
            stored == query -> WEIGHT_MERCHANT_EXACT
            stored.startsWith(query) || query.startsWith(stored) -> WEIGHT_MERCHANT_PREFIX
            stored.contains(query) || query.contains(stored) -> WEIGHT_MERCHANT_PARTIAL
            else -> 0.0
        }
    }

    /**
     * Habits drift, so a category used last week counts for more than the same category
     * used a year ago. Halves every [RECENCY_HALF_LIFE_DAYS], which keeps old history
     * contributing without letting it dominate.
     */
    private fun recencyScore(date: LocalDate, newestEpochDay: Long): Double {
        val age = (newestEpochDay - date.toEpochDays()).coerceAtLeast(0L)
        val halfLives = age.toDouble() / RECENCY_HALF_LIFE_DAYS
        var decay = 1.0
        var remaining = halfLives
        // Cheap pow() for a value that only needs to be roughly right.
        while (remaining >= 1.0) {
            decay *= 0.5
            remaining -= 1.0
            if (decay < 0.001) return 0.0
        }
        return WEIGHT_RECENCY * decay * (1.0 - 0.5 * remaining)
    }

    /**
     * People spend to a rhythm: coffee in the morning, dinner in the evening, groceries at
     * the weekend. Two separate comparisons — same kind of day, and a similar hour.
     *
     * The hour comes from `createdAt` because `date` is a plain [LocalDate] with no time.
     * That makes it the hour the entry was logged rather than strictly the hour of the
     * purchase, which for hand-entered transactions is usually close enough to be useful
     * and is the only time-of-day signal actually stored.
     */
    private fun timeScore(transaction: Transaction, now: LocalDateTime, zone: TimeZone): Double {
        var score = 0.0

        if (isWeekend(transaction.date.dayOfWeek) == isWeekend(now.date.dayOfWeek)) {
            score += WEIGHT_DAY_KIND
        }

        val loggedHour = transaction.createdAt.toLocalDateTime(zone).hour
        if (hoursApart(loggedHour, now.hour) <= NEARBY_HOURS) {
            score += WEIGHT_HOUR
        }
        return score
    }

    /** Circular distance, so 23:00 and 01:00 are two hours apart rather than twenty-two. */
    private fun hoursApart(a: Int, b: Int): Int {
        val direct = abs(a - b)
        return minOf(direct, 24 - direct)
    }

    /**
     * A £3 spend and a £300 spend are rarely the same kind of purchase. Compares order of
     * magnitude rather than absolute difference, so "about this big" is what matters.
     */
    private fun amountScore(stored: Double, typed: Double?): Double {
        val target = typed?.takeIf { it > 0.0 } ?: return 0.0
        if (stored <= 0.0) return 0.0

        val ratio = if (stored > target) stored / target else target / stored
        return when {
            ratio <= 1.5 -> WEIGHT_AMOUNT_CLOSE
            ratio <= 4.0 -> WEIGHT_AMOUNT_NEAR
            else -> 0.0
        }
    }

    private fun isWeekend(day: DayOfWeek): Boolean =
        day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY

    private companion object {
        const val WEIGHT_MERCHANT_EXACT = 12.0
        const val WEIGHT_MERCHANT_PREFIX = 6.0
        const val WEIGHT_MERCHANT_PARTIAL = 3.0
        const val WEIGHT_RECENCY = 1.0
        const val WEIGHT_DAY_KIND = 0.3
        const val WEIGHT_HOUR = 0.5
        const val WEIGHT_AMOUNT_CLOSE = 0.6
        const val WEIGHT_AMOUNT_NEAR = 0.2

        const val RECENCY_HALF_LIFE_DAYS = 45.0
        const val NEARBY_HOURS = 2
    }
}
