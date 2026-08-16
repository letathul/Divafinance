package com.divafinance.core.testing.fake

import com.divafinance.core.data.repository.AccountRepository
import com.divafinance.core.model.Account
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAccountRepository : AccountRepository {
    private val accounts = MutableStateFlow<List<Account>>(emptyList())

    override fun getAll(): Flow<List<Account>> = accounts.asStateFlow()

    override suspend fun getById(id: String): Account? = accounts.value.find { it.id == id }

    override suspend fun insert(account: Account) {
        accounts.value = accounts.value + account
    }

    override suspend fun update(account: Account) {
        accounts.value = accounts.value.map { if (it.id == account.id) account else it }
    }

    override suspend fun updateBalance(id: String, balance: Double) {
        accounts.value = accounts.value.map {
            if (it.id == id) it.copy(balance = balance) else it
        }
    }

    override suspend fun delete(id: String) {
        accounts.value = accounts.value.filterNot { it.id == id }
    }

    override suspend fun count(): Long = accounts.value.size.toLong()

    fun setAccounts(list: List<Account>) {
        accounts.value = list
    }
}
