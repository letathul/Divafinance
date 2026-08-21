package com.divafinance.core.domain.usecase.location

import com.divafinance.core.data.repository.TransactionRepository
import com.divafinance.core.domain.engine.NearbyPlace
import com.divafinance.core.domain.engine.NearbyPlaceEngine
import com.divafinance.core.domain.premium.PremiumGate
import com.divafinance.core.model.LocationTag

/**
 * Shops the user has spent at before, near where they are now.
 *
 * Premium-gated. The suggestion itself is computed locally from their own history and
 * costs nothing to produce, so the gate is a product decision rather than a cost one —
 * and it is the seam where a paid POI provider would slot in to cover shops never
 * visited.
 *
 * Returns empty rather than failing for every "can't do this" case: not premium, no
 * coordinates, no history with locations.
 */
class SuggestNearbyPlacesUseCase(
    private val transactionRepository: TransactionRepository,
    private val premiumGate: PremiumGate,
    private val engine: NearbyPlaceEngine = NearbyPlaceEngine(),
) {
    suspend operator fun invoke(origin: LocationTag?, limit: Int = 5): List<NearbyPlace> {
        if (origin == null) return emptyList()
        if (!premiumGate.isPremiumNow()) return emptyList()

        // Only rows that actually carry coordinates matter here, and there are far fewer
        // of them than transactions overall.
        val located = runCatching { transactionRepository.getWithLocation() }
            .getOrDefault(emptyList())

        return engine.nearby(history = located, origin = origin, limit = limit)
    }
}
