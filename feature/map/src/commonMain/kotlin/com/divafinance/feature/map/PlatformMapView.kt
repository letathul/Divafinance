package com.divafinance.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformMapView(
    locations: List<LocationSpending>,
    onLocationClick: (LocationSpending) -> Unit,
    modifier: Modifier = Modifier,
)

expect fun isPlatformMapAvailable(): Boolean
