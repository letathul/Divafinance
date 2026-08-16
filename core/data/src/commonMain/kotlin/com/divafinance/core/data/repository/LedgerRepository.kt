package com.divafinance.core.data.repository

import com.divafinance.core.model.LedgerEntry
import kotlinx.coroutines.flow.Flow

interface LedgerRepository {
    fun getAll(): Flow<List<LedgerEntry>>
    suspend fun getByPersonId(personId: String): List<LedgerEntry>
    suspend fun getByTransactionId(transactionId: String): List<LedgerEntry>
    suspend fun insert(entry: LedgerEntry)
    suspend fun update(entry: LedgerEntry)
    suspend fun delete(id: String)

    /** Used when a split transaction is removed, so its debts do not outlive it. */
    suspend fun deleteByTransactionId(transactionId: String)
    suspend fun deleteByPersonId(personId: String)
    suspend fun count(): Long
}
