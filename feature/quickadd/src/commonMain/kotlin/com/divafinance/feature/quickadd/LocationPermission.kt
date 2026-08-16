package com.divafinance.feature.quickadd

import androidx.compose.runtime.Composable

/**
 * Asks the OS for location permission.
 *
 * This lives in the feature rather than `core:common` alongside `LocationProvider`
 * deliberately: requesting a runtime permission needs an Activity result launcher, which
 * means Compose and `androidx.activity`, and `core:common` is a non-Compose module that
 * every other module depends on.
 */
fun interface LocationPermissionRequester {
    /** Prompts if needed. [onResult] receives whether location is usable afterwards. */
    fun request(onResult: (granted: Boolean) -> Unit)
}

/**
 * A requester bound to the current screen. On platforms with no permission model, or no
 * location support, this reports denial rather than pretending to prompt.
 */
@Composable
expect fun rememberLocationPermissionRequester(): LocationPermissionRequester
