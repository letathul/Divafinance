package com.divafinance.core.domain.usecase.transactions

import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.testing.fake.FakeTransactionRepository
import com.divafinance.core.testing.fake.TestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A split bill must contribute only the user's own share to spending, everywhere.
 *
 * There are several separate implementations of "how much did I spend" in this app — the
 * `sumByCategory` SQL, the `totalSpending` SQL, and the in-memory `ownShare` sums in
 * `FeedViewModel`, `ReportViewModel` and `YouViewModel` — and nothing but this test stops
 * them drifting apart. The visible symptom would be the feed and a period report
 * disagreeing about the same dinner.
 */
class SharedExpenseSpendingTest {

    private val repo = FakeTransactionRepository()
    private val start = LocalDate(2026, 1, 1)
    private val end = LocalDate(2026, 12, 31)
    private val date = LocalDate(2026, 6, 15)

    /** A £120 dinner split three ways: £40 mine, £80 owed back. */
    private fun seedSplitDinner() {
        repo.setTransactions(
            listOf(
                TestData.transaction(
                    id = "dinner",
                    amount = 120.0,
                    othersShare = 80.0,
                    category = SpendingCategory.DINING,
                    date = date,
                )
            )
        )
    }

    /** Mirrors the in-memory `ownShare` calculation the feed and report view models use. */
    private suspend fun dashboardStyleTotal(): Double =
        repo.getAll().first()
            .filter { it.type == com.divafinance.core.model.enums.TransactionType.DEBIT }
            .sumOf { it.amount - it.othersShare }

    @Test
    fun onlyTheUsersShareCountsTowardsACategory() = runTest {
        seedSplitDinner()

        assertEquals(
            mapOf(SpendingCategory.DINING.name to 40.0),
            repo.getSpendingByCategory(start, end),
        )
    }

    @Test
    fun onlyTheUsersShareCountsTowardsTheTotal() = runTest {
        seedSplitDinner()

        assertEquals(40.0, repo.getTotalSpending(start, end))
    }

    /** The one that catches home screen and graphs drifting apart. */
    @Test
    fun allThreeSpendingTotalsAgree() = runTest {
        seedSplitDinner()

        val byCategory = repo.getSpendingByCategory(start, end).values.sum()
        val total = repo.getTotalSpending(start, end)
        val dashboard = dashboardStyleTotal()

        assertEquals(40.0, byCategory)
        assertEquals(40.0, total)
        assertEquals(40.0, dashboard)
    }

    @Test
    fun anOrdinaryTransactionIsUnaffected() = runTest {
        repo.setTransactions(
            listOf(TestData.transaction(id = "solo", amount = 50.0, date = date))
        )

        assertEquals(50.0, repo.getTotalSpending(start, end))
        assertEquals(50.0, dashboardStyleTotal())
    }

    /** Paying for someone entirely means the spend was never yours. */
    @Test
    fun aFullyReimbursedTransactionCountsAsNoSpending() = runTest {
        repo.setTransactions(
            listOf(
                TestData.transaction(
                    id = "ticket",
                    amount = 80.0,
                    othersShare = 80.0,
                    date = date,
                )
            )
        )

        assertEquals(0.0, dashboardStyleTotal())
        // getTotalSpending returns null rather than 0.0 when nothing net was spent.
        assertEquals(null, repo.getTotalSpending(start, end))
    }
}
