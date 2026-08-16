package com.divafinance.core.testing.fake

import com.divafinance.core.data.repository.TransactionRepository
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.enums.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate

class FakeTransactionRepository : TransactionRepository {
    private val transactions = MutableStateFlow<List<Transaction>>(emptyList())

    override fun getAll(): Flow<List<Transaction>> = transactions

    override suspend fun getById(id: String): Transaction? =
        transactions.value.find { it.id == id }

    override suspend fun getByAccountId(accountId: String): List<Transaction> =
        transactions.value.filter { it.accountId == accountId }

    override suspend fun getByCategory(category: String): List<Transaction> =
        transactions.value.filter { it.category.name == category }

    override suspend fun getByDateRange(startDate: LocalDate, endDate: LocalDate): List<Transaction> =
        transactions.value.filter { it.date in startDate..endDate }

    override suspend fun getByCategoryAndDateRange(
        category: String, startDate: LocalDate, endDate: LocalDate
    ): List<Transaction> =
        transactions.value.filter { it.category.name == category && it.date in startDate..endDate }

    override suspend fun getWithLocation(): List<Transaction> =
        transactions.value.filter { it.location != null }

    // Mirrors the real SQL, which subtracts what others owe back. Left summing `amount`,
    // this fake would keep every domain test passing against semantics the database no
    // longer has — and it is the only implementation those tests ever see.
    override suspend fun getSpendingByCategory(startDate: LocalDate, endDate: LocalDate): Map<String, Double> =
        transactions.value
            .filter { it.type == TransactionType.DEBIT && it.date in startDate..endDate }
            .groupBy { it.category.name }
            .mapValues { (_, txs) -> txs.sumOf { it.amount - it.othersShare } }

    override suspend fun getTotalSpending(startDate: LocalDate, endDate: LocalDate): Double? =
        transactions.value
            .filter { it.type == TransactionType.DEBIT && it.date in startDate..endDate }
            .sumOf { it.amount - it.othersShare }
            .takeIf { it > 0.0 }

    override suspend fun insert(transaction: Transaction) {
        transactions.value = transactions.value + transaction
    }

    override suspend fun update(transaction: Transaction) {
        transactions.value = transactions.value.map { if (it.id == transaction.id) transaction else it }
    }

    override suspend fun delete(id: String) {
        transactions.value = transactions.value.filter { it.id != id }
    }

    override suspend fun count(): Long = transactions.value.size.toLong()

    override suspend fun getKnownMerchants(): List<String> =
        transactions.value
            .mapNotNull { it.merchantName?.trim()?.takeIf(String::isNotEmpty) }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }

    fun setTransactions(list: List<Transaction>) {
        transactions.value = list
    }
}
