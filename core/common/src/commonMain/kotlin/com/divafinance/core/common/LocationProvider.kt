package com.divafinance.core.common

/**
 * A point on the earth. Deliberately not `core:model`'s `LocationTag` — this module
 * depends on nothing internal, and callers map between the two.
 */
data class Coordinates(
    val latitude: Double,
    val longitude: Double,
)

/**
 * One-shot device location, plus a best-effort name for a point.
 *
 * Every method is allowed to return null and none of them throw. Location is an optional
 * enrichment on a transaction, so a missing permission, a disabled provider, an absent
 * geocoder backend, or simply no fix in time must all degrade to "no location" rather
 * than interrupting an entry.
 *
 * Backed by the platform location APIs rather than Google Play Services: adding
 * `play-services-location` here would push GMS into every module that depends on
 * `core:common`, which is all of them, and a single fix for a receipt does not need
 * fused-provider machinery.
 */
interface LocationSource {

    /** False when the platform has no location support at all. */
    fun isAvailable(): Boolean

    /** Whether location permission has already been granted. Never prompts. */
    fun hasPermission(): Boolean

    /**
     * A single fix, or null. Returns quickly with a cached position when one is recent
     * enough, since an entry should not wait on GPS.
     */
    suspend fun currentCoordinates(): Coordinates?

    /**
     * Reverse-geocodes to something a person would recognise, e.g. a street and locality.
     * Null when no geocoding backend is present — common on non-GMS devices.
     */
    suspend fun describe(coordinates: Coordinates): String?
}

/**
 * The platform implementation of [LocationSource].
 *
 * Callers depend on the interface, not this class: an `expect class` cannot be subclassed,
 * so anything taking `LocationProvider` directly would be impossible to exercise in a test
 * on a target whose actual always reports unavailable.
 */
expect class LocationProvider : LocationSource