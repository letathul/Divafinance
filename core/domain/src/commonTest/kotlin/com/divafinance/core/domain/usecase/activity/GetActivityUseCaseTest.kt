package com.divafinance.core.domain.usecase.activity

import com.divafinance.core.model.FeedPost
import com.divafinance.core.model.enums.FeedPostType
import com.divafinance.core.model.enums.LedgerEntryKind
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.testing.fake.FakeFeedRepository
import com.divafinance.core.testing.fake.FakeLedgerRepository
import com.divafinance.core.testing.fake.FakePersonRepository
import com.divafinance.core.testing.fake.FakeTransactionRepository
import com.divafinance.core.testing.fake.TestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GetActivityUseCaseTest {

    private val txRepo = FakeTransactionRepository()
    private val ledgerRepo = FakeLedgerRepository()
    private val personRepo = FakePersonRepository()
    private val feedRepo = FakeFeedRepository()

    private val useCase = GetActivityUseCase(txRepo, ledgerRepo, personRepo, feedRepo)

    private fun post(
        id: String,
        type: FeedPostType,
        title: String = "Title",
        body: String = "Body",
    ) = FeedPost(id = id, type = type, title = title, body = body, createdAt = TestData.now)

    private suspend fun items(filter: ActivityFilter = ActivityFilter()) = useCase(filter).first()

    @Test
    fun isEmptyWithNoData() = runTest {
        assertTrue(items().isEmpty())
    }

    @Test
    fun mergesAllThreeKindsIntoOneStream() = runTest {
        txRepo.setTransactions(listOf(TestData.transaction(id = "t1")))
        personRepo.setPeople(listOf(TestData.person(id = "p1", name = "Sam")))
        ledgerRepo.setEntries(listOf(TestData.ledgerEntry(id = "l1", personId = "p1")))
        feedRepo.setPosts(listOf(post("f1", FeedPostType.BOT_INSIGHT)))

        assertEquals(
            setOf(ActivityKind.SPENDING, ActivityKind.DEBTS, ActivityKind.INSIGHTS),
            items().map { it.kind }.toSet(),
        )
    }

    /**
     * Every transaction also gets a FeedPost of type TRANSACTION written by
     * PostTransactionToFeedUseCase. Including those would show every spend twice.
     */
    @Test
    fun doesNotShowATransactionTwiceViaItsFeedEcho() = runTest {
        txRepo.setTransactions(listOf(TestData.transaction(id = "t1")))
        feedRepo.setPosts(
            listOf(
                post("f1", FeedPostType.TRANSACTION),
                post("f2", FeedPostType.BOT_INSIGHT),
            )
        )

        val result = items()
        assertEquals(1, result.count { it.kind == ActivityKind.SPENDING })
        assertEquals(1, result.count { it.kind == ActivityKind.INSIGHTS })
        assertEquals(2, result.size)
    }

    @Test
    fun ordersNewestFirst() = runTest {
        txRepo.setTransactions(
            listOf(
                TestData.transaction(id = "old", date = LocalDate(2026, 1, 1)),
                TestData.transaction(id = "new", date = LocalDate(2026, 6, 1)),
            )
        )

        assertEquals(listOf("spend:new", "spend:old"), items().map { it.id })
    }

    @Test
    fun attachesThePersonToADebt() = runTest {
        personRepo.setPeople(listOf(TestData.person(id = "p1", name = "Sam")))
        ledgerRepo.setEntries(listOf(TestData.ledgerEntry(id = "l1", personId = "p1")))

        val debt = items().single() as ActivityItem.Debt
        assertEquals("Sam", debt.personName)
    }

    /** A debt whose person record has gone must still render rather than vanish. */
    @Test
    fun survivesAMissingPerson() = runTest {
        ledgerRepo.setEntries(listOf(TestData.ledgerEntry(id = "l1", personId = "gone")))

        val debt = items().single() as ActivityItem.Debt
        assertNull(debt.person)
        assertEquals("Someone", debt.personName)
    }

    @Test
    fun exposesTheOwnShareOfASplitSpend() = runTest {
        txRepo.setTransactions(
            listOf(TestData.transaction(id = "t1", amount = 120.0, othersShare = 80.0))
        )

        val spend = items().single() as ActivityItem.Spend
        assertTrue(spend.isShared)
        assertEquals(40.0, spend.ownShare)
    }

    @Test
    fun anOrdinarySpendIsNotMarkedShared() = runTest {
        txRepo.setTransactions(listOf(TestData.transaction(id = "t1", amount = 50.0)))

        val spend = items().single() as ActivityItem.Spend
        assertTrue(!spend.isShared)
        assertEquals(50.0, spend.ownShare)
    }

    @Test
    fun debtsCarryTheirDirection() = runTest {
        ledgerRepo.setEntries(
            listOf(
                TestData.ledgerEntry(id = "l1", amount = 20.0, kind = LedgerEntryKind.LENT),
                TestData.ledgerEntry(id = "l2", amount = 20.0, kind = LedgerEntryKind.BORROWED),
            )
        )

        val signs = items().filterIsInstance<ActivityItem.Debt>().map { it.signedAmount }
        assertEquals(setOf(20.0, -20.0), signs.toSet())
    }

    // --- filtering ----------------------------------------------------------

    @Test
    fun filtersByKind() = runTest {
        txRepo.setTransactions(listOf(TestData.transaction(id = "t1")))
        ledgerRepo.setEntries(listOf(TestData.ledgerEntry(id = "l1")))
        feedRepo.setPosts(listOf(post("f1", FeedPostType.BOT_INSIGHT)))

        val onlyDebts = items(ActivityFilter(kinds = setOf(ActivityKind.DEBTS)))
        assertEquals(listOf("debt:l1"), onlyDebts.map { it.id })
    }

    @Test
    fun filtersByDateRange() = runTest {
        txRepo.setTransactions(
            listOf(
                TestData.transaction(id = "jan", date = LocalDate(2026, 1, 15)),
                TestData.transaction(id = "jun", date = LocalDate(2026, 6, 15)),
                TestData.transaction(id = "dec", date = LocalDate(2026, 12, 15)),
            )
        )

        val spring = items(
            ActivityFilter(from = LocalDate(2026, 6, 1), to = LocalDate(2026, 6, 30)),
        )
        assertEquals(listOf("spend:jun"), spring.map { it.id })
    }

    @Test
    fun dateBoundsAreInclusive() = runTest {
        txRepo.setTransactions(
            listOf(TestData.transaction(id = "edge", date = LocalDate(2026, 6, 1)))
        )

        val result = items(
            ActivityFilter(from = LocalDate(2026, 6, 1), to = LocalDate(2026, 6, 1)),
        )
        assertEquals(1, result.size)
    }

    @Test
    fun searchesAcrossEveryKind() = runTest {
        txRepo.setTransactions(
            listOf(TestData.transaction(id = "t1", merchantName = "Blue Bottle"))
        )
        personRepo.setPeople(listOf(TestData.person(id = "p1", name = "Bluebell")))
        ledgerRepo.setEntries(listOf(TestData.ledgerEntry(id = "l1", personId = "p1")))
        feedRepo.setPosts(listOf(post("f1", FeedPostType.BOT_INSIGHT, title = "Blue week")))

        assertEquals(3, items(ActivityFilter(query = "blue")).size)
    }

    @Test
    fun searchIsCaseInsensitiveAndTrimmed() = runTest {
        txRepo.setTransactions(
            listOf(TestData.transaction(id = "t1", merchantName = "Blue Bottle"))
        )

        assertEquals(1, items(ActivityFilter(query = "  BLUE  ")).size)
    }

    @Test
    fun searchMatchesACategoryName() = runTest {
        txRepo.setTransactions(
            listOf(
                TestData.transaction(
                    id = "t1",
                    merchantName = null,
                    category = SpendingCategory.GROCERIES,
                )
            )
        )

        assertEquals(1, items(ActivityFilter(query = "groceries")).size)
    }

    @Test
    fun reportsWhetherAFilterIsActive() {
        assertTrue(!ActivityFilter().isFiltered)
        assertTrue(ActivityFilter(query = "x").isFiltered)
        assertTrue(ActivityFilter(kinds = setOf(ActivityKind.DEBTS)).isFiltered)
        assertTrue(ActivityFilter(from = LocalDate(2026, 1, 1)).isFiltered)
    }
}
