package com.divafinance.feature.transactions

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.divafinance.core.domain.usecase.cards.GetAllCardsUseCase
import com.divafinance.core.domain.usecase.feed.PostTransactionToFeedUseCase
import com.divafinance.core.domain.usecase.transactions.AddTransactionUseCase
import com.divafinance.core.domain.usecase.transactions.GetTransactionsUseCase
import com.divafinance.core.testing.installTestMainDispatcher
import com.divafinance.core.testing.resetTestMainDispatcher
import com.divafinance.core.testing.fake.FakeCardRepository
import com.divafinance.core.testing.fake.FakeFeedRepository
import com.divafinance.core.testing.fake.FakeTransactionRepository
import com.divafinance.core.testing.fake.TestData
import com.divafinance.core.ui.theme.DivaTheme
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TransactionListScreenTest {

    @BeforeTest
    fun setUpMainDispatcher() = installTestMainDispatcher()

    @AfterTest
    fun tearDownMainDispatcher() = resetTestMainDispatcher()

    private val txRepo = FakeTransactionRepository()
    private val cardRepo = FakeCardRepository()
    private val feedRepo = FakeFeedRepository()

    private fun viewModel() = TransactionsViewModel(
        GetTransactionsUseCase(txRepo),
        AddTransactionUseCase(txRepo, cardRepo),
        GetAllCardsUseCase(cardRepo),
        PostTransactionToFeedUseCase(feedRepo),
    )

    @Test
    fun displaysTitle() = runComposeUiTest {
        setContent {
            DivaTheme { TransactionListScreen(viewModel = viewModel()) }
        }
        onNodeWithText("Transactions").assertIsDisplayed()
    }

    @Test
    fun rendersAStoredTransaction() = runComposeUiTest {
        txRepo.setTransactions(
            listOf(TestData.transaction(id = "t1", merchantName = "Blue Bottle"))
        )

        setContent {
            DivaTheme { TransactionListScreen(viewModel = viewModel()) }
        }

        waitUntil {
            onAllNodesWithText("Blue Bottle").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithText("Blue Bottle").assertIsDisplayed()
    }
}
