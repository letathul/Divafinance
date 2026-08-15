package com.divafinance.feature.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.component.LoadingIndicator
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendingMapScreen(
    onBack: () -> Unit = {},
    viewModel: MapViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spending Map") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isPlatformMapAvailable()) {
                        IconButton(onClick = { viewModel.toggleMapView() }) {
                            Icon(
                                if (uiState.showMapView) Icons.Default.List else Icons.Default.Place,
                                contentDescription = if (uiState.showMapView) "List view" else "Map view",
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            }
            uiState.error != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Something went wrong",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = uiState.error!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = { viewModel.loadSpendingByLocation() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            uiState.showMapView -> {
                PlatformMapView(
                    locations = uiState.locationGroups,
                    onLocationClick = { viewModel.selectGroup(it) },
                    modifier = Modifier.padding(padding),
                )
            }
            else -> {
                MapFallbackScreen(
                    locationGroups = uiState.locationGroups,
                    onGroupClick = { viewModel.selectGroup(it) },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }

    if (uiState.selectedGroup != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.selectGroup(null) },
            sheetState = sheetState,
        ) {
            LocationDetailSheet(
                group = uiState.selectedGroup!!,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        }
    }

    if (uiState.showTagDialog) {
        LocationTagDialog(
            onDismiss = { viewModel.dismissTagDialog() },
            onConfirm = { name, lat, lng -> viewModel.tagTransaction(name, lat, lng) },
        )
    }
}
