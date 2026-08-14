package com.divafinance.core.domain.fake

import com.divafinance.core.data.repository.RewardRepository
import com.divafinance.core.model.CardRewardRule
import com.divafinance.core.model.enums.SpendingCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeRewardRepository : RewardRepository {
    private val rules = MutableStateFlow<List<CardRewardRule>>(emptyList())

    override fun getAll(): Flow<List<CardRewardRule>> = rules

    override suspend fun getByCardId(cardId: String): List<CardRewardRule> =
        rules.value.filter { it.cardId == cardId }

    override suspend fun getByCategory(category: SpendingCategory): List<CardRewardRule> =
        rules.value.filter { it.category == category }

    override suspend fun insert(rule: CardRewardRule) {
        rules.value = rules.value + rule
    }

    override suspend fun update(rule: CardRewardRule) {
        rules.value = rules.value.map { if (it.id == rule.id) rule else it }
    }

    override suspend fun deleteByCardId(cardId: String) {
        rules.value = rules.value.filter { it.cardId != cardId }
    }

    override suspend fun delete(id: String) {
        rules.value = rules.value.filter { it.id != id }
    }

    fun setRules(list: List<CardRewardRule>) {
        rules.value = list
    }
}
