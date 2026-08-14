package com.divafinance.feature.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
actual fun PlatformMapView(
    locations: List<LocationSpending>,
    onLocationClick: (LocationSpending) -> Unit,
    modifier: Modifier,
) {
    val center = remember(locations) {
        if (locations.isEmpty()) {
            LatLng(0.0, 0.0)
        } else {
            val avgLat = locations.map { it.location.latitude }.average()
            val avgLng = locations.map { it.location.longitude }.average()
            LatLng(avgLat, avgLng)
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, 12f)
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
    ) {
        locations.forEach { spending ->
            val position = LatLng(spending.location.latitude, spending.location.longitude)
            Marker(
                state = MarkerState(position = position),
                title = spending.name,
                snippet = "${"$%.2f".format(spending.totalAmount)} (${spending.transactions.size} transactions)",
                onClick = {
                    onLocationClick(spending)
                    false
                },
            )
        }
    }
}

actual fun isPlatformMapAvailable(): Boolean = true
