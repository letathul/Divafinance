package com.divafinance.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.divafinance.core.database.DivaFinanceDb
import com.divafinance.core.model.CreditCard
import com.divafinance.core.model.enums.CardNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

class CardRepositoryImpl(
    private val db: DivaFinanceDb,
    private val rewardRepository: RewardRepository,
) : CardRepository {

    override fun getAll(): Flow<List<CreditCard>> {
        return db.creditCardQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }
    }

    override suspend fun getById(id: String): CreditCard? {
        val card = db.creditCardQueries.selectById(id).executeAsOneOrNull()?.toDomain() ?: return null
        val rules = rewardRepository.getByCardId(id)
        return card.copy(rewardRules = rules)
    }

    override suspend fun getByAccountId(accountId: String): List<CreditCard> {
        return db.creditCardQueries.selectByAccountId(accountId)
            .executeAsList()
            .map { it.toDomain() }
    }

    override suspend fun insert(card: CreditCard) {
        db.creditCardQueries.insert(
            id = card.id,
            account_id = card.accountId,
            name = card.name,
            last_four = card.lastFour,
            network = card.network.name,
            color = card.color,
            credit_limit = card.creditLimit,
            current_balance = card.currentBalance,
            statement_date = card.statementDate?.toLong(),
            due_date = card.dueDate?.toLong(),
            annual_fee = card.annualFee,
            is_active = if (card.isActive) 1L else 0L,
            created_at = card.createdAt.toString(),
            updated_at = card.updatedAt.toString(),
        )
    }

    override suspend fun update(card: CreditCard) {
        db.creditCardQueries.update(
            name = card.name,
            last_four = card.lastFour,
            network = card.network.name,
            color = card.color,
            credit_limit = card.creditLimit,
            current_balance = card.currentBalance,
            statement_date = card.statementDate?.toLong(),
            due_date = card.dueDate?.toLong(),
            annual_fee = card.annualFee,
            is_active = if (card.isActive) 1L else 0L,
            updated_at = card.updatedAt.toString(),
            id = card.id,
        )
    }

    override suspend fun updateBalance(id: String, balance: Double) {
        db.creditCardQueries.updateBalance(
            current_balance = balance,
            updated_at = kotlin.time.Clock.System.now().toString(),
            id = id,
        )
    }

    override suspend fun delete(id: String) {
        db.creditCardQueries.delete(id)
    }

    override suspend fun count(): Long {
        return db.creditCardQueries.count().executeAsOne()
    }
}

private fun com.divafinance.core.database.CreditCard.toDomain() = CreditCard(
    id = id,
    accountId = account_id,
    name = name,
    lastFour = last_four,
    network = runCatching { CardNetwork.valueOf(network) }.getOrDefault(CardNetwork.OTHER),
    color = color,
    creditLimit = credit_limit,
    currentBalance = current_balance,
    statementDate = statement_date?.toInt(),
    dueDate = due_date?.toInt(),
    annualFee = annual_fee,
    isActive = is_active == 1L,
    createdAt = Instant.parse(created_at),
    updatedAt = Instant.parse(updated_at),
)
