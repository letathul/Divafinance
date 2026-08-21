package com.divafinance.core.domain.usecase.backup

import com.divafinance.core.model.BackupArchive
import com.divafinance.core.testing.fake.FakeBackupRepository
import com.divafinance.core.testing.fake.TestData
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Guards the backup format across versions.
 *
 * These assertions are not enforced by the compiler: the new archive fields have defaults
 * so that old `.diva` files still decode, and that same default means forgetting to
 * populate them anywhere compiles perfectly and silently exports nothing.
 */
class BackupCompatibilityTest {

    private val repo = FakeBackupRepository()
    private val useCase = ImportBackupUseCase(repo)

    /** Matches the reader in BackupViewModel. */
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun refusesAnArchiveFromANewerBuild() = runTest {
        val future = repo.exportArchive.copy(version = BackupArchive.CURRENT_VERSION + 1)

        val error = assertFailsWith<UnsupportedBackupVersionException> { useCase(future) }
        assertEquals(BackupArchive.CURRENT_VERSION + 1, error.archiveVersion)
        assertEquals(null, repo.lastImport)
    }

    @Test
    fun acceptsAnArchiveFromAnOlderBuild() = runTest {
        useCase(repo.exportArchive.copy(version = 1))

        assertEquals(1, repo.lastImport?.version)
    }

    @Test
    fun acceptsTheCurrentVersion() = runTest {
        useCase(repo.exportArchive)

        assertEquals(BackupArchive.CURRENT_VERSION, repo.lastImport?.version)
    }

    /**
     * The compatibility guarantee: a v1 file predates `people` and `ledgerEntries`
     * entirely. Without defaults on those fields this throws MissingFieldException and
     * every backup a user already has becomes unrestorable.
     */
    @Test
    fun decodesAV1ArchiveThatPredatesPeople() {
        val v1 = """
            {
              "version": 1,
              "createdAt": "2026-01-01T00:00:00Z",
              "accounts": [], "creditCards": [], "rewardRules": [],
              "transactions": [], "receipts": [], "feedPosts": [],
              "settings": [], "thresholds": []
            }
        """.trimIndent()

        val archive = json.decodeFromString<BackupArchive>(v1)

        assertEquals(1, archive.version)
        assertTrue(archive.people.isEmpty())
        assertTrue(archive.ledgerEntries.isEmpty())
    }

    @Test
    fun roundTripsPeopleAndLedgerEntries() {
        val archive = repo.exportArchive.copy(
            people = listOf(TestData.person(id = "p1", name = "Sam")),
            ledgerEntries = listOf(TestData.ledgerEntry(id = "l1", personId = "p1", amount = 40.0)),
        )

        val restored = json.decodeFromString<BackupArchive>(json.encodeToString(archive))

        assertEquals(listOf("Sam"), restored.people.map { it.name })
        assertEquals(40.0, restored.ledgerEntries.single().amount)
        assertEquals("p1", restored.ledgerEntries.single().personId)
    }

    /** A transaction's split portion must survive an export/import cycle. */
    @Test
    fun roundTripsTheSplitPortionOfATransaction() {
        val archive = repo.exportArchive.copy(
            transactions = listOf(
                TestData.transaction(id = "dinner", amount = 120.0, othersShare = 80.0)
            ),
        )

        val restored = json.decodeFromString<BackupArchive>(json.encodeToString(archive))

        assertEquals(80.0, restored.transactions.single().othersShare)
    }
}
