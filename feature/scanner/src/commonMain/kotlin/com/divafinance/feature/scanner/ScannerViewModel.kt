package com.divafinance.feature.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divafinance.core.domain.usecase.scanner.ImportStatementUseCase
import com.divafinance.core.domain.usecase.scanner.ParseReceiptUseCase
import com.divafinance.core.model.Receipt
import com.divafinance.feature.scanner.ocr.OcrEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScannerUiState(
    val currentTab: ScannerTab = ScannerTab.RECEIPT,
    val isProcessing: Boolean = false,
    val lastReceipt: Receipt? = null,
    val importedCount: Int? = null,
    val error: String? = null,
    val csvContent: String = "",
    val accountId: String = "",
    val cardId: String = "",
)

enum class ScannerTab { RECEIPT, IMPORT }

class ScannerViewModel(
    private val parseReceipt: ParseReceiptUseCase,
    private val importStatement: ImportStatementUseCase,
    private val ocrEngine: OcrEngine = OcrEngine(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun switchTab(tab: ScannerTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab, error = null)
    }

    fun isOcrAvailable(): Boolean = ocrEngine.isAvailable()

    fun scanImage(imagePath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, error = null)
            try {
                val ocrResult = ocrEngine.recognizeText(imagePath)
                val receipt = parseReceipt(imagePath, ocrResult.fullText)
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    lastReceipt = receipt,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = e.message ?: "Failed to scan image",
                )
            }
        }
    }

    fun processOcrResult(imagePath: String, ocrText: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, error = null)
            try {
                val receipt = parseReceipt(imagePath, ocrText)
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    lastReceipt = receipt,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = e.message ?: "Failed to process receipt",
                )
            }
        }
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
            lastReceipt = null,
            importedCount = null,
            error = null,
        )
    }
}
