package com.divafinance.core.domain.usecase.people

import com.divafinance.core.model.enums.LedgerEntryKind
import com.divafinance.core.testing.fake.FakeLedgerRepository
import com.divafinance.core.testing.fake.FakePersonRepository
import com.divafinance.core.testing.fake.TestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettleUpUseCaseTest {

    private val ledgerRepo = FakeLedgerRepository()
    private val personRepo = FakePersonRepository()
    private val settleUp = SettleUpUseCase(ledgerRepo)
    private val recordDebt = RecordDebtUseCase(personRepo, ledgerRepo)

    private suspend fun balance() = ledgerRepo.getByPersonId("p1").balance()

    private fun seed(amount: Double, kind: LedgerEntryKind) {
        ledgerRepo.setEntries(
            listOf(TestData.ledgerEntry(id = "l1", personId = "p1", amount = amount, kind = kind))
        )
    }

    // --- direction is derived, not passed in --------------------------------

    @Test
    fun repayingWhatTheyOweReducesTheBalance() = runTest {
        seed(100.0, LedgerEntryKind.LENT)

        assertTrue(settleUp("p1", 40.0))

        assertEquals(60.0, balance())
        assertEquals(
            LedgerEntryKind.REPAID_TO_ME,
            ledgerRepo.getByPersonId("p1").first { it.id != "l1" }.kind,
        )
    }

    @Test
    fun repayingWhatYouOweMovesTheBalanceTowardsZero() = runTest {
        seed(100.0, LedgerEntryKind.BORROWED)

        assertTrue(settleUp("p1", 40.0))

        assertEquals(-60.0, balance())
        assertEquals(
            LedgerEntryKind.REPAID_BY_ME,
            ledgerRepo.getByPersonId("p1").first { it.id != "l1" }.kind,
        )
    }

    @Test
    fun settlingTheWholeAmountClearsIt() = runTest {
        seed(100.0, LedgerEntryKind.LENT)

        settleUp("p1", 100.0)

        assertEquals(0.0, balance())
    }

    @Test
    fun partialRepaymentsAccumulate() = runTest {
        seed(100.0, LedgerEntryKind.LENT)

        settleUp("p1", 30.0)
        settleUp("p1", 30.0)

        assertEquals(40.0, balance())
        assertEquals(3, ledgerRepo.count())
    }

    /** Repaying against nothing would create a debt the other way, which is never meant. */
    @Test
    fun refusesWhenThereIsNothingToSettle() = runTest {
        assertFalse(settleUp("p1", 20.0))
        assertEquals(0, ledgerRepo.count())
    }

    @Test
    fun refusesWhenTheBalanceIsOnlyFloatingPointResidue() = runTest {
        ledgerRepo.setEntries(
            listOf(
                TestData.ledgerEntry(id = "a", personId = "p1", amount = 0.1, kind = LedgerEntryKind.LENT),
                TestData.ledgerEntry(id = "b", personId = "p1", amount = 0.2, kind = LedgerEntryKind.LENT),
                TestData.ledgerEntry(id = "c", personId = "p1", amount = 0.3, kind = LedgerEntryKind.REPAID_TO_ME),
            )
        )

        assertFalse(settleUp("p1", 5.0))
    }

    @Test
    fun rejectsANonPositiveAmount() = runTest {
        seed(100.0, LedgerEntryKind.LENT)

        assertFailsWith<IllegalArgumentException> { settleUp("p1", 0.0) }
        assertFailsWith<IllegalArgumentException> { settleUp("p1", -10.0) }
    }

    @Test
    fun overpayingFlipsTheDirection() = runTest {
        seed(50.0, LedgerEntryKind.LENT)

        settleUp("p1", 80.0)

        assertEquals(-30.0, balance())
    }

    // --- outstanding --------------------------------------------------------

    @Test
    fun reportsWhatIsOutstanding() = runTest {
        seed(100.0, LedgerEntryKind.LENT)

        assertEquals(100.0, settleUp.outstandingFor("p1"))
    }

    @Test
    fun outstandingIsPositiveInEitherDirection() = runTest {
        seed(100.0, LedgerEntryKind.BORROWED)

        assertEquals(100.0, settleUp.outstandingFor("p1"))
    }

    @Test
    fun outstandingIsZeroWhenSettled() = runTest {
        assertEquals(0.0, settleUp.outstandingFor("p1"))
    }

    // --- recording a plain loan --------------------------------------------

    @Test
    fun recordsCashLentWithoutCreatingATransaction() = runTest {
        recordDebt(name = "Sam", amount = 20.0, kind = LedgerEntryKind.LENT)

        val entry = ledgerRepo.getAll().first().single()
        assertEquals(20.0, entry.amount)
        assertEquals(LedgerEntryKind.LENT, entry.kind)
        // Handing over cash you expect back is not spending, so nothing links to a spend.
        assertEquals(null, entry.transactionId)
    }

    @Test
    fun recordsMoneyBorrowed() = runTest {
        val person = recordDebt(name = "Alex", amount = 15.0, kind = LedgerEntryKind.BORROWED)

        assertEquals(-15.0, ledgerRepo.getByPersonId(person.id).balance())
    }

    @Test
    fun reusesAnExistingPersonWhenRecordingCash() = runTest {
        personRepo.setPeople(listOf(TestData.person(id = "p1", name = "Sam")))

        val person = recordDebt(name = "  sam ", amount = 10.0, kind = LedgerEntryKind.LENT)

        assertEquals("p1", person.id)
        assertEquals(1, personRepo.count())
    }

    @Test
    fun rejectsANonPositiveLoan() = runTest {
        assertFailsWith<IllegalArgumentException> {
            recordDebt(name = "Sam", amount = 0.0, kind = LedgerEntryKind.LENT)
        }
    }

    /** A loan then a repayment nets to nothing, through the two use cases together. */
    @Test
    fun lendingThenBeingRepaidNetsToZero() = runTest {
        val person = recordDebt(name = "Sam", amount = 25.0, kind = LedgerEntryKind.LENT)

        settleUp(person.id, 25.0)

        assertEquals(0.0, ledgerRepo.getByPersonId(person.id).balance())
    }
}
