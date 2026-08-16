package com.divafinance.core.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** A cached fix older than this is stale enough to be worth asking for a fresh one. */
private const val MAX_CACHED_AGE_MS = 2 * 60 * 1000L

/** An entry must not stall waiting for GPS; without a fix by now, save without one. */
private const val FRESH_FIX_TIMEOUT_MS = 8_000L

actual class LocationProvider(private val context: Context) : LocationSource {

    // Resolved through the platform rather than ContextCompat: core:common is depended on
    // by every module, so it is not the place to introduce an androidx dependency.
    private val locationManager: LocationManager?
        get() = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    override fun isAvailable(): Boolean = locationManager != null

    override fun hasPermission(): Boolean {
        // Coarse is enough to identify a shop; fine is better but not required.
        return hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun hasPermission(permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    override suspend fun currentCoordinates(): Coordinates? {
        if (!hasPermission()) return null
        val manager = locationManager ?: return null

        lastKnownFix(manager)?.let { return it.toCoordinates() }

        // No usable cached fix, so ask the hardware — bounded, because the caller is a
        // person waiting to save a transaction.
        return withTimeoutOrNull(FRESH_FIX_TIMEOUT_MS) { freshFix(manager) }?.toCoordinates()
    }

    /** Newest sufficiently recent fix across providers. */
    private fun lastKnownFix(manager: LocationManager): Location? {
        val now = System.currentTimeMillis()
        return enabledProviders(manager)
            .mapNotNull { provider ->
                @Suppress("MissingPermission")
                runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            }
            .filter { now - it.time <= MAX_CACHED_AGE_MS }
            .maxByOrNull { it.time }
    }

    private suspend fun freshFix(manager: LocationManager): Location? {
        val provider = enabledProviders(manager).firstOrNull() ?: return null

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            suspendCancellableCoroutine { continuation ->
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                @Suppress("MissingPermission")
                runCatching {
                    manager.getCurrentLocation(
                        provider,
                        signal,
                        context.mainExecutor,
                    ) { location -> if (continuation.isActive) continuation.resume(location) }
                }.onFailure { if (continuation.isActive) continuation.resume(null) }
            }
        } else {
            // getCurrentLocation is API 30+; below that a single update is the equivalent.
            suspendCancellableCoroutine { continuation ->
                val listener = SingleUpdateListener { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
                continuation.invokeOnCancellation {
                    runCatching { manager.removeUpdates(listener) }
                }
                @Suppress("MissingPermission")
                runCatching {
                    manager.requestSingleUpdate(provider, listener, context.mainLooper)
                }.onFailure { if (continuation.isActive) continuation.resume(null) }
            }
        }
    }

    /** GPS first when enabled, then network — ordered by accuracy, not availability. */
    private fun enabledProviders(manager: LocationManager): List<String> =
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }

    override suspend fun describe(coordinates: Coordinates): String? {
        // Absent on AOSP builds and anything without a geocoding backend.
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context)

        val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // The synchronous overload is deprecated on 33+ in favour of this callback.
            withTimeoutOrNull(FRESH_FIX_TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    runCatching {
                        geocoder.getFromLocation(
                            coordinates.latitude,
                            coordinates.longitude,
                            1,
                        ) { results ->
                            if (continuation.isActive) continuation.resume(results.firstOrNull())
                        }
                    }.onFailure { if (continuation.isActive) continuation.resume(null) }
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching {
                    geocoder.getFromLocation(coordinates.latitude, coordinates.longitude, 1)
                        ?.firstOrNull()
                }.getOrNull()
            }
        } ?: return null

        // Prefer a named feature (often the venue) over a bare street number.
        return listOfNotNull(
            address.featureName?.takeIf { it.isNotBlank() && it != address.subThoroughfare },
            address.thoroughfare,
            address.locality,
        ).distinct().joinToString(", ").takeIf { it.isNotBlank() }
    }
}

/**
 * `LocationListener` is a Java interface with three legacy methods that are abstract below
 * API 30, so it cannot be written as a lambda on the minSdk this app supports.
 */
private class SingleUpdateListener(
    private val onLocation: (Location?) -> Unit,
) : android.location.LocationListener {
    override fun onLocationChanged(location: Location) = onLocation(location)
    override fun onProviderEnabled(provider: String) = Unit
    override fun onProviderDisabled(provider: String) = onLocation(null)
    @Deprecated("Required below API 30")
    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
}

private fun Location.toCoordinates() = Coordinates(latitude, longitude)
