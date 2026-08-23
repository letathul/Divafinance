package com.divafinance.feature.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divafinance.core.domain.usecase.scanner.GetReceiptsUseCase
import com.divafinance.core.domain.usecase.scanner.OcrLine
import com.divafinance.core.domain.usecase.scanner.ImportStatementUseCase
import com.divafinance.core.domain.usecase.scanner.ParseReceiptUseCase
import com.divafinance.core.data.repository.AccountRepository
import com.divafinance.core.data.repository.CardRepository
import com.divafinance.core.model.Account
import com.divafinance.core.model.CreditCard
import com.divafinance.core.model.Receipt
import com.divafinance.feature.scanner.capture.ImageCaptureResult
import com.divafinance.feature.scanner.capture.ImageSource
import com.divafinance.feature.scanner.capture.TextFileResult
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
    /** "Reading page 2 of 3…" — null for a single page, where the spinner says enough. */
    val scanProgress: String? = null,
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
    /** What the user picked, for showing them what is about to be imported. */
    val csvFileName: String? = null,
    val importedCount: Int? = null,
    /**
     * The chosen account and card, or null for none. Ids again, but never typed — the
     * screen offers [accounts] and [cards] and stores the id behind the chosen name.
     */
    val accountId: String? = null,
    val cardId: String? = null,
    val accounts: List<Account> = emptyList(),
    val cards: List<CreditCard> = emptyList(),
)

enum class ScannerTab { RECEIPT, HISTORY, IMPORT }

class ScannerViewModel(
    private val parseReceipt: ParseReceiptUseCase,
    private val importStatement: ImportStatementUseCase,
    getReceipts: GetReceiptsUseCase,
    accountRepository: AccountRepository,
    cardRepository: CardRepository,
    private val ocrEngine: OcrEngine = OcrEngine(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    init {
        // The import form used to ask the user to type these ids by hand, which nobody
        // knows. Defaulting to the only account when there is exactly one means the common
        // case needs no choice at all.
        viewModelScope.launch {
            accountRepository.getAll().collect { accounts ->
                _uiState.value = _uiState.value.copy(
                    accounts = accounts,
                    accountId = _uiState.value.accountId
                        ?: accounts.singleOrNull()?.id,
                )
            }
        }
        viewModelScope.launch {
            cardRepository.getAll().collect { cards ->
                _uiState.value = _uiState.value.copy(cards = cards)
            }
        }
    }

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

            is ImageCaptureResult.Success -> scanImages(result.paths)
        }
    }

    /**
     * A document scan can return several pages of one receipt. They are recognised in order and
     * concatenated before a single parse, so the totals block on the last page is read in the
     * context of the merchant header on the first — parsing per page and merging afterwards
     * would give every page an equal claim to being "the" total.
     */
    private fun scanImages(imagePaths: List<String>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                error = null,
                pendingSource = null,
                scanProgress = progressLabel(0, imagePaths.size),
            )
            try {
                val texts = mutableListOf<String>()
                val lines = mutableListOf<OcrLine>()
                imagePaths.forEachIndexed { index, path ->
                    _uiState.value = _uiState.value.copy(
                        scanProgress = progressLabel(index, imagePaths.size),
                    )
                    // An unavailable engine returns empty text rather than throwing, which
                    // ParseReceiptUseCase records as a FAILED receipt — still reviewable by hand.
                    val ocrResult = ocrEngine.recognizeText(path)
                    texts += ocrResult.fullText
                    // Only single-page geometry is meaningful: two pages' coordinates both span
                    // 0..1 and would interleave into rows that never existed.
                    if (imagePaths.size == 1) lines += ocrResult.lines
                }
                val receipt = parseReceipt(
                    imagePath = imagePaths.first(),
                    ocrText = texts.joinToString("\n"),
                    lines = lines,
                    additionalPagePaths = imagePaths.drop(1),
                )
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    scanProgress = null,
                    reviewReceiptId = receipt.id,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    scanProgress = null,
                    error = e.message ?: "Failed to scan image",
                )
            }
        }
    }

    private fun progressLabel(index: Int, total: Int): String? =
        if (total <= 1) null else "Reading page ${index + 1} of $total…"

    /** Clears the navigation signal so returning from the review step doesn't re-fire it. */
    fun onReviewNavigated() {
        _uiState.value = _uiState.value.copy(reviewReceiptId = null)
    }

    fun updateCsvContent(content: String) {
        // Typed or pasted, so it is no longer what any picked file contained.
        _uiState.value = _uiState.value.copy(csvContent = content, csvFileName = null)
    }

    /** A file chosen through [com.divafinance.feature.scanner.capture.TextFilePicker]. */
    fun onCsvFilePicked(result: TextFileResult) {
        _uiState.value = when (result) {
            is TextFileResult.Cancelled -> _uiState.value
            is TextFileResult.Failed -> _uiState.value.copy(error = result.message)
            is TextFileResult.Success -> _uiState.value.copy(
                csvContent = result.content,
                csvFileName = result.fileName,
                error = null,
            )
        }
    }

    fun updateAccountId(id: String?) {
        _uiState.value = _uiState.value.copy(accountId = id)
    }

    fun updateCardId(id: String?) {
        _uiState.value = _uiState.value.copy(cardId = id)
    }

    fun importCsvStatement() {
        val state = _uiState.value
        if (state.csvContent.isBlank()) {
            _uiState.value = state.copy(error = "No CSV content provided")
            return
        }
        val accountId = state.accountId
        if (accountId == null) {
            _uiState.value = state.copy(error = "Choose an account to import into")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, error = null)
            try {
                val count = importStatement(
                    csvContent = state.csvContent,
                    accountId = accountId,
                    cardId = state.cardId,
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
