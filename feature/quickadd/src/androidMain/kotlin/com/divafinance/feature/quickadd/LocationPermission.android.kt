package com.divafinance.feature.quickadd

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberLocationPermissionRequester(): LocationPermissionRequester {
    // The callback is held in a holder because the launcher's own callback is fixed at
    // creation, while each request needs to report back to a different caller.
    val pending = remember { arrayOfNulls<(Boolean) -> Unit>(1) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        // Coarse alone is enough to identify a shop, so either permission counts.
        val allowed = granted.values.any { it }
        pending[0]?.invoke(allowed)
        pending[0] = null
    }

    return remember(launcher) {
        LocationPermissionRequester { onResult ->
            pending[0] = onResult
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
            )
        }
    }
}
