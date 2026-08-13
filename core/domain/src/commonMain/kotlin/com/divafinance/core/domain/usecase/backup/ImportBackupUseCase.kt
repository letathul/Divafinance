package com.divafinance.core.domain.usecase.backup

import com.divafinance.core.data.repository.BackupRepository
import com.divafinance.core.model.BackupArchive

class ImportBackupUseCase(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(archive: BackupArchive, replaceExisting: Boolean = false) {
        backupRepository.importAll(archive, replaceExisting)
    }
}
