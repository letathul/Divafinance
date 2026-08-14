package com.divafinance.feature.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.divafinance.core.common.BackupFileInfo
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.component.DivaOutlinedButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    onBack: () -> Unit = {},
    viewModel: BackupViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.exportResult) {
        when (val result = state.exportResult) {
            is ExportResult.Success -> {
                snackbarHostState.showSnackbar("Backup saved: ${result.fileName}")
                viewModel.clearExportResult()
            }
            is ExportResult.Error -> {
                snackbarHostState.showSnackbar("Export failed: ${result.message}")
                viewModel.clearExportResult()
            }
            null -> {}
        }
    }

    LaunchedEffect(state.importResult) {
        when (val result = state.importResult) {
            is ImportResult.Success -> {
                snackbarHostState.showSnackbar(
                    "Restored ${result.accounts} accounts, ${result.cards} cards, ${result.transactions} transactions"
                )
                viewModel.clearImportResult()
            }
            is ImportResult.Error -> {
                snackbarHostState.showSnackbar("Import failed: ${result.message}")
                viewModel.clearImportResult()
            }
            null -> {}
        }
    }

    if (state.isExporting) {
        BackupProgressDialog(
            title = "Exporting",
            message = "Creating backup of your financial data...",
        )
    }

    if (state.isImporting) {
        BackupProgressDialog(
            title = "Importing",
            message = "Restoring data from backup...",
        )
    }

    if (state.showImportConfirmDialog && state.pendingImportFile != null) {
        ImportConfirmDialog(
            fileName = state.pendingImportFile!!.name,
            onConfirmReplace = { viewModel.confirmImport(replaceExisting = true) },
            onConfirmMerge = { viewModel.confirmImport(replaceExisting = false) },
            onDismiss = { viewModel.cancelImport() },
        )
    }

    if (state.showDeleteConfirmDialog && state.pendingDeleteFile != null) {
        DeleteConfirmDialog(
            fileName = state.pendingDeleteFile!!.name,
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.cancelDelete() },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ExportSection(
                    isExporting = state.isExporting,
                    onExport = { viewModel.exportBackup() },
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Saved Backups",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (state.backupFiles.isEmpty()) {
                item {
                    Text(
                        text = "No backups yet. Create your first backup above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }

            items(state.backupFiles, key = { it.path }) { file ->
                BackupFileItem(
                    file = file,
                    onImport = { viewModel.requestImport(file) },
                    onDelete = { viewModel.requestDelete(file) },
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ExportSection(
    isExporting: Boolean,
    onExport: () -> Unit,
) {
    DivaCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Create Backup",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Export all your accounts, cards, transactions, and settings to a backup file.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            DivaButton(
                text = "Export Backup",
                onClick = onExport,
                enabled = !isExporting,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BackupFileItem(
    file: BackupFileInfo,
    onImport: () -> Unit,
    onDelete: () -> Unit,
) {
    DivaCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatFileSize(file.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            DivaOutlinedButton(
                text = "Restore",
                onClick = onImport,
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ImportConfirmDialog(
    fileName: String,
    onConfirmReplace: () -> Unit,
    onConfirmMerge: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore Backup") },
        text = {
            Column {
                Text("Restore data from \"$fileName\"?")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Replace: Clears all current data and restores from backup.\nMerge: Adds backup data alongside existing data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirmReplace) {
                Text("Replace", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = onConfirmMerge) { Text("Merge") }
            }
        },
    )
}

@Composable
private fun DeleteConfirmDialog(
    fileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Backup") },
        text = { Text("Delete \"$fileName\"? This cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
        else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    }
}
