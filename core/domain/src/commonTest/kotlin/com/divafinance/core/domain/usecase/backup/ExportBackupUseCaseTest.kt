package com.divafinance.core.domain.usecase.backup

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
        assertEquals(1, result.version)
    }

    @Test
    fun delegatesToRepository() = runTest {
        useCase()

        assertNotNull(backupRepo.lastExport)
    }
}
