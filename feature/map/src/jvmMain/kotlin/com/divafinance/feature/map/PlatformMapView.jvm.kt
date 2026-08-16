package com.divafinance.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The desktop JVM target exists only to host Compose tests — there is no map SDK for it.
 * Reporting unavailable routes callers to [MapFallbackScreen], which is the same path a
 * device without the on-demand map module takes, so tests exercise a real code path.
 */
@Composable
actual fun PlatformMapView(
    locations: List<LocationSpending>,
    onLocationClick: (LocationSpending) -> Unit,
    modifier: Modifier,
) {
    // Never composed: isPlatformMapAvailable() is false, so callers show the fallback.
}

actual fun isPlatformMapAvailable(): Boolean = false
