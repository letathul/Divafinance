package com.divafinance.core.domain.usecase.backup

import com.divafinance.core.data.repository.BackupRepository
import com.divafinance.core.model.BackupArchive

class ExportBackupUseCase(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(): BackupArchive {
        return backupRepository.exportAll()
    }
}
