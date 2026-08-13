package com.divafinance.core.data.repository

import com.divafinance.core.model.CardRewardRule
import com.divafinance.core.model.enums.SpendingCategory
import kotlinx.coroutines.flow.Flow

interface RewardRepository {
    fun getAll(): Flow<List<CardRewardRule>>
    suspend fun getByCardId(cardId: String): List<CardRewardRule>
    suspend fun getByCategory(category: SpendingCategory): List<CardRewardRule>
    suspend fun insert(rule: CardRewardRule)
    suspend fun update(rule: CardRewardRule)
    suspend fun deleteByCardId(cardId: String)
    suspend fun delete(id: String)
}
