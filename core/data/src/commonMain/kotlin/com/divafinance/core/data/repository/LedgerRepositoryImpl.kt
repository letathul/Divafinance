package com.divafinance.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.divafinance.core.database.DivaFinanceDb
import com.divafinance.core.model.LedgerEntry
import com.divafinance.core.model.enums.LedgerEntryKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

class LedgerRepositoryImpl(
    private val db: DivaFinanceDb
) : LedgerRepository {

    override fun getAll(): Flow<List<LedgerEntry>> {
        return db.ledgerEntryQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }
    }

    override suspend fun getByPersonId(personId: String): List<LedgerEntry> =
        db.ledgerEntryQueries.selectByPersonId(personId).executeAsList().map { it.toDomain() }

    override suspend fun getByTransactionId(transactionId: String): List<LedgerEntry> =
        db.ledgerEntryQueries.selectByTransactionId(transactionId).executeAsList().map { it.toDomain() }

    override suspend fun insert(entry: LedgerEntry) {
        db.ledgerEntryQueries.insert(
            id = entry.id,
            person_id = entry.personId,
            amount = entry.amount,
            currency = entry.currency,
            kind = entry.kind.name,
            note = entry.note,
            date = entry.date.toString(),
            transaction_id = entry.transactionId,
            created_at = entry.createdAt.toString(),
        )
    }

    override suspend fun update(entry: LedgerEntry) {
        db.ledgerEntryQueries.update(
            person_id = entry.personId,
            amount = entry.amount,
            currency = entry.currency,
            kind = entry.kind.name,
            note = entry.note,
            date = entry.date.toString(),
            transaction_id = entry.transactionId,
            id = entry.id,
        )
    }

    override suspend fun delete(id: String) {
        db.ledgerEntryQueries.delete(id)
    }

    override suspend fun deleteByTransactionId(transactionId: String) {
        db.ledgerEntryQueries.deleteByTransactionId(transactionId)
    }

    override suspend fun deleteByPersonId(personId: String) {
        db.ledgerEntryQueries.deleteByPersonId(personId)
    }

    override suspend fun count(): Long = db.ledgerEntryQueries.count().executeAsOne()
}

private fun com.divafinance.core.database.LedgerEntry.toDomain() = LedgerEntry(
    id = id,
    personId = person_id,
    amount = amount,
    currency = currency,
    // Total lookup rather than valueOf: an unrecognised name would otherwise make every
    // historical row unreadable. LENT is the safest default — it is what a split creates.
    kind = LedgerEntryKind.entries.firstOrNull { it.name == kind } ?: LedgerEntryKind.LENT,
    note = note,
    date = LocalDate.parse(date),
    transactionId = transaction_id,
    createdAt = Instant.parse(created_at),
)
