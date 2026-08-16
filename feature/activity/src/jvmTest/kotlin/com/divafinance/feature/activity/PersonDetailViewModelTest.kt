package com.divafinance.feature.activity

import com.divafinance.core.domain.usecase.people.GetPersonDetailUseCase
import com.divafinance.core.domain.usecase.people.SettleUpUseCase
import com.divafinance.core.model.enums.LedgerEntryKind
import com.divafinance.core.testing.fake.FakeLedgerRepository
import com.divafinance.core.testing.fake.FakePersonRepository
import com.divafinance.core.testing.fake.TestData
import com.divafinance.core.testing.installTestMainDispatcher
import com.divafinance.core.testing.resetTestMainDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PersonDetailViewModelTest {

    @BeforeTest
    fun setUpMainDispatcher() = installTestMainDispatcher()

    @AfterTest
    fun tearDownMainDispatcher() = resetTestMainDispatcher()

    private val personRepo = FakePersonRepository()
    private val ledgerRepo = FakeLedgerRepository()

    /**
     * uiState is a `stateIn(WhileSubscribed)` flow, so it stays on its initial value until
     * something collects it — exactly as it would with no screen attached. Tests have to
     * subscribe the way the UI does or they assert against an empty state.
     */
    private fun TestScope.viewModel(personId: String = "p1"): PersonDetailViewModel {
        val vm = PersonDetailViewModel(
            personId,
            GetPersonDetailUseCase(personRepo, ledgerRepo),
            SettleUpUseCase(ledgerRepo),
        )
        // On Main, which the harness makes unconfined, so collection starts immediately
        // rather than being queued behind runTest's standard scheduler.
        backgroundScope.launch(Dispatchers.Main) { vm.uiState.collect { } }
        return vm
    }

    private fun seedOwing(amount: Double = 100.0) {
        personRepo.setPeople(listOf(TestData.person(id = "p1", name = "Sam")))
        ledgerRepo.setEntries(
            listOf(
                TestData.ledgerEntry(
                    id = "l1", personId = "p1", amount = amount, kind = LedgerEntryKind.LENT,
                )
            )
        )
    }

    @Test
    fun showsTheBalanceAndHistory() = runTest {
        seedOwing()
        val vm = viewModel()

        val detail = assertNotNull(vm.uiState.value.detail)
        assertEquals("Sam", detail.person.name)
        assertEquals(100.0, detail.balance.balance)
        assertEquals(1, detail.entries.size)
    }

    @Test
    fun reportsAMissingPerson() = runTest {
        assertNull(viewModel("gone").uiState.value.detail)
    }

    @Test
    fun cannotSettleWithoutAnAmount() = runTest {
        seedOwing()

        assertFalse(viewModel().uiState.value.canSettle)
    }

    @Test
    fun recordsARepayment() = runTest {
        seedOwing()
        val vm = viewModel()

        vm.onSettleAmountChange("40")
        assertTrue(vm.uiState.value.canSettle)
        vm.onSettle()

        assertEquals(60.0, assertNotNull(vm.uiState.value.detail).balance.balance)
        assertEquals("", vm.uiState.value.settleAmount)
    }

    /** The screen updates itself, since the detail flow is reactive. */
    @Test
    fun theHistoryGrowsAfterSettling() = runTest {
        seedOwing()
        val vm = viewModel()

        vm.onSettleAmountChange("40")
        vm.onSettle()

        assertEquals(2, assertNotNull(vm.uiState.value.detail).entries.size)
    }

    @Test
    fun settleAllFillsInEverythingOutstanding() = runTest {
        seedOwing(amount = 73.5)
        val vm = viewModel()

        vm.onSettleAll()

        assertEquals(73.5, vm.uiState.value.settleAmountValue)
    }

    @Test
    fun settlingEverythingClearsTheBalance() = runTest {
        seedOwing()
        val vm = viewModel()

        vm.onSettleAll()
        vm.onSettle()

        assertTrue(assertNotNull(vm.uiState.value.detail).balance.isSettled)
    }

    /** Reuses the keypad's evaluator, so arithmetic works in this field too. */
    @Test
    fun acceptsArithmeticInTheAmount() = runTest {
        seedOwing()
        val vm = viewModel()

        vm.onSettleAmountChange("20+20")

        assertEquals(40.0, vm.uiState.value.settleAmountValue)
    }

    @Test
    fun rejectsAZeroAmount() = runTest {
        seedOwing()
        val vm = viewModel()

        vm.onSettleAmountChange("0")
        vm.onSettle()

        assertNotNull(vm.uiState.value.error)
        assertEquals(1, ledgerRepo.count())
    }

    @Test
    fun cannotSettleWhenAlreadyClear() = runTest {
        personRepo.setPeople(listOf(TestData.person(id = "p1", name = "Sam")))
        val vm = viewModel()

        vm.onSettleAmountChange("20")

        assertFalse(vm.uiState.value.canSettle)
    }

    @Test
    fun worksForDebtsYouOwe() = runTest {
        personRepo.setPeople(listOf(TestData.person(id = "p1", name = "Sam")))
        ledgerRepo.setEntries(
            listOf(
                TestData.ledgerEntry(
                    id = "l1", personId = "p1", amount = 50.0, kind = LedgerEntryKind.BORROWED,
                )
            )
        )
        val vm = viewModel()

        vm.onSettleAll()
        vm.onSettle()

        assertTrue(assertNotNull(vm.uiState.value.detail).balance.isSettled)
    }
}
