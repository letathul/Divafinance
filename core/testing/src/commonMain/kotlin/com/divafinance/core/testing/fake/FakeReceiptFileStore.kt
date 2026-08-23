package com.divafinance.core.testing.fake

import com.divafinance.core.data.repository.ReceiptFileStore

/**
 * Tracks deletions instead of performing them. [seed] is what makes the "already gone"
 * case testable: an unseeded path reports `false` from [delete], exactly as the real
 * filesystem does for a file that isn't there.
 */
class FakeReceiptFileStore : ReceiptFileStore {
    private val files = mutableSetOf<String>()
    private val deleted = mutableListOf<String>()

    override fun delete(path: String): Boolean {
        deleted += path
        return files.remove(path)
    }

    fun seed(vararg paths: String) {
        files += paths
    }

    /** Every path [delete] was called with, in order, including misses. */
    fun deletedPaths(): List<String> = deleted.toList()

    /** What survives — the assertion that actually catches a leak. */
    fun remainingFiles(): Set<String> = files.toSet()
}
