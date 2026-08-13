package com.divafinance.core.data.repository

import com.divafinance.core.model.BackupArchive

interface BackupRepository {
    suspend fun exportAll(): BackupArchive
    suspend fun importAll(archive: BackupArchive, replaceExisting: Boolean = false)
}
