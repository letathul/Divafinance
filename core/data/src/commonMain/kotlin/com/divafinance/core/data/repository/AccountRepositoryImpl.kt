package com.divafinance.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.divafinance.core.database.DivaFinanceDb
import com.divafinance.core.model.Account
import com.divafinance.core.model.enums.AccountType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

class AccountRepositoryImpl(
    private val db: DivaFinanceDb
) : AccountRepository {

    override fun getAll(): Flow<List<Account>> {
        return db.accountQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }
    }

    override suspend fun getById(id: String): Account? {
        return db.accountQueries.selectById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun insert(account: Account) {
        db.accountQueries.insert(
            id = account.id,
            name = account.name,
            type = account.type.name,
            currency = account.currency,
            balance = account.balance,
            color = account.color,
            icon = account.icon,
            is_active = if (account.isActive) 1L else 0L,
            created_at = account.createdAt.toString(),
            updated_at = account.updatedAt.toString(),
        )
    }

    override suspend fun update(account: Account) {
        db.accountQueries.update(
            name = account.name,
            type = account.type.name,
            currency = account.currency,
            balance = account.balance,
            color = account.color,
            icon = account.icon,
            is_active = if (account.isActive) 1L else 0L,
            updated_at = account.updatedAt.toString(),
            id = account.id,
        )
    }

    override suspend fun updateBalance(id: String, balance: Double) {
        db.accountQueries.updateBalance(
            balance = balance,
            updated_at = kotlin.time.Clock.System.now().toString(),
            id = id,
        )
    }

    override suspend fun delete(id: String) {
        db.accountQueries.delete(id)
    }

    override suspend fun count(): Long {
        return db.accountQueries.count().executeAsOne()
    }
}

private fun com.divafinance.core.database.Account.toDomain() = Account(
    id = id,
    name = name,
    type = AccountType.valueOf(type),
    currency = currency,
    balance = balance,
    color = color,
    icon = icon,
    isActive = is_active == 1L,
    createdAt = Instant.parse(created_at),
    updatedAt = Instant.parse(updated_at),
)
