package com.divafinance.feature.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.adaptive.DivaScaffold
import com.divafinance.core.model.Receipt
import com.divafinance.core.model.enums.ReceiptStatus
import com.divafinance.core.ui.component.AmountDisplay
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.component.DivaOutlinedButton
import com.divafinance.core.ui.component.DivaTextField
import com.divafinance.core.ui.component.LoadingIndicator
import com.divafinance.core.ui.component.Meta
import com.divafinance.core.ui.component.SegmentedControl
import com.divafinance.core.ui.component.StatPill
import com.divafinance.core.ui.theme.diva
import com.divafinance.core.ui.util.formatDate
import com.divafinance.core.ui.adaptive.DivaChip
import com.divafinance.feature.scanner.capture.ImageSource
import com.divafinance.feature.scanner.capture.isDocumentScanSupported
import com.divafinance.feature.scanner.capture.rememberImageCaptureRequester
import com.divafinance.feature.scanner.capture.rememberTextFilePicker
import org.koin.compose.viewmodel.koinViewModel

private val TABS = listOf("Scan", "History", "Import")

/**
 * Two ways to get transactions in without typing them, plus the record of what has been
 * scanned. `onReviewReceipt` and `onOpenTransaction` default to no-ops so
 * `:dynamic:scanner_dynamic`, which hosts this screen in a bare Activity with no NavHost,
 * still compiles.
 */
@Composable
fun ScannerScreen(
    onBack: () -> Unit = {},
    onReviewReceipt: (String) -> Unit = {},
    onOpenTransaction: (String) -> Unit = {},
    viewModel: ScannerViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val receipts by viewModel.receipts.collectAsState()
    val captureRequester = rememberImageCaptureRequester()
    val canScanDocument = isDocumentScanSupported()
    val filePicker = rememberTextFilePicker()

    // The ViewModel decides whether to launch; the launcher lives here because Android needs
    // an Activity result contract. Keyed on the nonce so every decision is acted on once.
    LaunchedEffect(uiState.captureRequestNonce) {
        val source = uiState.pendingSource
        if (uiState.captureRequestNonce > 0 && source != null) {
            captureRequester.request(source, viewModel::onImageCaptured)
        }
    }

    LaunchedEffect(uiState.reviewReceiptId) {
        uiState.reviewReceiptId?.let { id ->
            viewModel.onReviewNavigated()
            onReviewReceipt(id)
        }
    }

    DivaScaffold(
        title = "Scanner & Import",
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            SegmentedControl(
                options = TABS,
                selectedIndex = uiState.currentTab.ordinal,
                onSelect = { viewModel.switchTab(ScannerTab.entries[it]) },
            )
            Spacer(Modifier.height(16.dp))

            when (uiState.currentTab) {
                ScannerTab.RECEIPT -> ReceiptScannerContent(
                    uiState = uiState,
                    isOcrAvailable = viewModel.isOcrAvailable(),
                    canScanDocument = canScanDocument,
                    onCapture = viewModel::onCaptureRequested,
                )
                ScannerTab.HISTORY -> ReceiptHistoryContent(
                    receipts = receipts,
                    onReviewReceipt = onReviewReceipt,
                    onOpenTransaction = onOpenTransaction,
                )
                ScannerTab.IMPORT -> StatementImportContent(
                    uiState = uiState,
                    onCsvChange = viewModel::updateCsvContent,
                    onChooseFile = { filePicker.pick(viewModel::onCsvFilePicked) },
                    onAccountIdChange = viewModel::updateAccountId,
                    onCardIdChange = viewModel::updateCardId,
                    onImport = viewModel::importCsvStatement,
                    onClear = viewModel::clearResult,
                )
            }
        }
    }
}

@Composable
private fun ReceiptScannerContent(
    uiState: ScannerUiState,
    isOcrAvailable: Boolean,
    canScanDocument: Boolean,
    onCapture: (ImageSource) -> Unit,
) {
    DivaCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Scan a receipt", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (isOcrAvailable) {
                Text(
                    if (canScanDocument) {
                        "Hold the receipt in frame and it'll capture, straighten and crop " +
                            "itself — several pages if the receipt is long. You get to check " +
                            "everything before it's saved."
                    } else {
                        "Photograph a receipt and we'll read the merchant, total and date " +
                            "off it. You get to check everything before it's saved."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                if (uiState.isProcessing) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LoadingIndicator()
                            // Only set for a multi-page scan, where the wait is long enough
                            // that a bare spinner reads as a hang.
                            uiState.scanProgress?.let {
                                Spacer(Modifier.height(8.dp))
                                Meta(it)
                            }
                        }
                    }
                } else {
                    // The document scanner is the primary action where it exists: it does the
                    // framing, cropping and de-skewing that a plain photo leaves to OCR.
                    if (canScanDocument) {
                        DivaButton(
                            text = "Scan receipt",
                            onClick = { onCapture(ImageSource.DOCUMENT_SCAN) },
                        )
                        Spacer(Modifier.height(8.dp))
                        DivaOutlinedButton(
                            text = "Take photo",
                            onClick = { onCapture(ImageSource.CAMERA) },
                        )
                    } else {
                        DivaButton(
                            text = "Take photo",
                            onClick = { onCapture(ImageSource.CAMERA) },
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    DivaOutlinedButton(
                        text = "Choose photo",
                        onClick = { onCapture(ImageSource.PHOTO_LIBRARY) },
                    )
                }
            } else {
                // No demo path: a fabricated result reads as a working scanner and hides a
                // genuinely unsupported device.
                Meta("Receipt scanning isn't available on this device")
                Spacer(Modifier.height(8.dp))
                Text(
                    "You can still import a statement from the Import tab.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    uiState.error?.let { error ->
        Spacer(Modifier.height(8.dp))
        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ReceiptHistoryContent(
    receipts: List<Receipt>,
    onReviewReceipt: (String) -> Unit,
    onOpenTransaction: (String) -> Unit,
) {
    if (receipts.isEmpty()) {
        EmptyReceipts()
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        receipts.forEach { receipt ->
            ReceiptRow(
                receipt = receipt,
                onClick = {
                    // A receipt already turned into a transaction opens that transaction
                    // rather than offering to save it a second time.
                    val transactionId = receipt.transactionId
                    if (transactionId != null) onOpenTransaction(transactionId)
                    else onReviewReceipt(receipt.id)
                },
            )
        }
    }
}

@Composable
private fun ReceiptRow(receipt: Receipt, onClick: () -> Unit) {
    DivaCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    receipt.merchantName ?: "Unknown merchant",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(2.dp))
                Meta(receipt.date?.let { formatDate(it) } ?: "No date found")
            }
            Column(horizontalAlignment = Alignment.End) {
                receipt.totalAmount?.let { AmountDisplay(amount = it) }
                    ?: Meta("No total")
                if (receipt.transactionId == null) {
                    Spacer(Modifier.height(4.dp))
                    StatPill(
                        text = if (receipt.status == ReceiptStatus.FAILED) "Unread" else "Review",
                        tint = diva.muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyReceipts() {
    DivaCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Outlined.PhotoCamera,
                contentDescription = null,
                tint = diva.muted,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text("No scans yet", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Receipts you scan will collect here, so you can finish one later.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatementImportContent(
    uiState: ScannerUiState,
    onCsvChange: (String) -> Unit,
    onChooseFile: () -> Unit,
    onAccountIdChange: (String?) -> Unit,
    onCardIdChange: (String?) -> Unit,
    onImport: () -> Unit,
    onClear: () -> Unit,
) {
    DivaCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("CSV Statement Import", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Choose a CSV with columns: date, description, amount. " +
                    "Header row is skipped automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            DivaOutlinedButton(
                text = uiState.csvFileName ?: "Choose a file…",
                onClick = onChooseFile,
                modifier = Modifier.fillMaxWidth(),
            )

            // The account the rows land in. Ids used to be typed by hand here, which is
            // not something anyone knows; one account is preselected by the ViewModel.
            Spacer(Modifier.height(12.dp))
            Text("Import into", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            if (uiState.accounts.isEmpty()) {
                Meta("No accounts yet — add one before importing.")
            } else {
                ChipRow(
                    options = uiState.accounts.map { it.id to it.name },
                    selectedId = uiState.accountId,
                    onSelect = onAccountIdChange,
                )
            }

            if (uiState.cards.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Card (optional)", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                ChipRow(
                    options = uiState.cards.map { it.id to it.name },
                    selectedId = uiState.cardId,
                    onSelect = onCardIdChange,
                )
            }

            Spacer(Modifier.height(12.dp))
            DivaTextField(
                value = uiState.csvContent,
                onValueChange = onCsvChange,
                label = "CSV Content",
                modifier = Modifier.fillMaxWidth().height(120.dp),
                singleLine = false,
            )
            Spacer(Modifier.height(16.dp))

            if (uiState.isProcessing) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            } else {
                DivaButton(
                    text = "Import",
                    onClick = onImport,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    uiState.importedCount?.let { count ->
        Spacer(Modifier.height(12.dp))
        DivaCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "$count transactions imported",
                    style = MaterialTheme.typography.titleMedium,
                    color = diva.positive,
                )
                Spacer(Modifier.height(8.dp))
                DivaButton(text = "Clear", onClick = onClear, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    uiState.error?.let { error ->
        Spacer(Modifier.height(8.dp))
        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * A single-select row of chips over `id to label` pairs. Tapping the selected one clears
 * it, which is how the optional card is unset without a separate "None" entry.
 */
@Composable
private fun ChipRow(
    options: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (id, label) ->
            DivaChip(
                label = label,
                selected = id == selectedId,
                onClick = { onSelect(if (id == selectedId) null else id) },
            )
        }
    }
}
