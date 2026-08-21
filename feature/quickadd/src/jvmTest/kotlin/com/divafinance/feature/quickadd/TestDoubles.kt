package com.divafinance.feature.quickadd

import com.divafinance.core.common.Coordinates
import com.divafinance.core.common.LocationSource
import com.divafinance.core.domain.premium.PremiumGate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePremiumGate(premium: Boolean = false) : PremiumGate {
    private val state = MutableStateFlow(premium)
    override val isPremium: Flow<Boolean> = state
    override suspend fun isPremiumNow(): Boolean = state.value
}

/**
 * The reason [com.divafinance.core.common.LocationSource] exists: `LocationProvider` is an
 * `expect class` and cannot be subclassed, and its JVM actual always reports unavailable,
 * so a test could otherwise never reach the capture path.
 */
class FakeLocationSource(
    var coordinates: Coordinates? = null,
    var description: String? = null,
    private var available: Boolean = true,
    private var permitted: Boolean = true,
) : LocationSource {
    override fun isAvailable(): Boolean = available
    override fun hasPermission(): Boolean = permitted
    override suspend fun currentCoordinates(): Coordinates? = coordinates
    override suspend fun describe(coordinates: Coordinates): String? = description
}
