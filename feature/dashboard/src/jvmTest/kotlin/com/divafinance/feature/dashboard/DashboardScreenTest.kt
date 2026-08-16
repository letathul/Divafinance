package com.divafinance.feature.dashboard

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.divafinance.core.domain.usecase.cards.GetAllCardsUseCase
import com.divafinance.core.domain.usecase.transactions.GetTransactionsUseCase
import com.divafinance.core.testing.installTestMainDispatcher
import com.divafinance.core.testing.resetTestMainDispatcher
import com.divafinance.core.testing.fake.FakeCardRepository
import com.divafinance.core.testing.fake.FakeTransactionRepository
import com.divafinance.core.testing.fake.TestData
import com.divafinance.core.ui.theme.DivaTheme
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class DashboardScreenTest {

    @BeforeTest
    fun setUpMainDispatcher() = installTestMainDispatcher()

    @AfterTest
    fun tearDownMainDispatcher() = resetTestMainDispatcher()

    private val txRepo = FakeTransactionRepository()
    private val cardRepo = FakeCardRepository()

    private fun viewModel() = DashboardViewModel(
        GetTransactionsUseCase(txRepo),
        GetAllCardsUseCase(cardRepo),
    )

    @Test
    fun displaysTitle() = runComposeUiTest {
        setContent {
            DivaTheme { DashboardScreen(viewModel = viewModel()) }
        }
        onNodeWithText("Dashboard").assertIsDisplayed()
    }

    @Test
    fun showsQuickActions() = runComposeUiTest {
        setContent {
            DivaTheme { DashboardScreen(viewModel = viewModel()) }
        }
        onNodeWithText("My Cards").assertIsDisplayed()
    }

    @Test
    fun rendersARecentTransaction() = runComposeUiTest {
        txRepo.setTransactions(
            listOf(TestData.transaction(id = "t1", merchantName = "Blue Bottle"))
        )

        setContent {
            DivaTheme { DashboardScreen(viewModel = viewModel()) }
        }

        // recentTransactions is a stateIn(WhileSubscribed) flow, so it emits one
        // coroutine turn after the first subscriber composes rather than synchronously.
        waitUntil {
            onAllNodesWithText("Blue Bottle").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithText("Blue Bottle").assertIsDisplayed()
    }
}
