package com.divafinance.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divafinance.core.common.BackupFileInfo
import com.divafinance.core.common.FileSystem
import com.divafinance.core.domain.usecase.backup.ExportBackupUseCase
import com.divafinance.core.domain.usecase.backup.ImportBackupUseCase
import com.divafinance.core.model.BackupArchive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.serialization.json.Json

data class BackupUiState(
    val backupFiles: List<BackupFileInfo> = emptyList(),
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val exportResult: ExportResult? = null,
    val importResult: ImportResult? = null,
    val showImportConfirmDialog: Boolean = false,
    val pendingImportFile: BackupFileInfo? = null,
    val showDeleteConfirmDialog: Boolean = false,
    val pendingDeleteFile: BackupFileInfo? = null,
)

sealed class ExportResult {
    data class Success(val fileName: String) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

sealed class ImportResult {
    data class Success(
        val accounts: Int,
        val cards: Int,
        val transactions: Int,
    ) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

class BackupViewModel(
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase,
    private val fileSystem: FileSystem,
) : ViewModel() {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        refreshFileList()
    }

    fun refreshFileList() {
        val files = fileSystem.listBackupFiles()
        _uiState.update { it.copy(backupFiles = files) }
    }

    fun exportBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportResult = null) }
            try {
                val archive = exportBackupUseCase()
                val jsonString = json.encodeToString(BackupArchive.serializer(), archive)
                val timestamp = Clock.System.now().epochSeconds
                val fileName = "diva_backup_$timestamp.diva"
                val filePath = "${fileSystem.getBackupDirectory()}/$fileName"
                fileSystem.writeText(filePath, jsonString)
                refreshFileList()
                _uiState.update {
                    it.copy(isExporting = false, exportResult = ExportResult.Success(fileName))
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportResult = ExportResult.Error(e.message ?: "Export failed"),
                    )
                }
            }
        }
    }

    fun requestImport(file: BackupFileInfo) {
        _uiState.update {
            it.copy(showImportConfirmDialog = true, pendingImportFile = file)
        }
    }

    fun cancelImport() {
        _uiState.update {
            it.copy(showImportConfirmDialog = false, pendingImportFile = null)
        }
    }

    fun confirmImport(replaceExisting: Boolean) {
        val file = _uiState.value.pendingImportFile ?: return
        _uiState.update {
            it.copy(showImportConfirmDialog = false, isImporting = true, importResult = null)
        }
        viewModelScope.launch {
            try {
                val jsonString = fileSystem.readText(file.path)
                val archive = json.decodeFromString(BackupArchive.serializer(), jsonString)
                if (archive.version > 1) {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importResult = ImportResult.Error(
                                "Unsupported backup version ${archive.version}. Please update the app."
                            ),
                        )
                    }
                    return@launch
                }
                importBackupUseCase(archive, replaceExisting)
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        pendingImportFile = null,
                        importResult = ImportResult.Success(
                            accounts = archive.accounts.size,
                            cards = archive.creditCards.size,
                            transactions = archive.transactions.size,
                        ),
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        pendingImportFile = null,
                        importResult = ImportResult.Error(e.message ?: "Import failed"),
                    )
                }
            }
        }
    }

    fun requestDelete(file: BackupFileInfo) {
        _uiState.update {
            it.copy(showDeleteConfirmDialog = true, pendingDeleteFile = file)
        }
    }

    fun cancelDelete() {
        _uiState.update {
            it.copy(showDeleteConfirmDialog = false, pendingDeleteFile = null)
        }
    }

    fun confirmDelete() {
        val file = _uiState.value.pendingDeleteFile ?: return
        fileSystem.deleteFile(file.path)
        refreshFileList()
        _uiState.update {
            it.copy(showDeleteConfirmDialog = false, pendingDeleteFile = null)
        }
    }

    fun clearExportResult() {
        _uiState.update { it.copy(exportResult = null) }
    }

    fun clearImportResult() {
        _uiState.update { it.copy(importResult = null) }
    }
}
