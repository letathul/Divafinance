package com.divafinance.feature.scanner

import com.divafinance.core.domain.usecase.scanner.GetReceiptsUseCase
import com.divafinance.core.domain.usecase.scanner.ImportStatementUseCase
import com.divafinance.core.domain.usecase.scanner.ParseReceiptUseCase
import com.divafinance.core.testing.fake.FakeAccountRepository
import com.divafinance.core.testing.fake.FakeCardRepository
import com.divafinance.core.testing.fake.FakeReceiptRepository
import com.divafinance.core.testing.fake.FakeSettingsRepository
import com.divafinance.core.testing.fake.FakeTransactionRepository
import com.divafinance.core.testing.fake.TestData
import com.divafinance.core.testing.installTestMainDispatcher
import com.divafinance.core.testing.resetTestMainDispatcher
import com.divafinance.feature.scanner.capture.ImageCaptureResult
import com.divafinance.feature.scanner.capture.ImageSource
import com.divafinance.feature.scanner.capture.TextFileResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScannerViewModelTest {

    @BeforeTest
    fun setUpMainDispatcher() = installTestMainDispatcher()

    @AfterTest
    fun tearDownMainDispatcher() = resetTestMainDispatcher()

    private val receiptRepo = FakeReceiptRepository()
    private val transactionRepo = FakeTransactionRepository()
    private val accountRepo = FakeAccountRepository()
    private val cardRepo = FakeCardRepository()

    private fun viewModel() = ScannerViewModel(
        ParseReceiptUseCase(receiptRepo, FakeSettingsRepository()),
        ImportStatementUseCase(transactionRepo),
        GetReceiptsUseCase(receiptRepo),
        accountRepo,
        cardRepo,
    )

    @Test
    fun capturingRequestsTheLauncherWithTheChosenSource() = runTest {
        val vm = viewModel()

        vm.onCaptureRequested(ImageSource.CAMERA)

        assertEquals(1, vm.uiState.value.captureRequestNonce)
        assertEquals(ImageSource.CAMERA, vm.uiState.value.pendingSource)
    }

    @Test
    fun theCaptureNonceOnlyEverIncreases() = runTest {
        // Restarting at zero would make the screen's LaunchedEffect fire on a value it has
        // already handled, reopening the camera behind the user.
        val vm = viewModel()

        vm.onCaptureRequested(ImageSource.CAMERA)
        vm.onImageCaptured(ImageCaptureResult.Cancelled)
        vm.onCaptureRequested(ImageSource.PHOTO_LIBRARY)

        assertEquals(2, vm.uiState.value.captureRequestNonce)
    }

    @Test
    fun aCapturedImageIsStoredAndOffersItForReview() = runTest {
        val vm = viewModel()

        vm.onCaptureRequested(ImageSource.CAMERA)
        vm.onImageCaptured(ImageCaptureResult.Success("/tmp/receipt.jpg"))
        advanceUntilIdle()

        val stored = receiptRepo.getReceipts().single()
        assertEquals("/tmp/receipt.jpg", stored.imagePath)
        assertEquals(stored.id, vm.uiState.value.reviewReceiptId)
        assertNull(vm.uiState.value.pendingSource)
    }

    @Test
    fun cancellingChangesNothingButClearsThePendingSource() = runTest {
        val vm = viewModel()

        vm.onCaptureRequested(ImageSource.CAMERA)
        vm.onImageCaptured(ImageCaptureResult.Cancelled)
        advanceUntilIdle()

        assertTrue(receiptRepo.getReceipts().isEmpty())
        assertNull(vm.uiState.value.reviewReceiptId)
        assertNull(vm.uiState.value.error)
        assertNull(vm.uiState.value.pendingSource)
    }

    @Test
    fun aFailedCaptureSurfacesItsReason() = runTest {
        val vm = viewModel()

        vm.onCaptureRequested(ImageSource.CAMERA)
        vm.onImageCaptured(ImageCaptureResult.Failed("Camera permission denied"))
        advanceUntilIdle()

        assertEquals("Camera permission denied", vm.uiState.value.error)
        assertTrue(receiptRepo.getReceipts().isEmpty())
    }

    @Test
    fun navigatingToReviewClearsTheSignal() = runTest {
        // Otherwise pressing back off the review screen would immediately navigate into it
        // again.
        val vm = viewModel()
        vm.onImageCaptured(ImageCaptureResult.Success("/tmp/receipt.jpg"))
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.reviewReceiptId)

        vm.onReviewNavigated()

        assertNull(vm.uiState.value.reviewReceiptId)
    }

    @Test
    fun historyReflectsStoredReceipts() = runTest {
        receiptRepo.setReceipts(
            listOf(TestData.receipt(id = "r1"), TestData.receipt(id = "r2")),
        )
        val vm = viewModel()

        assertEquals(listOf("r1", "r2"), vm.receipts.first().map { it.id })
    }

    @Test
    fun switchingTabsClearsAStaleError() = runTest {
        val vm = viewModel()
        vm.onImageCaptured(ImageCaptureResult.Failed("Camera permission denied"))
        advanceUntilIdle()

        vm.switchTab(ScannerTab.IMPORT)

        assertEquals(ScannerTab.IMPORT, vm.uiState.value.currentTab)
        assertNull(vm.uiState.value.error)
    }

    // ── CSV import: untouched by this work, guarded so it stays that way ──────────────

    @Test
    fun importsACsvStatement() = runTest {
        val vm = viewModel()
        vm.updateAccountId("acc-1")
        vm.updateCsvContent("date,description,amount\n2024-06-15,Coffee,5.50")

        vm.importCsvStatement()
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.importedCount)
        assertEquals(1, transactionRepo.getAll().first().size)
    }

    @Test
    fun refusesToImportWithoutAnAccount() = runTest {
        val vm = viewModel()
        vm.updateCsvContent("date,description,amount\n2024-06-15,Coffee,5.50")

        vm.importCsvStatement()
        advanceUntilIdle()

        assertEquals("Choose an account to import into", vm.uiState.value.error)
        assertNull(vm.uiState.value.importedCount)
    }

    /**
     * The whole point of the account picker: the id is never typed, so with one account
     * there is nothing to choose and the import just works.
     */
    @Test
    fun aSingleAccountIsPreselected() = runTest {
        accountRepo.insert(TestData.account(id = "acc-1"))
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals("acc-1", vm.uiState.value.accountId)
    }

    /** With a choice to make, nothing is assumed — picking is the user's. */
    @Test
    fun severalAccountsPreselectNothing() = runTest {
        accountRepo.insert(TestData.account(id = "acc-1"))
        accountRepo.insert(TestData.account(id = "acc-2"))
        val vm = viewModel()
        advanceUntilIdle()

        assertNull(vm.uiState.value.accountId)
        assertEquals(2, vm.uiState.value.accounts.size)
    }

    @Test
    fun aPickedFileFillsTheCsvBoxAndShowsItsName() = runTest {
        val vm = viewModel()

        vm.onCsvFilePicked(
            TextFileResult.Success("june.csv", "date,description,amount\n2024-06-15,Coffee,5.50"),
        )

        assertEquals("june.csv", vm.uiState.value.csvFileName)
        assertTrue(vm.uiState.value.csvContent.startsWith("date,description,amount"))
        assertNull(vm.uiState.value.error)
    }

    /** Backing out of the picker leaves whatever was already there. */
    @Test
    fun cancellingThePickerChangesNothing() = runTest {
        val vm = viewModel()
        vm.updateCsvContent("already,typed,in")

        vm.onCsvFilePicked(TextFileResult.Cancelled)

        assertEquals("already,typed,in", vm.uiState.value.csvContent)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun aFailedPickSurfacesItsMessage() = runTest {
        val vm = viewModel()

        vm.onCsvFilePicked(TextFileResult.Failed("Couldn't read that file"))

        assertEquals("Couldn't read that file", vm.uiState.value.error)
    }

    /** Typing over a picked file drops the name, which no longer describes the content. */
    @Test
    fun editingTheCsvClearsThePickedFileName() = runTest {
        val vm = viewModel()
        vm.onCsvFilePicked(TextFileResult.Success("june.csv", "a,b,c"))

        vm.updateCsvContent("a,b,c\nd,e,f")

        assertNull(vm.uiState.value.csvFileName)
    }
}
