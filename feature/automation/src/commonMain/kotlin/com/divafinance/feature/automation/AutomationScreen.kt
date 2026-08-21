package com.divafinance.feature.automation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.adaptive.DivaScaffold
import com.divafinance.core.ui.adaptive.DivaSwitch
import com.divafinance.core.ui.component.DivaCard
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AutomationScreen(
    onBack: () -> Unit = {},
    viewModel: AutomationViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    DivaScaffold(
        title = "Automations",
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            item {
                Text(
                    "Configure shortcuts and automations for quick access to Diva Finance features.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            }

            val crossPlatform = uiState.actions.filter { it.platform == AutomationPlatform.CROSS_PLATFORM }
            if (crossPlatform.isNotEmpty()) {
                item {
                    Text("General", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
                items(crossPlatform, key = { it.id }) { action ->
                    AutomationActionCard(action = action, onToggle = { viewModel.toggleAction(action.id) })
                }
            }

            val androidActions = uiState.actions.filter { it.platform == AutomationPlatform.ANDROID }
            if (androidActions.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Android", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
                items(androidActions, key = { it.id }) { action ->
                    AutomationActionCard(action = action, onToggle = { viewModel.toggleAction(action.id) })
                }
            }

            val iosActions = uiState.actions.filter { it.platform == AutomationPlatform.IOS }
            if (iosActions.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("iOS", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
                items(iosActions, key = { it.id }) { action ->
                    AutomationActionCard(action = action, onToggle = { viewModel.toggleAction(action.id) })
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun AutomationActionCard(
    action: AutomationAction,
    onToggle: () -> Unit,
) {
    DivaCard {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(action.title, style = MaterialTheme.typography.bodyMedium)
                Text(
                    action.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DivaSwitch(checked = action.enabled, onCheckedChange = { onToggle() })
        }
    }
}
