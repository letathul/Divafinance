package com.divafinance.core.common

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.CLPlacemark
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.Foundation.NSError
import platform.Foundation.timeIntervalSinceNow
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.math.abs

/** A cached fix older than this is stale enough to be worth asking for a fresh one. */
private const val MAX_CACHED_AGE_SECONDS = 120.0

/** An entry must not stall waiting for GPS; without a fix by now, save without one. */
private const val FIX_TIMEOUT_MS = 8_000L

/**
 * CoreLocation-backed [LocationSource].
 *
 * `CLLocationManager` is delegate-driven and expects a run loop, so the manager is created
 * and driven on the main dispatcher; the delegate is held for the lifetime of the request
 * because CoreLocation keeps only a weak reference to it and would otherwise deliver its
 * callback to a collected object.
 */
@OptIn(ExperimentalForeignApi::class)
actual class LocationProvider : LocationSource {

    /**
     * Strong reference to the in-flight delegate.
     *
     * `CLLocationManager.delegate` is a weak property, so a delegate held only by the
     * local scope of the request can be collected before CoreLocation calls back — the
     * fix would then simply never arrive, and the request would fall out on its timeout
     * looking like "no signal".
     */
    private var pendingDelegate: SingleFixDelegate? = null

    override fun isAvailable(): Boolean = CLLocationManager.locationServicesEnabled()

    override fun hasPermission(): Boolean {
        val status = CLLocationManager().authorizationStatus
        return status == kCLAuthorizationStatusAuthorizedWhenInUse ||
            status == kCLAuthorizationStatusAuthorizedAlways
    }

    override suspend fun currentCoordinates(): Coordinates? {
        if (!hasPermission()) return null

        return withContext(Dispatchers.Main) {
            val manager = CLLocationManager()

            // A recent cached fix avoids waking the hardware for an entry that is being
            // typed right now.
            manager.location?.takeIf { it.isRecent() }?.let { return@withContext it.toCoordinates() }

            withTimeoutOrNull(FIX_TIMEOUT_MS) { requestSingleFix(manager) }?.toCoordinates()
                .also { pendingDelegate = null }
        }
    }

    /**
     * One fix via `requestLocation()`, which CoreLocation delivers exactly once.
     */
    private suspend fun requestSingleFix(manager: CLLocationManager): CLLocation? =
        suspendCancellableCoroutine { continuation ->
            val handler = SingleFixDelegate { location ->
                if (continuation.isActive) continuation.resume(location)
            }
            pendingDelegate = handler
            manager.delegate = handler

            continuation.invokeOnCancellation {
                manager.stopUpdatingLocation()
                manager.delegate = null
                pendingDelegate = null
            }
            manager.requestLocation()
        }

    override suspend fun describe(coordinates: Coordinates): String? {
        val location = CLLocation(
            latitude = coordinates.latitude,
            longitude = coordinates.longitude,
        )

        val placemark = withTimeoutOrNull(FIX_TIMEOUT_MS) {
            suspendCancellableCoroutine<CLPlacemark?> { continuation ->
                val geocoder = CLGeocoder()
                continuation.invokeOnCancellation { geocoder.cancelGeocode() }
                geocoder.reverseGeocodeLocation(location) { placemarks, _ ->
                    if (!continuation.isActive) return@reverseGeocodeLocation
                    continuation.resume(placemarks?.firstOrNull() as? CLPlacemark)
                }
            }
        } ?: return null

        // Prefer a named venue over a bare street number, matching the Android actual.
        return listOfNotNull(
            placemark.name?.takeIf { it.isNotBlank() && it != placemark.subThoroughfare },
            placemark.thoroughfare,
            placemark.locality,
        ).distinct().joinToString(", ").takeIf { it.isNotBlank() }
    }
}

/**
 * `CLLocationManagerDelegateProtocol` is an Objective-C protocol, so it needs a real
 * `NSObject` subclass rather than a lambda.
 */
private class SingleFixDelegate(
    private val onResult: (CLLocation?) -> Unit,
) : NSObject(), CLLocationManagerDelegateProtocol {

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        onResult(didUpdateLocations.lastOrNull() as? CLLocation)
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        // No location is a normal outcome here, not an error worth surfacing.
        onResult(null)
    }
}

/** `timeIntervalSinceNow` is negative for a past timestamp. */
private fun CLLocation.isRecent(): Boolean =
    abs(timestamp.timeIntervalSinceNow) <= MAX_CACHED_AGE_SECONDS

@OptIn(ExperimentalForeignApi::class)
private fun CLLocation.toCoordinates(): Coordinates =
    coordinate.useContents { Coordinates(latitude = latitude, longitude = longitude) }
