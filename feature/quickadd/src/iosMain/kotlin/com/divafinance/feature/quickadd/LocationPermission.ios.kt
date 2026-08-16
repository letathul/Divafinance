package com.divafinance.feature.quickadd

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * No runtime location permission flow on this target, so the sheet takes the same path as
 * a denial: the toggle stays off and the entry saves without a location.
 */
@Composable
actual fun rememberLocationPermissionRequester(): LocationPermissionRequester =
    remember { LocationPermissionRequester { onResult -> onResult(false) } }
