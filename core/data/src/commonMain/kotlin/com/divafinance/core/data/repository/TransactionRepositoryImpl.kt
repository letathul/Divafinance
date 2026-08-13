package com.divafinance.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.divafinance.core.database.DivaFinanceDb
import com.divafinance.core.model.LocationTag
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

class TransactionRepositoryImpl(
    private val db: DivaFinanceDb
) : TransactionRepository {

    override fun getAll(): Flow<List<Transaction>> {
        return db.transactionQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toTransactionDomain() } }
    }

    override suspend fun getById(id: String): Transaction? {
        return db.transactionQueries.selectById(id).executeAsOneOrNull()?.toTransactionDomain()
    }

    override suspend fun getByAccountId(accountId: String): List<Transaction> {
        return db.transactionQueries.selectByAccountId(accountId)
            .executeAsList()
            .map { it.toTransactionDomain() }
    }

    override suspend fun getByCategory(category: String): List<Transaction> {
        return db.transactionQueries.selectByCategory(category)
            .executeAsList()
            .map { it.toTransactionDomain() }
    }

    override suspend fun getByDateRange(startDate: LocalDate, endDate: LocalDate): List<Transaction> {
        return db.transactionQueries.selectByDateRange(startDate.toString(), endDate.toString())
            .executeAsList()
            .map { it.toTransactionDomain() }
    }

    override suspend fun getByCategoryAndDateRange(
        category: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Transaction> {
        return db.transactionQueries.selectByCategoryAndDateRange(
            category, startDate.toString(), endDate.toString()
        ).executeAsList().map { row ->
            Transaction(
                id = row.id,
                accountId = row.account_id,
                cardId = row.card_id,
                amount = row.amount,
                currency = row.currency,
                category = SpendingCategory.valueOf(row.category),
                subcategory = row.subcategory,
                merchantName = row.merchant_name,
                note = row.note,
                date = LocalDate.parse(row.date),
                type = TransactionType.valueOf(row.type),
                location = run {
                    val lat = row.latitude
                    val lon = row.longitude
                    if (lat != null && lon != null) {
                        LocationTag(latitude = lat, longitude = lon, name = row.location_name)
                    } else null
                },
                receiptId = row.receipt_id,
                isRecurring = row.is_recurring == 1L,
                createdAt = Instant.parse(row.created_at),
            )
        }
    }

    override suspend fun getWithLocation(): List<Transaction> {
        return db.transactionQueries.selectWithLocation()
            .executeAsList()
            .map { row ->
                Transaction(
                    id = row.id,
                    accountId = row.account_id,
                    cardId = row.card_id,
                    amount = row.amount,
                    currency = row.currency,
                    category = SpendingCategory.valueOf(row.category),
                    subcategory = row.subcategory,
                    merchantName = row.merchant_name,
                    note = row.note,
                    date = LocalDate.parse(row.date),
                    type = TransactionType.valueOf(row.type),
                    location = run {
                        val lat = row.latitude
                        val lon = row.longitude
                        if (lat != null && lon != null) {
                            LocationTag(latitude = lat, longitude = lon, name = row.location_name)
                        } else null
                    },
                    receiptId = row.receipt_id,
                    isRecurring = row.is_recurring == 1L,
                    createdAt = Instant.parse(row.created_at),
                )
            }
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

internal fun com.divafinance.core.database.DivaTransaction.toTransactionDomain(): Transaction = Transaction(
    id = id,
    accountId = account_id,
    cardId = card_id,
    amount = amount,
    currency = currency,
    category = SpendingCategory.valueOf(category),
    subcategory = subcategory,
    merchantName = merchant_name,
    note = note,
    date = LocalDate.parse(date),
    type = TransactionType.valueOf(type),
    location = run {
        val lat = latitude
        val lon = longitude
        if (lat != null && lon != null) {
            LocationTag(latitude = lat, longitude = lon, name = location_name)
        } else null
    },
    receiptId = receipt_id,
    isRecurring = is_recurring == 1L,
    createdAt = Instant.parse(created_at),
)
