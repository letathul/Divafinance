package com.divafinance.core.testing.fake

import com.divafinance.core.data.repository.LedgerRepository
import com.divafinance.core.model.LedgerEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeLedgerRepository : LedgerRepository {
    private val entries = MutableStateFlow<List<LedgerEntry>>(emptyList())

    override fun getAll(): Flow<List<LedgerEntry>> = entries

    override suspend fun getByPersonId(personId: String): List<LedgerEntry> =
        entries.value.filter { it.personId == personId }

    override suspend fun getByTransactionId(transactionId: String): List<LedgerEntry> =
        entries.value.filter { it.transactionId == transactionId }

    override suspend fun insert(entry: LedgerEntry) {
        entries.value = entries.value + entry
    }

    override suspend fun update(entry: LedgerEntry) {
        entries.value = entries.value.map { if (it.id == entry.id) entry else it }
    }

    override suspend fun delete(id: String) {
        entries.value = entries.value.filterNot { it.id == id }
    }

    override suspend fun deleteByTransactionId(transactionId: String) {
        entries.value = entries.value.filterNot { it.transactionId == transactionId }
    }

    override suspend fun deleteByPersonId(personId: String) {
        entries.value = entries.value.filterNot { it.personId == personId }
    }

    override suspend fun count(): Long = entries.value.size.toLong()

    fun setEntries(list: List<LedgerEntry>) {
        entries.value = list
    }
}
