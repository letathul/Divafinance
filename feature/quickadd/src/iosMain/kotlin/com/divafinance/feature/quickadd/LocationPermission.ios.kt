package com.divafinance.feature.quickadd

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.darwin.NSObject

/**
 * CoreLocation asks for permission through `CLLocationManager` rather than an Activity
 * result, so there is no launcher here — the manager is created, the request is made, and
 * the answer arrives on its delegate.
 *
 * Requires `NSLocationWhenInUseUsageDescription` in the app's Info.plist. Without it iOS
 * denies the request outright rather than prompting, which surfaces here as a plain
 * denial and leaves the sheet saving without a location.
 */
@Composable
actual fun rememberLocationPermissionRequester(): LocationPermissionRequester {
    // Held across recompositions: the delegate must outlive the request, since
    // CLLocationManager.delegate is weak.
    val holder = remember { IosPermissionHolder() }

    return remember(holder) {
        LocationPermissionRequester { onResult -> holder.request(onResult) }
    }
}

private class IosPermissionHolder {
    private val manager = CLLocationManager()
    private var delegate: AuthorizationDelegate? = null

    fun request(onResult: (Boolean) -> Unit) {
        if (manager.authorizationStatus.isGranted()) {
            onResult(true)
            return
        }
        // Anything other than "not yet asked" means the user has already decided, and iOS
        // will not prompt again — sending them to Settings is the only remaining path.
        if (manager.authorizationStatus != kCLAuthorizationStatusNotDetermined) {
            onResult(false)
            return
        }

        val handler = AuthorizationDelegate { status ->
            onResult(status.isGranted())
            delegate = null
        }
        delegate = handler
        manager.delegate = handler
        manager.requestWhenInUseAuthorization()
    }
}

/** Fires once the user answers the system prompt. */
private class AuthorizationDelegate(
    private val onDecided: (Int) -> Unit,
) : NSObject(), CLLocationManagerDelegateProtocol {

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        // Also fires while the prompt is still up on some versions; the undetermined
        // state is not an answer, so it is ignored rather than reported as a refusal.
        if (manager.authorizationStatus != kCLAuthorizationStatusNotDetermined) {
            onDecided(manager.authorizationStatus)
        }
    }
}

private fun Int.isGranted(): Boolean =
    this == kCLAuthorizationStatusAuthorizedWhenInUse ||
        this == kCLAuthorizationStatusAuthorizedAlways
