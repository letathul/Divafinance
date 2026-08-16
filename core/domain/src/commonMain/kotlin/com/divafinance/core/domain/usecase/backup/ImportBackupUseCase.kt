package com.divafinance.core.domain.usecase.backup

import com.divafinance.core.data.repository.BackupRepository
import com.divafinance.core.model.BackupArchive

/** Thrown when an archive is too new for this build to import safely. */
class UnsupportedBackupVersionException(
    val archiveVersion: Int,
    val supportedVersion: Int,
) : Exception(
    "This backup was made by a newer version of the app " +
        "(format $archiveVersion, this build supports up to $supportedVersion).",
)

class ImportBackupUseCase(
    private val backupRepository: BackupRepository
) {
    /**
     * Older archives import fine — fields added since they were written fall back to their
     * defaults. Newer ones are refused rather than partially read: unknown keys are
     * silently dropped on decode, so importing one would quietly discard whatever it
     * carried, and the next export would write that loss back to disk.
     */
    suspend operator fun invoke(archive: BackupArchive, replaceExisting: Boolean = false) {
        if (archive.version > BackupArchive.CURRENT_VERSION) {
            throw UnsupportedBackupVersionException(
                archiveVersion = archive.version,
                supportedVersion = BackupArchive.CURRENT_VERSION,
            )
        }
        backupRepository.importAll(archive, replaceExisting)
    }
}
