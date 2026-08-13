package com.divafinance.core.data.repository

import com.divafinance.core.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAll(): Flow<List<Account>>
    suspend fun getById(id: String): Account?
    suspend fun insert(account: Account)
    suspend fun update(account: Account)
    suspend fun updateBalance(id: String, balance: Double)
    suspend fun delete(id: String)
    suspend fun count(): Long
}
