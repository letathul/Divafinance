package com.divafinance.core.domain.usecase.people

import com.divafinance.core.domain.usecase.transactions.AddTransactionUseCase
import com.divafinance.core.model.enums.LedgerEntryKind
import com.divafinance.core.testing.fake.FakeCardRepository
import com.divafinance.core.testing.fake.FakeLedgerRepository
import com.divafinance.core.testing.fake.FakePersonRepository
import com.divafinance.core.testing.fake.FakeTransactionRepository
import com.divafinance.core.testing.fake.TestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SaveSplitTransactionUseCaseTest {

    private val txRepo = FakeTransactionRepository()
    private val cardRepo = FakeCardRepository()
    private val personRepo = FakePersonRepository()
    private val ledgerRepo = FakeLedgerRepository()

    private val useCase = SaveSplitTransactionUseCase(
        AddTransactionUseCase(txRepo, cardRepo),
        personRepo,
        ledgerRepo,
    )

    /** A £120 dinner three ways: £40 mine, £80 owed by two others. */
    private fun dinner(amount: Double = 120.0, othersShare: Double = 80.0) =
        TestData.transaction(
            id = "dinner",
            amount = amount,
            othersShare = othersShare,
            merchantName = "Trattoria",
            cardId = null,
        )

    private fun shares(vararg names: String, each: Double = 40.0) =
        names.map { SplitShareInput(personId = null, name = it, amount = each) }

    @Test
    fun recordsTheTransactionAtTheFullAmount() = runTest {
        useCase(dinner(), shares("Sam", "Alex"))

        val stored = txRepo.getAll().first().single()
        assertEquals(120.0, stored.amount)
        assertEquals(80.0, stored.othersShare)
    }

    @Test
    fun createsOneLedgerEntryPerPerson() = runTest {
        useCase(dinner(), shares("Sam", "Alex"))

        val entries = ledgerRepo.getAll().first()
        assertEquals(2, entries.size)
        assertTrue(entries.all { it.kind == LedgerEntryKind.LENT })
        assertTrue(entries.all { it.amount == 40.0 })
    }

    @Test
    fun linksEveryEntryToTheTransaction() = runTest {
        useCase(dinner(), shares("Sam", "Alex"))

        assertEquals(2, ledgerRepo.getByTransactionId("dinner").size)
    }

    @Test
    fun createsPeopleThatDoNotExistYet() = runTest {
        useCase(dinner(), shares("Sam", "Alex"))

        assertEquals(setOf("Sam", "Alex"), personRepo.getAll().first().map { it.name }.toSet())
    }

    /** Otherwise every dinner with Sam creates another Sam with his own balance. */
    @Test
    fun reusesAnExistingPersonByName() = runTest {
        personRepo.setPeople(listOf(TestData.person(id = "p1", name = "Sam")))

        useCase(dinner(), shares("Sam", "Alex"))

        assertEquals(2, personRepo.count())
        assertEquals("p1", ledgerRepo.getAll().first().first { it.amount == 40.0 }.personId)
    }

    @Test
    fun matchesAnExistingPersonIgnoringCaseAndSpacing() = runTest {
        personRepo.setPeople(listOf(TestData.person(id = "p1", name = "Sam")))

        useCase(dinner(othersShare = 40.0), listOf(SplitShareInput(null, "  sam  ", 40.0)))

        assertEquals(1, personRepo.count())
        assertEquals("p1", ledgerRepo.getAll().first().single().personId)
    }

    @Test
    fun prefersAnExplicitPersonId() = runTest {
        personRepo.setPeople(
            listOf(
                TestData.person(id = "p1", name = "Sam"),
                TestData.person(id = "p2", name = "Samantha"),
            )
        )

        useCase(
            dinner(othersShare = 40.0),
            listOf(SplitShareInput(personId = "p2", name = "Sam", amount = 40.0)),
        )

        assertEquals("p2", ledgerRepo.getAll().first().single().personId)
    }

    @Test
    fun carriesTheMerchantAndDateOntoTheEntries() = runTest {
        val tx = dinner(othersShare = 40.0)
        useCase(tx, listOf(SplitShareInput(null, "Sam", 40.0)))

        val entry = ledgerRepo.getAll().first().single()
        assertEquals("Trattoria", entry.note)
        assertEquals(tx.date, entry.date)
    }

    @Test
    fun skipsParticipantsWhoOweNothing() = runTest {
        useCase(
            dinner(othersShare = 40.0),
            listOf(SplitShareInput(null, "Sam", 40.0), SplitShareInput(null, "Guest", 0.0)),
        )

        assertEquals(1, ledgerRepo.count())
        // A zero-share guest is not worth creating a person record for either.
        assertEquals(listOf("Sam"), personRepo.getAll().first().map { it.name })
    }

    // --- the invariant SQLite cannot enforce --------------------------------

    @Test
    fun rejectsSharesThatDoNotAddUpToTheOthersShare() = runTest {
        assertFailsWith<IllegalArgumentException> {
            useCase(dinner(othersShare = 80.0), shares("Sam", "Alex", each = 30.0))
        }
        assertEquals(0, txRepo.count())
        assertEquals(0, ledgerRepo.count())
    }

    @Test
    fun rejectsAnOthersShareLargerThanTheAmount() = runTest {
        assertFailsWith<IllegalArgumentException> {
            useCase(dinner(amount = 50.0, othersShare = 80.0), shares("Sam", "Alex"))
        }
    }

    @Test
    fun rejectsANegativeOthersShare() = runTest {
        assertFailsWith<IllegalArgumentException> {
            useCase(dinner(othersShare = -10.0), emptyList())
        }
    }

    @Test
    fun rejectsANegativeShare() = runTest {
        assertFailsWith<IllegalArgumentException> {
            useCase(
                dinner(othersShare = 40.0),
                listOf(SplitShareInput(null, "Sam", 80.0), SplitShareInput(null, "Alex", -40.0)),
            )
        }
    }

    /** Shares come from integer-cent allocation, so cent-level rounding must be tolerated. */
    @Test
    fun toleratesCentRoundingInTheShareTotal() = runTest {
        // £100 three ways: 33.33 + 33.33 owed, 33.34 kept.
        useCase(
            TestData.transaction(id = "t", amount = 100.0, othersShare = 66.66, cardId = null),
            listOf(SplitShareInput(null, "Sam", 33.33), SplitShareInput(null, "Alex", 33.33)),
        )

        assertEquals(2, ledgerRepo.count())
    }

    @Test
    fun aSplitWithNobodyElseIsJustATransaction() = runTest {
        useCase(dinner(amount = 40.0, othersShare = 0.0), emptyList())

        assertEquals(1, txRepo.count())
        assertEquals(0, ledgerRepo.count())
    }

    /** The whole point: a split must not inflate the user's own spending. */
    @Test
    fun onlyTheUsersShareCountsAsSpending() = runTest {
        useCase(dinner(), shares("Sam", "Alex"))

        val stored = txRepo.getAll().first().single()
        assertEquals(40.0, stored.amount - stored.othersShare)
    }

    /** A card is charged the whole bill, not just the payer's portion. */
    @Test
    fun theCardIsChargedTheFullBill() = runTest {
        cardRepo.insert(TestData.card(id = "c1", currentBalance = 0.0))

        useCase(
            TestData.transaction(id = "t", amount = 120.0, othersShare = 80.0, cardId = "c1"),
            shares("Sam", "Alex"),
        )

        assertEquals(120.0, cardRepo.getById("c1")?.currentBalance)
    }
}
