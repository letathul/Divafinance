package com.divafinance.feature.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.divafinance.core.common.toFixed
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKAnnotationProtocol
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKMapView
import platform.MapKit.MKPointAnnotation

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformMapView(
    locations: List<LocationSpending>,
    onLocationClick: (LocationSpending) -> Unit,
    modifier: Modifier,
) {
    val annotations = remember(locations) {
        locations.map { spending ->
            MKPointAnnotation(
                CLLocationCoordinate2DMake(
                    spending.location.latitude,
                    spending.location.longitude,
                ),
                spending.name,
                "${"$" + spending.totalAmount.toFixed(2)} (${spending.transactions.size} transactions)",
            )
        }
    }

    val center = remember(locations) {
        if (locations.isEmpty()) {
            CLLocationCoordinate2DMake(0.0, 0.0)
        } else {
            CLLocationCoordinate2DMake(
                locations.map { it.location.latitude }.average(),
                locations.map { it.location.longitude }.average(),
            )
        }
    }

    UIKitView(
        factory = {
            MKMapView().apply {
                setRegion(
                    MKCoordinateRegionMakeWithDistance(center, 10_000.0, 10_000.0),
                    animated = false,
                )
                annotations.forEach { addAnnotation(it) }
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { mapView ->
            mapView.removeAnnotations(mapView.annotations)
            annotations.forEach { mapView.addAnnotation(it) }
            if (locations.isNotEmpty()) {
                mapView.setRegion(
                    MKCoordinateRegionMakeWithDistance(center, 10_000.0, 10_000.0),
                    animated = true,
                )
            }
        },
    )
}

actual fun isPlatformMapAvailable(): Boolean = true
