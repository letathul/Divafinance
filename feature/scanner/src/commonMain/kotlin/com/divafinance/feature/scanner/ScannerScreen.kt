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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.component.DivaTextField
import com.divafinance.core.ui.component.LoadingIndicator
import com.divafinance.core.ui.theme.DivaGreen
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onBack: () -> Unit = {},
    viewModel: ScannerViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scanner & Import") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.currentTab == ScannerTab.RECEIPT,
                    onClick = { viewModel.switchTab(ScannerTab.RECEIPT) },
                    label = { Text("Receipt Scanner") },
                )
                FilterChip(
                    selected = uiState.currentTab == ScannerTab.IMPORT,
                    onClick = { viewModel.switchTab(ScannerTab.IMPORT) },
                    label = { Text("CSV Import") },
                )
            }
            Spacer(Modifier.height(16.dp))

            when (uiState.currentTab) {
                ScannerTab.RECEIPT -> ReceiptScannerContent(
                    uiState = uiState,
                    onScanDemo = {
                        viewModel.processOcrResult(
                            imagePath = "demo_receipt.jpg",
                            ocrText = "STARBUCKS\n123 Main St\nLatte 5.50\nTotal: \$5.50",
                        )
                    },
                    onClear = { viewModel.clearResult() },
                )
                ScannerTab.IMPORT -> StatementImportContent(
                    uiState = uiState,
                    onCsvChange = { viewModel.updateCsvContent(it) },
                    onAccountIdChange = { viewModel.updateAccountId(it) },
                    onCardIdChange = { viewModel.updateCardId(it) },
                    onImport = { viewModel.importCsvStatement() },
                    onClear = { viewModel.clearResult() },
                )
            }
        }
    }
}

@Composable
private fun ReceiptScannerContent(
    uiState: ScannerUiState,
    onScanDemo: () -> Unit,
    onClear: () -> Unit,
) {
    DivaCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Receipt Scanner", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Scan a receipt to extract merchant name and total amount. " +
                    "Camera integration requires the scanner dynamic module.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            if (uiState.isProcessing) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            } else {
                DivaButton(
                    text = "Scan Demo Receipt",
                    onClick = onScanDemo,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    uiState.lastReceipt?.let { receipt ->
        Spacer(Modifier.height(12.dp))
        DivaCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Scan Result", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                ResultRow("Merchant", receipt.merchantName ?: "Unknown")
                ResultRow("Total", receipt.totalAmount?.let { "${"$%.2f".format(it)}" } ?: "Not found")
                ResultRow("Status", receipt.status.name)
                Spacer(Modifier.height(12.dp))
                DivaButton(text = "Clear", onClick = onClear, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    uiState.error?.let { error ->
        Spacer(Modifier.height(8.dp))
        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StatementImportContent(
    uiState: ScannerUiState,
    onCsvChange: (String) -> Unit,
    onAccountIdChange: (String) -> Unit,
    onCardIdChange: (String) -> Unit,
    onImport: () -> Unit,
    onClear: () -> Unit,
) {
    DivaCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("CSV Statement Import", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Paste CSV content with columns: date, description, amount. " +
                    "Header row is skipped automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            DivaTextField(
                value = uiState.accountId,
                onValueChange = onAccountIdChange,
                label = "Account ID",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            DivaTextField(
                value = uiState.cardId,
                onValueChange = onCardIdChange,
                label = "Card ID (optional)",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
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
                    color = DivaGreen,
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

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
