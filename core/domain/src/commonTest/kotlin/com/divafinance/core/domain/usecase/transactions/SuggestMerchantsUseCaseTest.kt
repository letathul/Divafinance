package com.divafinance.core.domain.usecase.transactions

import com.divafinance.core.testing.fake.FakeTransactionRepository
import com.divafinance.core.testing.fake.TestData
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SuggestMerchantsUseCaseTest {

    private val repo = FakeTransactionRepository()
    private val useCase = SuggestMerchantsUseCase(repo)

    private fun seed(vararg merchants: String) {
        repo.setTransactions(
            merchants.mapIndexed { i, name ->
                TestData.transaction(id = "t$i", merchantName = name)
            }
        )
    }

    @Test
    fun returnsNothingWithoutHistory() = runTest {
        assertEquals(emptyList(), useCase())
    }

    @Test
    fun listsMostUsedMerchantsWhenNothingIsTyped() = runTest {
        seed("Tesco", "Tesco", "Tesco", "Costa", "Costa", "Shell")

        assertEquals(listOf("Tesco", "Costa", "Shell"), useCase())
    }

    @Test
    fun respectsTheLimit() = runTest {
        seed("Tesco", "Costa", "Shell", "Boots", "Greggs", "Pret")

        assertEquals(3, useCase(limit = 3).size)
    }

    @Test
    fun filtersByWhatWasTyped() = runTest {
        seed("Tesco", "Costa", "Shell")

        assertEquals(listOf("Costa"), useCase("cos"))
    }

    @Test
    fun matchesCaseInsensitively() = runTest {
        seed("Blue Bottle")

        assertEquals(listOf("Blue Bottle"), useCase("BLUE"))
    }

    /** Typing "co" should offer "Costa" before "Tesco". */
    @Test
    fun ranksPrefixMatchesAboveMidStringOnes() = runTest {
        // Tesco is used more often, so only the prefix rule can put Costa first.
        seed("Tesco", "Tesco", "Tesco", "Costa")

        assertEquals(listOf("Costa", "Tesco"), useCase("co"))
    }

    @Test
    fun usageStillBreaksTiesWithinAMatchTier() = runTest {
        seed("Costa", "Costa", "Corner Shop")

        assertEquals(listOf("Costa", "Corner Shop"), useCase("co"))
    }

    @Test
    fun doesNotSuggestWhatIsAlreadyTypedInFull() = runTest {
        seed("Costa", "Corner Shop")

        assertTrue(useCase("Costa").none { it == "Costa" })
    }

    @Test
    fun returnsNothingWhenNothingMatches() = runTest {
        seed("Tesco", "Costa")

        assertEquals(emptyList(), useCase("zzz"))
    }

    @Test
    fun ignoresBlankMerchantNames() = runTest {
        repo.setTransactions(
            listOf(
                TestData.transaction(id = "1", merchantName = "   "),
                TestData.transaction(id = "2", merchantName = null),
                TestData.transaction(id = "3", merchantName = "Tesco"),
            )
        )

        assertEquals(listOf("Tesco"), useCase())
    }

    @Test
    fun trimsSurroundingWhitespaceFromTheQuery() = runTest {
        seed("Costa")

        assertEquals(listOf("Costa"), useCase("  cos  "))
    }
}
