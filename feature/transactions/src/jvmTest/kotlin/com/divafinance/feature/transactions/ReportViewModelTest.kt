package com.divafinance.feature.transactions

import com.divafinance.core.domain.usecase.activity.GetActivityUseCase
import com.divafinance.core.domain.usecase.cards.GetAllCardsUseCase
import com.divafinance.core.domain.usecase.reports.ReportPeriod
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import com.divafinance.core.testing.fake.FakeCardRepository
import com.divafinance.core.testing.fake.FakeFeedRepository
import com.divafinance.core.testing.fake.FakeLedgerRepository
import com.divafinance.core.testing.fake.FakePersonRepository
import com.divafinance.core.testing.fake.FakeTransactionRepository
import com.divafinance.core.testing.fake.TestData
import com.divafinance.core.testing.installTestMainDispatcher
import com.divafinance.core.testing.resetTestMainDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ReportViewModelTest {

    @BeforeTest
    fun setUpMainDispatcher() = installTestMainDispatcher()

    @AfterTest
    fun tearDownMainDispatcher() = resetTestMainDispatcher()

    private val transactionRepo = FakeTransactionRepository()
    private val ledgerRepo = FakeLedgerRepository()
    private val personRepo = FakePersonRepository()
    private val feedRepo = FakeFeedRepository()
    private val cardRepo = FakeCardRepository()

    private val anchor = LocalDate(2026, 8, 17)

    /**
     * uiState is a `stateIn(WhileSubscribed)` flow, so it stays on its initial value until
     * something collects it. Tests have to subscribe the way the UI does.
     */
    private fun TestScope.viewModel(period: ReportPeriod = ReportPeriod.MONTH): ReportViewModel {
        val vm = ReportViewModel(
            period = period,
            anchor = anchor,
            getActivity = GetActivityUseCase(transactionRepo, ledgerRepo, personRepo, feedRepo),
            getAllCards = GetAllCardsUseCase(cardRepo),
        )
        backgroundScope.launch(Dispatchers.Main) { vm.uiState.collect { } }
        return vm
    }

    private fun seedSpendAndSalary() {
        transactionRepo.setTransactions(
            listOf(
                TestData.transaction(id = "spend", amount = 100.0, date = anchor),
                TestData.transaction(
                    id = "salary",
                    amount = 4200.0,
                    type = TransactionType.CREDIT,
                    date = anchor,
                ),
            )
        )
    }

    /**
     * The header reads "Total spent", so income must not land in it. Without an explicit
     * type filter a report means expenses — otherwise a month reads as one figure on the
     * feed and a larger one in its own report.
     */
    @Test
    fun incomeIsExcludedFromTheTotalByDefault() = runTest {
        seedSpendAndSalary()
        val vm = viewModel()

        assertEquals(100.0, vm.uiState.value.total)
        assertEquals(1, vm.uiState.value.transactionCount)
    }

    /** Asking for income explicitly still works — the default is a default, not a rule. */
    @Test
    fun incomeIsReachableThroughTheTypeFilter() = runTest {
        seedSpendAndSalary()
        val vm = viewModel()

        vm.updateFilter { it.copy(types = setOf(TransactionType.CREDIT)) }

        assertEquals(4200.0, vm.uiState.value.total)
        assertEquals(1, vm.uiState.value.transactionCount)
    }

    /** A split bill contributes only the user's own share, matching the feed. */
    @Test
    fun onlyTheUsersShareOfASplitCounts() = runTest {
        transactionRepo.setTransactions(
            listOf(
                TestData.transaction(
                    id = "dinner",
                    amount = 120.0,
                    othersShare = 80.0,
                    category = SpendingCategory.DINING,
                    date = anchor,
                )
            )
        )
        val vm = viewModel()

        assertEquals(40.0, vm.uiState.value.total)
        assertEquals(40.0, vm.uiState.value.breakdown.single().total)
    }

    /** Dates outside the anchored period land in the comparison, not the total. */
    @Test
    fun onlyTheAnchoredPeriodIsIncluded() = runTest {
        transactionRepo.setTransactions(
            listOf(
                TestData.transaction(id = "august", amount = 10.0, date = LocalDate(2026, 8, 2)),
                TestData.transaction(id = "july", amount = 999.0, date = LocalDate(2026, 7, 30)),
            )
        )
        val vm = viewModel()

        assertEquals(10.0, vm.uiState.value.total)
        assertEquals(999.0, vm.uiState.value.previousTotal)
    }

    /** Clearing a filter and never setting one are the same state. */
    @Test
    fun clearingFiltersRestoresTheDefaultTotal() = runTest {
        seedSpendAndSalary()
        val vm = viewModel()

        vm.updateFilter { it.copy(categories = setOf(SpendingCategory.TRAVEL)) }
        assertEquals(0.0, vm.uiState.value.total)

        vm.clearFilters()
        assertEquals(100.0, vm.uiState.value.total)
    }
}
