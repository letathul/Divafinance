package com.divafinance.core.common

/**
 * Stub. iOS is not an active target yet (the app target is commented out in the
 * convention plugins), but every `expect` still needs an iOS actual or the framework
 * fails at link time rather than compile time.
 *
 * A real implementation is CLLocationManager for the fix and CLGeocoder for the name.
 */
actual class LocationProvider : LocationSource {
    override fun isAvailable(): Boolean = false
    override fun hasPermission(): Boolean = false
    override suspend fun currentCoordinates(): Coordinates? = null
    override suspend fun describe(coordinates: Coordinates): String? = null
}
