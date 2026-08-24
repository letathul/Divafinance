package com.divafinance.feature.scanner.review

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.divafinance.core.domain.usecase.cards.GetAllCardsUseCase
import com.divafinance.core.domain.usecase.cards.GetBestCardForCategoryUseCase
import com.divafinance.core.domain.usecase.scanner.ConfirmReceiptUseCase
import com.divafinance.core.domain.usecase.scanner.GetReceiptUseCase
import com.divafinance.core.domain.usecase.transactions.AddTransactionUseCase
import com.divafinance.core.domain.usecase.transactions.PredictCategoryUseCase
import com.divafinance.core.testing.fake.FakeCardRepository
import com.divafinance.core.testing.fake.FakeReceiptRepository
import com.divafinance.core.testing.fake.FakeRewardRepository
import com.divafinance.core.testing.fake.FakeSettingsRepository
import com.divafinance.core.testing.fake.FakeTransactionRepository
import com.divafinance.core.testing.fake.TestData
import com.divafinance.core.testing.installTestMainDispatcher
import com.divafinance.core.testing.resetTestMainDispatcher
import com.divafinance.core.ui.theme.DivaTheme
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ReceiptReviewScreenTest {

    @BeforeTest
    fun setUpMainDispatcher() = installTestMainDispatcher()

    @AfterTest
    fun tearDownMainDispatcher() = resetTestMainDispatcher()

    private val receiptRepo = FakeReceiptRepository()
    private val transactionRepo = FakeTransactionRepository()
    private val cardRepo = FakeCardRepository()
    private val settingsRepo = FakeSettingsRepository()
    private val rewardRepo = FakeRewardRepository()

    private fun viewModel() = ReceiptReviewViewModel(
        receiptId = "r1",
        getReceipt = GetReceiptUseCase(receiptRepo),
        confirmReceipt = ConfirmReceiptUseCase(
            receiptRepo,
            AddTransactionUseCase(transactionRepo, cardRepo),
        ),
        predictCategory = PredictCategoryUseCase(transactionRepo),
        getAllCards = GetAllCardsUseCase(cardRepo),
        settingsRepository = settingsRepo,
        getBestCard = GetBestCardForCategoryUseCase(cardRepo, rewardRepo, settingsRepo),
    )

    @Test
    fun rendersWhatWasReadOffTheReceipt() = runComposeUiTest {
        receiptRepo.setReceipts(
            listOf(TestData.receipt(id = "r1", merchantName = "Blue Bottle", totalAmount = 6.25)),
        )
        setContent { DivaTheme { ReceiptReviewScreen(viewModel = viewModel()) } }

        // Twice, deliberately: the header names what was scanned and the field below it
        // is where that reading gets corrected.
        onAllNodesWithText("Blue Bottle").assertCountEquals(2)
        onNodeWithText("6.25").assertIsDisplayed()
    }

    @Test
    fun cannotSaveUntilThereIsAnAmount() = runComposeUiTest {
        receiptRepo.setReceipts(listOf(TestData.receipt(id = "r1", totalAmount = null)))
        setContent { DivaTheme { ReceiptReviewScreen(viewModel = viewModel()) } }

        onNodeWithText("Save transaction").assertIsNotEnabled()

        onNodeWithText("Amount").performTextInput("12.30")

        onNodeWithText("Save transaction").assertIsEnabled()
    }

    @Test
    fun clearingTheAmountDisablesSavingAgain() = runComposeUiTest {
        receiptRepo.setReceipts(listOf(TestData.receipt(id = "r1", totalAmount = 6.25)))
        setContent { DivaTheme { ReceiptReviewScreen(viewModel = viewModel()) } }

        onNodeWithText("6.25").performTextClearance()

        onNodeWithText("Save transaction").assertIsNotEnabled()
    }

    @Test
    fun showsThePlaceholderWhenThePlatformCannotDecodeTheImage() = runComposeUiTest {
        // rememberReceiptThumbnail returns null on this host, and that is not an error state.
        receiptRepo.setReceipts(listOf(TestData.receipt(id = "r1")))
        setContent { DivaTheme { ReceiptReviewScreen(viewModel = viewModel()) } }

        onNodeWithText("Photo attached", ignoreCase = true).assertIsDisplayed()
    }
}
