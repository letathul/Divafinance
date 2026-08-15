package com.divafinance.core.domain.fake

import com.divafinance.core.data.repository.BackupRepository
import com.divafinance.core.model.BackupArchive
import kotlin.time.Clock

class FakeBackupRepository : BackupRepository {
    var lastExport: BackupArchive? = null
    var lastImport: BackupArchive? = null
    var lastReplaceExisting: Boolean = false
    var exportArchive: BackupArchive = BackupArchive(
        version = 1,
        createdAt = Clock.System.now(),
        accounts = emptyList(),
        creditCards = emptyList(),
        rewardRules = emptyList(),
        transactions = emptyList(),
        receipts = emptyList(),
        feedPosts = emptyList(),
        settings = emptyList(),
        thresholds = emptyList(),
    )

    override suspend fun exportAll(): BackupArchive {
        lastExport = exportArchive
        return exportArchive
    }

    override suspend fun importAll(archive: BackupArchive, replaceExisting: Boolean) {
        lastImport = archive
        lastReplaceExisting = replaceExisting
    }
}
