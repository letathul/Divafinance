package com.divafinance.core.data.repository

import com.divafinance.core.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface TransactionRepository {
    fun getAll(): Flow<List<Transaction>>
    suspend fun getById(id: String): Transaction?
    suspend fun getByAccountId(accountId: String): List<Transaction>
    suspend fun getByCategory(category: String): List<Transaction>
    suspend fun getByDateRange(startDate: LocalDate, endDate: LocalDate): List<Transaction>
    suspend fun getByCategoryAndDateRange(category: String, startDate: LocalDate, endDate: LocalDate): List<Transaction>
    suspend fun getWithLocation(): List<Transaction>

    /** Distinct merchant names, most-used first. Backs entry autocomplete. */
    suspend fun getKnownMerchants(): List<String>
    suspend fun getSpendingByCategory(startDate: LocalDate, endDate: LocalDate): Map<String, Double>
    suspend fun getTotalSpending(startDate: LocalDate, endDate: LocalDate): Double?
    suspend fun insert(transaction: Transaction)
    suspend fun update(transaction: Transaction)
    suspend fun delete(id: String)
    suspend fun count(): Long
}
