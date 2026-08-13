package com.divafinance.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.divafinance.core.database.DivaFinanceDb
import com.divafinance.core.model.CardRewardRule
import com.divafinance.core.model.enums.CapPeriod
import com.divafinance.core.model.enums.RewardType
import com.divafinance.core.model.enums.SpendingCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RewardRepositoryImpl(
    private val db: DivaFinanceDb
) : RewardRepository {

    override fun getAll(): Flow<List<CardRewardRule>> {
        return db.rewardRuleQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }
    }

    override suspend fun getByCardId(cardId: String): List<CardRewardRule> {
        return db.rewardRuleQueries.selectByCardId(cardId)
            .executeAsList()
            .map { it.toDomain() }
    }

    override suspend fun getByCategory(category: SpendingCategory): List<CardRewardRule> {
        return db.rewardRuleQueries.selectByCategory(category.name)
            .executeAsList()
            .map {
                CardRewardRule(
                    id = it.id,
                    cardId = it.card_id,
                    category = SpendingCategory.valueOf(it.category),
                    multiplier = it.multiplier,
                    rewardType = RewardType.valueOf(it.reward_type),
                    capAmount = it.cap_amount,
                    capPeriod = it.cap_period?.let { p -> runCatching { CapPeriod.valueOf(p) }.getOrNull() },
                    isActive = it.is_active == 1L,
                )
            }
    }

    override suspend fun insert(rule: CardRewardRule) {
        db.rewardRuleQueries.insert(
            id = rule.id,
            card_id = rule.cardId,
            category = rule.category.name,
            multiplier = rule.multiplier,
            reward_type = rule.rewardType.name,
            cap_amount = rule.capAmount,
            cap_period = rule.capPeriod?.name,
            is_active = if (rule.isActive) 1L else 0L,
        )
    }

    override suspend fun update(rule: CardRewardRule) {
        db.rewardRuleQueries.update(
            category = rule.category.name,
            multiplier = rule.multiplier,
            reward_type = rule.rewardType.name,
            cap_amount = rule.capAmount,
            cap_period = rule.capPeriod?.name,
            is_active = if (rule.isActive) 1L else 0L,
            id = rule.id,
        )
    }

    override suspend fun deleteByCardId(cardId: String) {
        db.rewardRuleQueries.deleteByCardId(cardId)
    }

    override suspend fun delete(id: String) {
        db.rewardRuleQueries.delete(id)
    }
}

private fun com.divafinance.core.database.RewardRule.toDomain() = CardRewardRule(
    id = id,
    cardId = card_id,
    category = SpendingCategory.valueOf(category),
    multiplier = multiplier,
    rewardType = RewardType.valueOf(reward_type),
    capAmount = cap_amount,
    capPeriod = cap_period?.let { runCatching { CapPeriod.valueOf(it) }.getOrNull() },
    isActive = is_active == 1L,
)
