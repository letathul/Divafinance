package com.divafinance.core.domain.usecase.backup

import com.divafinance.core.model.BackupArchive
import com.divafinance.core.testing.fake.FakeBackupRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ExportBackupUseCaseTest {

    private val backupRepo = FakeBackupRepository()
    private val useCase = ExportBackupUseCase(backupRepo)

    @Test
    fun returnsExportedArchive() = runTest {
        val result = useCase()

        assertNotNull(result)
        // Tracks the constant rather than a literal, so a format bump is a
        // deliberate edit to BackupArchive rather than a surprise test failure here.
        assertEquals(BackupArchive.CURRENT_VERSION, result.version)
    }

    @Test
    fun delegatesToRepository() = runTest {
        useCase()

        assertNotNull(backupRepo.lastExport)
    }
}
