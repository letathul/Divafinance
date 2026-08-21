package com.divafinance.feature.scanner

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.divafinance.core.domain.usecase.scanner.GetReceiptsUseCase
import com.divafinance.core.domain.usecase.scanner.ImportStatementUseCase
import com.divafinance.core.domain.usecase.scanner.ParseReceiptUseCase
import com.divafinance.core.model.enums.ReceiptStatus
import com.divafinance.core.testing.fake.FakeReceiptRepository
import com.divafinance.core.testing.fake.FakeTransactionRepository
import com.divafinance.core.testing.fake.TestData
import com.divafinance.core.testing.installTestMainDispatcher
import com.divafinance.core.testing.resetTestMainDispatcher
import com.divafinance.core.ui.theme.DivaTheme
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ScannerScreenTest {

    @BeforeTest
    fun setUpMainDispatcher() = installTestMainDispatcher()

    @AfterTest
    fun tearDownMainDispatcher() = resetTestMainDispatcher()

    private val receiptRepo = FakeReceiptRepository()
    private val transactionRepo = FakeTransactionRepository()

    private fun viewModel() = ScannerViewModel(
        ParseReceiptUseCase(receiptRepo),
        ImportStatementUseCase(transactionRepo),
        GetReceiptsUseCase(receiptRepo),
    )

    @Test
    fun showsAllThreeTabs() = runComposeUiTest {
        setContent { DivaTheme { ScannerScreen(viewModel = viewModel()) } }

        onNodeWithText("Scan").assertIsDisplayed()
        onNodeWithText("History").assertIsDisplayed()
        onNodeWithText("Import").assertIsDisplayed()
    }

    @Test
    fun reportsThatScanningIsUnavailableOnThisHost() = runComposeUiTest {
        // The desktop OcrEngine reports itself unavailable, which is the same path a device
        // without the scanner module takes. It must say so rather than offer a fake scan.
        setContent { DivaTheme { ScannerScreen(viewModel = viewModel()) } }

        // Meta uppercases its text, so match case-insensitively.
        onNodeWithText(
            "Receipt scanning isn't available on this device",
            substring = true,
            ignoreCase = true,
        ).assertIsDisplayed()
    }

    @Test
    fun showsTheEmptyStateWhenNothingHasBeenScanned() = runComposeUiTest {
        setContent { DivaTheme { ScannerScreen(viewModel = viewModel()) } }

        onNodeWithText("History").performClick()

        onNodeWithText("No scans yet").assertIsDisplayed()
    }

    @Test
    fun listsStoredReceipts() = runComposeUiTest {
        receiptRepo.setReceipts(
            listOf(TestData.receipt(id = "r1", merchantName = "Blue Bottle", totalAmount = 6.25)),
        )
        setContent { DivaTheme { ScannerScreen(viewModel = viewModel()) } }

        onNodeWithText("History").performClick()

        onNodeWithText("Blue Bottle").assertIsDisplayed()
    }

    @Test
    fun tappingAPendingReceiptOpensItForReview() = runComposeUiTest {
        receiptRepo.setReceipts(
            listOf(
                TestData.receipt(
                    id = "r1",
                    merchantName = "Blue Bottle",
                    status = ReceiptStatus.PENDING,
                ),
            ),
        )
        var reviewed: String? = null
        setContent {
            DivaTheme {
                ScannerScreen(onReviewReceipt = { reviewed = it }, viewModel = viewModel())
            }
        }

        onNodeWithText("History").performClick()
        onNodeWithText("Blue Bottle").performClick()

        assertEquals("r1", reviewed)
    }

    @Test
    fun tappingAProcessedReceiptOpensItsTransactionInstead() = runComposeUiTest {
        receiptRepo.setReceipts(
            listOf(
                TestData.receipt(
                    id = "r1",
                    merchantName = "Blue Bottle",
                    transactionId = "t1",
                    status = ReceiptStatus.PROCESSED,
                ),
            ),
        )
        var openedTransaction: String? = null
        var reviewed: String? = null
        setContent {
            DivaTheme {
                ScannerScreen(
                    onReviewReceipt = { reviewed = it },
                    onOpenTransaction = { openedTransaction = it },
                    viewModel = viewModel(),
                )
            }
        }

        onNodeWithText("History").performClick()
        onNodeWithText("Blue Bottle").performClick()

        assertEquals("t1", openedTransaction)
        assertEquals(null, reviewed)
    }

    @Test
    fun stillOffersTheCsvImportTab() = runComposeUiTest {
        setContent { DivaTheme { ScannerScreen(viewModel = viewModel()) } }

        onNodeWithText("Import").performClick()

        onNodeWithText("CSV Statement Import").assertIsDisplayed()
    }
}
