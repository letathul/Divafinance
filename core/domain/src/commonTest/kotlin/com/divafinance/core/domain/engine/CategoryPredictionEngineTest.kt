package com.divafinance.core.domain.engine

import com.divafinance.core.model.Transaction
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import com.divafinance.core.testing.fake.TestData
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CategoryPredictionEngineTest {

    private val engine = CategoryPredictionEngine()

    /** Fixed reference point so recency and time signals are deterministic. */
    private val now = LocalDateTime(2026, 6, 15, 12, 30) // a Monday, midday

    private fun context(
        merchant: String? = null,
        amount: Double? = null,
        at: LocalDateTime = now,
    ) = PredictionContext(merchant, amount, at, TimeZone.UTC)

    /** Logged at midday UTC on its own date, so the hour signal is controllable. */
    private fun tx(
        id: String,
        category: SpendingCategory,
        merchant: String? = null,
        amount: Double = 50.0,
        date: LocalDate = LocalDate(2026, 6, 15),
        hour: Int = 12,
        type: TransactionType = TransactionType.DEBIT,
    ): Transaction = TestData.transaction(
        id = id,
        category = category,
        merchantName = merchant,
        amount = amount,
        date = date,
        type = type,
    ).copy(createdAt = LocalDateTime(date.year, date.month, date.day, hour, 0).toInstantUtc())

    private fun top(predictions: List<CategoryPrediction>) = predictions.first().category

    // --- empty / degenerate -------------------------------------------------

    @Test
    fun returnsNothingWithoutHistory() {
        assertEquals(emptyList(), engine.predict(emptyList(), context()))
    }

    @Test
    fun ignoresIncomeHistory() {
        val history = listOf(
            tx("1", SpendingCategory.OTHER, type = TransactionType.CREDIT),
        )
        assertEquals(emptyList(), engine.predict(history, context()))
    }

    @Test
    fun neverReturnsMoreThanTheLimit() {
        val history = SpendingCategory.entries.mapIndexed { i, category ->
            tx("$i", category)
        }
        assertEquals(3, engine.predict(history, context(), limit = 3).size)
    }

    @Test
    fun confidencesSumToOneAcrossAllCategories() {
        val history = listOf(
            tx("1", SpendingCategory.DINING),
            tx("2", SpendingCategory.GAS),
        )
        val total = engine.predict(history, context(), limit = 99).sumOf { it.confidence }
        assertTrue(total in 0.999..1.001, "confidences summed to $total")
    }

    // --- merchant -----------------------------------------------------------

    @Test
    fun anExactMerchantMatchWins() {
        val history = listOf(
            // Stack the other signals against the answer: many recent Groceries rows.
            tx("1", SpendingCategory.GROCERIES),
            tx("2", SpendingCategory.GROCERIES),
            tx("3", SpendingCategory.GROCERIES),
            tx("4", SpendingCategory.GROCERIES),
            tx("5", SpendingCategory.DINING, merchant = "Blue Bottle"),
        )

        assertEquals(
            SpendingCategory.DINING,
            top(engine.predict(history, context(merchant = "Blue Bottle"))),
        )
    }

    @Test
    fun matchesMerchantsCaseInsensitively() {
        val history = listOf(
            tx("1", SpendingCategory.GROCERIES),
            tx("2", SpendingCategory.GROCERIES),
            tx("3", SpendingCategory.DINING, merchant = "Blue Bottle"),
        )

        assertEquals(
            SpendingCategory.DINING,
            top(engine.predict(history, context(merchant = "  blue bottle  "))),
        )
    }

    /** Card statements append store numbers; "Starbucks" should still find "Starbucks #123". */
    @Test
    fun matchesAMerchantPrefix() {
        val history = listOf(
            tx("1", SpendingCategory.GROCERIES),
            tx("2", SpendingCategory.GROCERIES),
            tx("3", SpendingCategory.DINING, merchant = "Starbucks #123"),
        )

        assertEquals(
            SpendingCategory.DINING,
            top(engine.predict(history, context(merchant = "Starbucks"))),
        )
    }

    @Test
    fun anUnknownMerchantDoesNotDistortTheRanking() {
        val history = listOf(
            tx("1", SpendingCategory.GROCERIES, merchant = "Corner Shop"),
            tx("2", SpendingCategory.GROCERIES, merchant = "Corner Shop"),
            tx("3", SpendingCategory.DINING, merchant = "Blue Bottle"),
        )

        assertEquals(
            SpendingCategory.GROCERIES,
            top(engine.predict(history, context(merchant = "Totally New Place"))),
        )
    }

    // --- recency ------------------------------------------------------------

    @Test
    fun recentCategoriesOutrankStaleOnes() {
        val history = listOf(
            // Three hits a year ago vs one hit today.
            tx("1", SpendingCategory.TRAVEL, date = LocalDate(2025, 6, 15)),
            tx("2", SpendingCategory.TRAVEL, date = LocalDate(2025, 6, 15)),
            tx("3", SpendingCategory.TRAVEL, date = LocalDate(2025, 6, 15)),
            tx("4", SpendingCategory.DINING, date = LocalDate(2026, 6, 15)),
        )

        assertEquals(SpendingCategory.DINING, top(engine.predict(history, context())))
    }

    @Test
    fun frequencyStillCountsWithinTheSamePeriod() {
        val history = listOf(
            tx("1", SpendingCategory.GAS),
            tx("2", SpendingCategory.GAS),
            tx("3", SpendingCategory.DINING),
        )

        assertEquals(SpendingCategory.GAS, top(engine.predict(history, context())))
    }

    // --- time of day --------------------------------------------------------

    @Test
    fun prefersCategoriesLoggedAroundTheSameHour() {
        val history = listOf(
            tx("1", SpendingCategory.DINING, hour = 12),
            tx("2", SpendingCategory.UTILITIES, hour = 3),
        )

        assertEquals(
            SpendingCategory.DINING,
            top(engine.predict(history, context(at = LocalDateTime(2026, 6, 15, 12, 30)))),
        )
    }

    /** 23:00 and 01:00 are two hours apart, not twenty-two. */
    @Test
    fun treatsTheHourAsCircular() {
        val history = listOf(
            tx("1", SpendingCategory.ENTERTAINMENT, hour = 23),
            tx("2", SpendingCategory.HEALTHCARE, hour = 12),
        )

        assertEquals(
            SpendingCategory.ENTERTAINMENT,
            top(engine.predict(history, context(at = LocalDateTime(2026, 6, 16, 1, 0)))),
        )
    }

    @Test
    fun prefersCategoriesFromTheSameKindOfDay() {
        val history = listOf(
            // Saturday, at an hour that gives neither row the hour bonus.
            tx("1", SpendingCategory.SHOPPING, date = LocalDate(2026, 6, 13), hour = 20),
            // Monday.
            tx("2", SpendingCategory.EDUCATION, date = LocalDate(2026, 6, 15), hour = 20),
        )

        // Sunday: matches the weekend row.
        val weekend = engine.predict(history, context(at = LocalDateTime(2026, 6, 14, 20, 0)))
        assertEquals(SpendingCategory.SHOPPING, top(weekend))
    }

    // --- amount -------------------------------------------------------------

    @Test
    fun prefersCategoriesWithASimilarAmount() {
        val history = listOf(
            tx("1", SpendingCategory.SUBSCRIPTIONS, amount = 9.99),
            tx("2", SpendingCategory.TRAVEL, amount = 850.0),
        )

        assertEquals(
            SpendingCategory.SUBSCRIPTIONS,
            top(engine.predict(history, context(amount = 11.0))),
        )
        assertEquals(
            SpendingCategory.TRAVEL,
            top(engine.predict(history, context(amount = 900.0))),
        )
    }

    @Test
    fun ignoresAmountWhenNoneIsEnteredYet() {
        val history = listOf(
            tx("1", SpendingCategory.SUBSCRIPTIONS, amount = 9.99),
            tx("2", SpendingCategory.TRAVEL, amount = 850.0),
            tx("3", SpendingCategory.TRAVEL, amount = 850.0),
        )

        assertEquals(SpendingCategory.TRAVEL, top(engine.predict(history, context(amount = null))))
    }

    // --- ordering -----------------------------------------------------------

    @Test
    fun ordersByDescendingConfidence() {
        val history = listOf(
            tx("1", SpendingCategory.GAS),
            tx("2", SpendingCategory.GAS),
            tx("3", SpendingCategory.DINING),
        )

        val predictions = engine.predict(history, context())
        assertEquals(
            predictions.map { it.confidence }.sortedDescending(),
            predictions.map { it.confidence },
        )
    }

    /** Equal scores must not reshuffle between calls, or the chip row jumps around. */
    @Test
    fun isStableForEqualScores() {
        val history = listOf(
            tx("1", SpendingCategory.DINING),
            tx("2", SpendingCategory.GAS),
            tx("3", SpendingCategory.TRAVEL),
        )

        val first = engine.predict(history, context()).map { it.category }
        repeat(5) {
            assertEquals(first, engine.predict(history, context()).map { it.category })
        }
    }
}

/** Test-local: treat a wall-clock time as UTC so `createdAt` hours are deterministic. */
private fun LocalDateTime.toInstantUtc(): Instant = toInstant(TimeZone.UTC)
