package com.divafinance.feature.scanner

import com.divafinance.core.data.repository.TransactionRepository
import com.divafinance.core.model.Transaction
import com.divafinance.core.testing.fake.FakeTransactionRepository

/**
 * Fails the write that a receipt's transaction goes through, so the review screen's failure
 * path can be exercised. Local rather than a flag on the shared fake: only this feature needs
 * to prove what happens when the transaction insert throws.
 */
class FailingTransactionRepository(
    private val delegate: FakeTransactionRepository = FakeTransactionRepository(),
) : TransactionRepository by delegate {
    override suspend fun insert(transaction: Transaction): Unit =
        throw IllegalStateException("Database unavailable")
}
