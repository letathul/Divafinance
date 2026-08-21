package com.divafinance.core.testing.fake

import com.divafinance.core.data.repository.CardRepository
import com.divafinance.core.model.CreditCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeCardRepository : CardRepository {
    private val cards = MutableStateFlow<List<CreditCard>>(emptyList())
    private val rewardRepo = FakeRewardRepository()

    override fun getAll(): Flow<List<CreditCard>> = cards

    override suspend fun getById(id: String): CreditCard? {
        val card = cards.value.find { it.id == id } ?: return null
        val rules = rewardRepo.getByCardId(id)
        return card.copy(rewardRules = rules)
    }

    override suspend fun getByAccountId(accountId: String): List<CreditCard> =
        cards.value.filter { it.accountId == accountId }

    override suspend fun insert(card: CreditCard) {
        cards.value = cards.value + card
    }

    override suspend fun update(card: CreditCard) {
        cards.value = cards.value.map { if (it.id == card.id) card else it }
    }

    override suspend fun updateBalance(id: String, balance: Double) {
        cards.value = cards.value.map {
            if (it.id == id) it.copy(currentBalance = balance) else it
        }
    }

    override suspend fun delete(id: String) {
        cards.value = cards.value.filter { it.id != id }
    }

    override suspend fun count(): Long = cards.value.size.toLong()

    fun setCards(list: List<CreditCard>) {
        cards.value = list
    }
}
