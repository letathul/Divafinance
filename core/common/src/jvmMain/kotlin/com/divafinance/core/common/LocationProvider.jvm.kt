package com.divafinance.core.common

/**
 * No device location on the desktop JVM target — it exists to host tests. Reporting
 * unavailable exercises the same "save without a location" path a phone takes when
 * permission is denied.
 */
actual class LocationProvider : LocationSource {
    override fun isAvailable(): Boolean = false
    override fun hasPermission(): Boolean = false
    override suspend fun currentCoordinates(): Coordinates? = null
    override suspend fun describe(coordinates: Coordinates): String? = null
}
