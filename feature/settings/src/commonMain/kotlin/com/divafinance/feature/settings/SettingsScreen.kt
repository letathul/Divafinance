package com.divafinance.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.component.DivaOutlinedButton
import com.divafinance.core.ui.theme.DivaTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToBackup: () -> Unit = {},
    onNavigateToScanner: () -> Unit = {},
    onNavigateToAutomation: () -> Unit = {},
    isServerRunning: Boolean = false,
    onToggleServer: (Boolean) -> Unit = {},
    serverPort: Int = 8080,
    /** Set once the socket is bound; this is the address to open on another device. */
    serverUrl: String? = null,
    serverError: String? = null,
    isDemoActive: Boolean = false,
    isRemovingDemo: Boolean = false,
    onRemoveDemo: () -> Unit = {},
) {
    var confirmRemoveDemo by remember { mutableStateOf(false) }

    if (confirmRemoveDemo) {
        AlertDialog(
            onDismissRequest = { confirmRemoveDemo = false },
            title = { Text("Remove demo data?") },
            text = {
                Text(
                    "This deletes the demo accounts, cards, transactions, receipts and " +
                        "insights. Anything you recorded against a demo account goes with " +
                        "it. This can't be undone, and the demo can't be turned back on.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRemoveDemo = false
                        onRemoveDemo()
                    },
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemoveDemo = false }) { Text("Keep it") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Only rendered while the demo is active. Once removed it never comes back,
            // so there is deliberately no "enable" path here.
            if (isDemoActive) {
                DivaCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Demo Data", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "You're exploring with sample data. Remove it when you're ready " +
                                "to start tracking for real — this is one-way.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DivaOutlinedButton(
                            text = if (isRemovingDemo) "Removing..." else "Remove demo data",
                            onClick = { confirmRemoveDemo = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isRemovingDemo,
                        )
                    }
                }
            }

            DivaCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Data", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    DivaOutlinedButton(
                        text = "Backup & Restore",
                        onClick = onNavigateToBackup,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            DivaCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tools", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    DivaOutlinedButton(
                        text = "Receipt Scanner",
                        onClick = onNavigateToScanner,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DivaOutlinedButton(
                        text = "Automations",
                        onClick = onNavigateToAutomation,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            DivaCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Local Server", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Browser Access",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                when {
                                    // The URL only exists once the socket is bound, so
                                    // its absence while "running" means still starting.
                                    serverUrl != null -> "Open $serverUrl"
                                    isServerRunning -> "Starting on port $serverPort…"
                                    else -> "Start to access from browser"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = isServerRunning,
                            onCheckedChange = onToggleServer,
                        )
                    }
                    if (serverError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            serverError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (serverUrl != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Sign in with your app PIN. Both devices must be on the " +
                                "same Wi-Fi network.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    DivaTheme {
        SettingsScreen()
    }
}
