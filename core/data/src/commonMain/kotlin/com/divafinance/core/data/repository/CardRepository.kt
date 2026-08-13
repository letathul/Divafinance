package com.divafinance.core.data.repository

import com.divafinance.core.model.CreditCard
import kotlinx.coroutines.flow.Flow

interface CardRepository {
    fun getAll(): Flow<List<CreditCard>>
    suspend fun getById(id: String): CreditCard?
    suspend fun getByAccountId(accountId: String): List<CreditCard>
    suspend fun insert(card: CreditCard)
    suspend fun update(card: CreditCard)
    suspend fun updateBalance(id: String, balance: Double)
    suspend fun delete(id: String)
    suspend fun count(): Long
}
