package com.divafinance.feature.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divafinance.core.domain.usecase.scanner.GetReceiptsUseCase
import com.divafinance.core.domain.usecase.scanner.ImportStatementUseCase
import com.divafinance.core.domain.usecase.scanner.ParseReceiptUseCase
import com.divafinance.core.model.Receipt
import com.divafinance.feature.scanner.capture.ImageCaptureResult
import com.divafinance.feature.scanner.capture.ImageSource
import com.divafinance.feature.scanner.ocr.OcrEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScannerUiState(
    val currentTab: ScannerTab = ScannerTab.RECEIPT,
    val isProcessing: Boolean = false,
    val error: String? = null,
    /**
     * Bumped whenever the OS camera or picker should be launched. Carried as a value rather
     * than an event the screen could miss, and **monotonic** — restarting at zero would make
     * the launcher fire again on a value it has already handled.
     */
    val captureRequestNonce: Int = 0,
    val pendingSource: ImageSource? = null,
    /** Set once a scan has been stored; the screen navigates to the review step on it. */
    val reviewReceiptId: String? = null,
    val csvContent: String = "",
    val importedCount: Int? = null,
    val accountId: String = "",
    val cardId: String = "",
)

enum class ScannerTab { RECEIPT, HISTORY, IMPORT }

class ScannerViewModel(
    private val parseReceipt: ParseReceiptUseCase,
    private val importStatement: ImportStatementUseCase,
    getReceipts: GetReceiptsUseCase,
    private val ocrEngine: OcrEngine = OcrEngine(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    /** Newest first, straight from the database — a scan appears here the moment it is stored. */
    val receipts: StateFlow<List<Receipt>> = getReceipts()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun switchTab(tab: ScannerTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab, error = null)
    }

    fun isOcrAvailable(): Boolean = ocrEngine.isAvailable()

    /**
     * Records that the user asked for an image. The launcher itself lives in the screen —
     * Android needs an Activity result contract — so this only carries the decision across.
     */
    fun onCaptureRequested(source: ImageSource) {
        _uiState.value = _uiState.value.copy(
            pendingSource = source,
            captureRequestNonce = _uiState.value.captureRequestNonce + 1,
            error = null,
        )
    }

    fun onImageCaptured(result: ImageCaptureResult) {
        when (result) {
            is ImageCaptureResult.Cancelled ->
                _uiState.value = _uiState.value.copy(pendingSource = null)

            is ImageCaptureResult.Failed ->
                _uiState.value = _uiState.value.copy(
                    pendingSource = null,
                    isProcessing = false,
                    error = result.message,
                )

            is ImageCaptureResult.Success -> scanImage(result.path)
        }
    }

    private fun scanImage(imagePath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                error = null,
                pendingSource = null,
            )
            try {
                // An unavailable engine returns empty text rather than throwing, which
                // ParseReceiptUseCase records as a FAILED receipt — still reviewable by hand.
                val ocrResult = ocrEngine.recognizeText(imagePath)
                val receipt = parseReceipt(imagePath, ocrResult.fullText)
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    reviewReceiptId = receipt.id,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = e.message ?: "Failed to scan image",
                )
            }
        }
    }

    /** Clears the navigation signal so returning from the review step doesn't re-fire it. */
    fun onReviewNavigated() {
        _uiState.value = _uiState.value.copy(reviewReceiptId = null)
    }

    fun updateCsvContent(content: String) {
        _uiState.value = _uiState.value.copy(csvContent = content)
    }

    fun updateAccountId(id: String) {
        _uiState.value = _uiState.value.copy(accountId = id)
    }

    fun updateCardId(id: String) {
        _uiState.value = _uiState.value.copy(cardId = id)
    }

    fun importCsvStatement() {
        val state = _uiState.value
        if (state.csvContent.isBlank()) {
            _uiState.value = state.copy(error = "No CSV content provided")
            return
        }
        if (state.accountId.isBlank()) {
            _uiState.value = state.copy(error = "Account ID is required")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, error = null)
            try {
                val count = importStatement(
                    csvContent = state.csvContent,
                    accountId = state.accountId,
                    cardId = state.cardId.ifBlank { null },
                )
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    importedCount = count,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = e.message ?: "Import failed",
                )
            }
        }
    }

    fun clearResult() {
        _uiState.value = _uiState.value.copy(
            importedCount = null,
            error = null,
        )
    }
}
