package com.divafinance.core.domain.usecase.backup

import com.divafinance.core.domain.fake.FakeBackupRepository
import com.divafinance.core.model.BackupArchive
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ImportBackupUseCaseTest {

    private val backupRepo = FakeBackupRepository()
    private val useCase = ImportBackupUseCase(backupRepo)

    private val archive = BackupArchive(
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

    @Test
    fun importsArchive() = runTest {
        useCase(archive)

        assertNotNull(backupRepo.lastImport)
        assertEquals(1, backupRepo.lastImport?.version)
    }

    @Test
    fun defaultsToNotReplaceExisting() = runTest {
        useCase(archive)

        assertFalse(backupRepo.lastReplaceExisting)
    }

    @Test
    fun passesReplaceExistingFlag() = runTest {
        useCase(archive, replaceExisting = true)

        assertTrue(backupRepo.lastReplaceExisting)
    }
}
