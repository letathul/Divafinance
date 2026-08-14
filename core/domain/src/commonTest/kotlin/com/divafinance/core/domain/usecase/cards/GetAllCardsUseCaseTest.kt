package com.divafinance.core.domain.usecase.cards

import com.divafinance.core.domain.fake.FakeCardRepository
import com.divafinance.core.domain.fake.TestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetAllCardsUseCaseTest {

    private val cardRepo = FakeCardRepository()
    private val useCase = GetAllCardsUseCase(cardRepo)

    @Test
    fun returnsAllCards() = runTest {
        cardRepo.setCards(listOf(
            TestData.card(id = "c1", name = "Card A"),
            TestData.card(id = "c2", name = "Card B"),
        ))

        val result = useCase().first()

        assertEquals(2, result.size)
        assertEquals("Card A", result[0].name)
        assertEquals("Card B", result[1].name)
    }

    @Test
    fun returnsEmptyListWhenNoCards() = runTest {
        val result = useCase().first()
        assertTrue(result.isEmpty())
    }
}
