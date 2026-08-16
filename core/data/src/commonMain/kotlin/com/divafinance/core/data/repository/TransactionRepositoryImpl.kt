package com.divafinance.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.divafinance.core.data.mapper.toDomain
import com.divafinance.core.database.DivaFinanceDb
import com.divafinance.core.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class TransactionRepositoryImpl(
    private val db: DivaFinanceDb
) : TransactionRepository {

    override fun getAll(): Flow<List<Transaction>> {
        return db.transactionQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }
    }

    override suspend fun getById(id: String): Transaction? {
        return db.transactionQueries.selectById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun getByAccountId(accountId: String): List<Transaction> {
        return db.transactionQueries.selectByAccountId(accountId)
            .executeAsList()
            .map { it.toDomain() }
    }

    override suspend fun getByCategory(category: String): List<Transaction> {
        return db.transactionQueries.selectByCategory(category)
            .executeAsList()
            .map { it.toDomain() }
    }

    override suspend fun getByDateRange(startDate: LocalDate, endDate: LocalDate): List<Transaction> {
        return db.transactionQueries.selectByDateRange(startDate.toString(), endDate.toString())
            .executeAsList()
            .map { it.toDomain() }
    }

    override suspend fun getByCategoryAndDateRange(
        category: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Transaction> {
        return db.transactionQueries.selectByCategoryAndDateRange(
            category, startDate.toString(), endDate.toString()
        ).executeAsList().map { it.toDomain() }
    }

    override suspend fun getWithLocation(): List<Transaction> {
        return db.transactionQueries.selectWithLocation()
            .executeAsList()
            .map { it.toDomain() }
    }

    override suspend fun getSpendingByCategory(
        startDate: LocalDate,
        endDate: LocalDate
    ): Map<String, Double> {
        return db.transactionQueries.sumByCategory(startDate.toString(), endDate.toString())
            .executeAsList()
            .associate { it.category to (it.total ?: 0.0) }
    }

    override suspend fun getTotalSpending(startDate: LocalDate, endDate: LocalDate): Double? {
        return db.transactionQueries.totalSpending(startDate.toString(), endDate.toString())
            .executeAsOne()
            .SUM
    }

    override suspend fun insert(transaction: Transaction) {
        db.transactionQueries.insert(
            id = transaction.id,
            account_id = transaction.accountId,
            card_id = transaction.cardId,
            amount = transaction.amount,
            currency = transaction.currency,
            category = transaction.category.name,
            subcategory = transaction.subcategory,
            merchant_name = transaction.merchantName,
            note = transaction.note,
            date = transaction.date.toString(),
            type = transaction.type.name,
            latitude = transaction.location?.latitude,
            longitude = transaction.location?.longitude,
            location_name = transaction.location?.name,
            receipt_id = transaction.receiptId,
            is_recurring = if (transaction.isRecurring) 1L else 0L,
            created_at = transaction.createdAt.toString(),
        )
    }

    override suspend fun update(transaction: Transaction) {
        db.transactionQueries.update(
            account_id = transaction.accountId,
            card_id = transaction.cardId,
            amount = transaction.amount,
            currency = transaction.currency,
            category = transaction.category.name,
            subcategory = transaction.subcategory,
            merchant_name = transaction.merchantName,
            note = transaction.note,
            date = transaction.date.toString(),
            type = transaction.type.name,
            latitude = transaction.location?.latitude,
            longitude = transaction.location?.longitude,
            location_name = transaction.location?.name,
            receipt_id = transaction.receiptId,
            is_recurring = if (transaction.isRecurring) 1L else 0L,
            id = transaction.id,
        )
    }

    override suspend fun delete(id: String) {
        db.transactionQueries.delete(id)
    }

    override suspend fun count(): Long {
        return db.transactionQueries.count().executeAsOne()
    }
}
