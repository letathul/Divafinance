package com.divafinance.core.domain.engine

import com.divafinance.core.model.LocationTag
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.enums.SpendingCategory
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

/** A place the user has spent at before, near where they are now. */
data class NearbyPlace(
    val name: String,
    val category: SpendingCategory,
    val location: LocationTag,
    val visits: Int,
    val metresAway: Double,
)

/**
 * Finds shops the user has already spent at, near a given point.
 *
 * Their own history is the whole data source. For a personal finance app that covers most
 * real entries — people return to the same handful of shops — and it costs nothing, works
 * offline, and sends no location anywhere. The trade-off is that a shop never visited
 * before cannot be suggested; a `PlaceSearchService` implementation backed by a POI
 * provider would fill that gap.
 *
 * Pure, like [RewardRecommendationEngine] and [CategoryPredictionEngine]: transactions are
 * passed in.
 */
class NearbyPlaceEngine {

    fun nearby(
        history: List<Transaction>,
        origin: LocationTag,
        radiusMetres: Double = DEFAULT_RADIUS_METRES,
        limit: Int = 5,
    ): List<NearbyPlace> {
        val candidates = history.mapNotNull { transaction ->
            val location = transaction.location ?: return@mapNotNull null
            val name = transaction.merchantName?.trim()?.takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null
            Triple(name, transaction, location)
        }

        return candidates
            .groupBy { (name, _, _) -> name.lowercase() }
            .mapNotNull { (_, group) ->
                // Several visits to one shop rarely share coordinates exactly, so the
                // cluster is represented by its mean position.
                val points = group.map { it.third }
                val centre = LocationTag(
                    latitude = points.map { it.latitude }.average(),
                    longitude = points.map { it.longitude }.average(),
                    name = points.firstNotNullOfOrNull { it.name },
                )
                val distance = distanceMetres(origin, centre)
                if (distance > radiusMetres) return@mapNotNull null

                NearbyPlace(
                    // Display the most recent spelling rather than the lowercased key.
                    name = group.maxByOrNull { it.second.date }!!.first,
                    category = group
                        .groupingBy { it.second.category }
                        .eachCount()
                        .maxByOrNull { it.value }!!
                        .key,
                    location = centre,
                    visits = group.size,
                    metresAway = distance,
                )
            }
            // Nearest first, then by how often it has been visited — at equal distance the
            // regular haunt is the better guess.
            .sortedWith(compareBy<NearbyPlace> { it.metresAway }.thenByDescending { it.visits })
            .take(limit)
    }

    /**
     * Equirectangular approximation. At the radius this is used over (a few hundred
     * metres) it is within centimetres of the great-circle distance, and it avoids the
     * trigonometry haversine needs.
     */
    internal fun distanceMetres(a: LocationTag, b: LocationTag): Double {
        val meanLatRadians = (a.latitude + b.latitude) / 2.0 * PI / 180.0
        val deltaLat = (b.latitude - a.latitude) * METRES_PER_DEGREE_LAT
        // Lines of longitude converge towards the poles.
        val deltaLon = (b.longitude - a.longitude) * METRES_PER_DEGREE_LAT * cos(meanLatRadians)
        return kotlin.math.sqrt(deltaLat * deltaLat + deltaLon * deltaLon).let { abs(it) }
    }

    private companion object {
        /** Roughly a city block: close enough to be "here", not the next street over. */
        const val DEFAULT_RADIUS_METRES = 150.0

        /** One degree of latitude, near enough constant everywhere. */
        const val METRES_PER_DEGREE_LAT = 111_320.0
    }
}
