package com.divafinance.core.domain.usecase.people

import com.divafinance.core.model.enums.LedgerEntryKind
import com.divafinance.core.testing.fake.FakeLedgerRepository
import com.divafinance.core.testing.fake.FakePersonRepository
import com.divafinance.core.testing.fake.TestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GetPeopleBalancesUseCaseTest {

    private val personRepo = FakePersonRepository()
    private val ledgerRepo = FakeLedgerRepository()
    private val useCase = GetPeopleBalancesUseCase(personRepo, ledgerRepo)

    private fun seedSam() {
        personRepo.setPeople(listOf(TestData.person(id = "p1", name = "Sam")))
    }

    private suspend fun balanceOfSam(): PersonBalance = useCase().first().single()

    @Test
    fun aPersonWithNoEntriesIsSettled() = runTest {
        seedSam()

        val result = balanceOfSam()
        assertEquals(0.0, result.balance)
        assertTrue(result.isSettled)
        assertEquals(0, result.entryCount)
    }

    @Test
    fun lendingMeansTheyOweYou() = runTest {
        seedSam()
        ledgerRepo.setEntries(
            listOf(TestData.ledgerEntry(personId = "p1", amount = 50.0, kind = LedgerEntryKind.LENT))
        )

        val result = balanceOfSam()
        assertEquals(50.0, result.balance)
        assertTrue(result.theyOweMe)
        assertFalse(result.iOweThem)
    }

    @Test
    fun borrowingMeansYouOweThem() = runTest {
        seedSam()
        ledgerRepo.setEntries(
            listOf(TestData.ledgerEntry(personId = "p1", amount = 30.0, kind = LedgerEntryKind.BORROWED))
        )

        val result = balanceOfSam()
        assertEquals(-30.0, result.balance)
        assertTrue(result.iOweThem)
    }

    /** The case the signed-kind model exists for: no special handling needed. */
    @Test
    fun aPartialRepaymentReducesTheBalance() = runTest {
        seedSam()
        ledgerRepo.setEntries(
            listOf(
                TestData.ledgerEntry(id = "l1", personId = "p1", amount = 100.0, kind = LedgerEntryKind.LENT),
                TestData.ledgerEntry(id = "l2", personId = "p1", amount = 40.0, kind = LedgerEntryKind.REPAID_TO_ME),
            )
        )

        val result = balanceOfSam()
        assertEquals(60.0, result.balance)
        assertTrue(result.theyOweMe)
    }

    @Test
    fun repayingInFullSettlesTheBalance() = runTest {
        seedSam()
        ledgerRepo.setEntries(
            listOf(
                TestData.ledgerEntry(id = "l1", personId = "p1", amount = 100.0, kind = LedgerEntryKind.LENT),
                TestData.ledgerEntry(id = "l2", personId = "p1", amount = 100.0, kind = LedgerEntryKind.REPAID_TO_ME),
            )
        )

        assertTrue(balanceOfSam().isSettled)
    }

    @Test
    fun overRepaymentFlipsTheDirection() = runTest {
        seedSam()
        ledgerRepo.setEntries(
            listOf(
                TestData.ledgerEntry(id = "l1", personId = "p1", amount = 50.0, kind = LedgerEntryKind.LENT),
                TestData.ledgerEntry(id = "l2", personId = "p1", amount = 80.0, kind = LedgerEntryKind.REPAID_TO_ME),
            )
        )

        val result = balanceOfSam()
        assertEquals(-30.0, result.balance)
        assertTrue(result.iOweThem)
    }

    @Test
    fun debtsInBothDirectionsNetOff() = runTest {
        seedSam()
        ledgerRepo.setEntries(
            listOf(
                TestData.ledgerEntry(id = "l1", personId = "p1", amount = 70.0, kind = LedgerEntryKind.LENT),
                TestData.ledgerEntry(id = "l2", personId = "p1", amount = 20.0, kind = LedgerEntryKind.BORROWED),
            )
        )

        assertEquals(50.0, balanceOfSam().balance)
    }

    /**
     * Amounts are stored as doubles, so a sequence that should cancel can leave a residue
     * far too small to display. Settled must be a tolerance, not an equality check.
     */
    @Test
    fun floatingPointResidueStillCountsAsSettled() = runTest {
        seedSam()
        ledgerRepo.setEntries(
            listOf(
                TestData.ledgerEntry(id = "l1", personId = "p1", amount = 0.1, kind = LedgerEntryKind.LENT),
                TestData.ledgerEntry(id = "l2", personId = "p1", amount = 0.2, kind = LedgerEntryKind.LENT),
                TestData.ledgerEntry(id = "l3", personId = "p1", amount = 0.3, kind = LedgerEntryKind.REPAID_TO_ME),
            )
        )

        val result = balanceOfSam()
        assertTrue(result.balance != 0.0, "expected floating-point residue, got exactly zero")
        assertTrue(result.isSettled, "residue of ${result.balance} should count as settled")
    }

    @Test
    fun entriesForOtherPeopleDoNotLeakIn() = runTest {
        personRepo.setPeople(
            listOf(TestData.person(id = "p1", name = "Sam"), TestData.person(id = "p2", name = "Alex"))
        )
        ledgerRepo.setEntries(
            listOf(
                TestData.ledgerEntry(id = "l1", personId = "p1", amount = 50.0),
                TestData.ledgerEntry(id = "l2", personId = "p2", amount = 20.0),
            )
        )

        val balances = useCase().first().associate { it.person.name to it.balance }
        assertEquals(mapOf("Sam" to 50.0, "Alex" to 20.0), balances)
    }

    @Test
    fun archivedPeopleAreHiddenByDefault() = runTest {
        personRepo.setPeople(
            listOf(
                TestData.person(id = "p1", name = "Sam"),
                TestData.person(id = "p2", name = "Gone", isArchived = true),
            )
        )

        assertEquals(listOf("Sam"), useCase().first().map { it.person.name })
        assertEquals(
            listOf("Sam", "Gone"),
            useCase(includeArchived = true).first().map { it.person.name },
        )
    }

    @Test
    fun countsEntriesPerPerson() = runTest {
        seedSam()
        ledgerRepo.setEntries(
            listOf(
                TestData.ledgerEntry(id = "l1", personId = "p1"),
                TestData.ledgerEntry(id = "l2", personId = "p1"),
            )
        )

        assertEquals(2, balanceOfSam().entryCount)
    }
}
