package com.divafinance.core.domain.usecase.location

import com.divafinance.core.domain.premium.PremiumGate
import com.divafinance.core.model.LocationTag
import com.divafinance.core.testing.fake.FakeTransactionRepository
import com.divafinance.core.testing.fake.TestData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SuggestNearbyPlacesUseCaseTest {

    private class FakePremiumGate(premium: Boolean) : PremiumGate {
        private val state = MutableStateFlow(premium)
        override val isPremium: Flow<Boolean> = state
        override suspend fun isPremiumNow(): Boolean = state.value
    }

    private val repo = FakeTransactionRepository()
    private val origin = LocationTag(51.5080, -0.1281)

    private fun useCase(premium: Boolean = true) =
        SuggestNearbyPlacesUseCase(repo, FakePremiumGate(premium))

    private fun seedNearbyShop() {
        repo.setTransactions(
            listOf(
                TestData.transaction(id = "1", merchantName = "Blue Bottle", location = origin),
            )
        )
    }

    @Test
    fun suggestsAPreviouslyVisitedShop() = runTest {
        seedNearbyShop()

        assertEquals(listOf("Blue Bottle"), useCase()(origin).map { it.name })
    }

    /** The gate is the point of the feature being paid; without it there is nothing to sell. */
    @Test
    fun suggestsNothingWithoutPremium() = runTest {
        seedNearbyShop()

        assertEquals(emptyList(), useCase(premium = false)(origin))
    }

    @Test
    fun suggestsNothingWithoutAKnownPosition() = runTest {
        seedNearbyShop()

        assertEquals(emptyList(), useCase()(origin = null))
    }

    @Test
    fun suggestsNothingWhenNoHistoryHasCoordinates() = runTest {
        repo.setTransactions(
            listOf(TestData.transaction(id = "1", merchantName = "Blue Bottle", location = null))
        )

        assertEquals(emptyList(), useCase()(origin))
    }

    @Test
    fun respectsTheLimit() = runTest {
        repo.setTransactions(
            (1..8).map {
                TestData.transaction(
                    id = "$it",
                    merchantName = "Shop $it",
                    location = LocationTag(origin.latitude, origin.longitude),
                )
            }
        )

        assertTrue(useCase()(origin, limit = 3).size == 3)
    }
}
