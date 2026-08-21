package com.divafinance.core.domain.usecase.cards

import com.divafinance.core.testing.fake.FakeCardRepository
import com.divafinance.core.testing.fake.FakeRewardRepository
import com.divafinance.core.testing.fake.TestData
import com.divafinance.core.model.enums.SpendingCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateCardUseCaseTest {

    private val cardRepo = FakeCardRepository()
    private val rewardRepo = FakeRewardRepository()
    private val useCase = UpdateCardUseCase(cardRepo, rewardRepo)

    @Test
    fun updatesCardAndReplacesRules() = runTest {
        val oldRules = listOf(TestData.rule(id = "r-old", cardId = "c1"))
        cardRepo.insert(TestData.card(id = "c1", name = "Old Name"))
        rewardRepo.setRules(oldRules)

        val newRules = listOf(
            TestData.rule(id = "r-new1", cardId = "c1", category = SpendingCategory.TRAVEL),
            TestData.rule(id = "r-new2", cardId = "c1", category = SpendingCategory.GROCERIES),
        )
        val updatedCard = TestData.card(id = "c1", name = "New Name", rewardRules = newRules)

        useCase(updatedCard)

        val cards = cardRepo.getAll().first()
        assertEquals("New Name", cards[0].name)

        val rules = rewardRepo.getByCardId("c1")
        assertEquals(2, rules.size)
        assertEquals(SpendingCategory.TRAVEL, rules[0].category)
    }
}
