package com.divafinance.core.domain.usecase.cards

import com.divafinance.core.testing.fake.FakeCardRepository
import com.divafinance.core.testing.fake.FakeRewardRepository
import com.divafinance.core.testing.fake.TestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AddCardUseCaseTest {

    private val cardRepo = FakeCardRepository()
    private val rewardRepo = FakeRewardRepository()
    private val useCase = AddCardUseCase(cardRepo, rewardRepo)

    @Test
    fun insertsCardAndRewardRules() = runTest {
        val rules = listOf(
            TestData.rule(id = "r1", cardId = "c1"),
            TestData.rule(id = "r2", cardId = "c1"),
        )
        val card = TestData.card(id = "c1", rewardRules = rules)

        useCase(card)

        val savedCards = cardRepo.getAll().first()
        assertEquals(1, savedCards.size)
        assertEquals("c1", savedCards[0].id)

        val savedRules = rewardRepo.getByCardId("c1")
        assertEquals(2, savedRules.size)
    }

    @Test
    fun insertsCardWithNoRules() = runTest {
        val card = TestData.card(id = "c1", rewardRules = emptyList())

        useCase(card)

        val savedCards = cardRepo.getAll().first()
        assertEquals(1, savedCards.size)

        val savedRules = rewardRepo.getByCardId("c1")
        assertEquals(0, savedRules.size)
    }
}
