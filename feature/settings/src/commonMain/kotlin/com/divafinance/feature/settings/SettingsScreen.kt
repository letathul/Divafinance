package com.divafinance.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.component.DivaOutlinedButton
import com.divafinance.core.model.enums.LocationCaptureMode
import com.divafinance.core.ui.component.SegmentedControl
import com.divafinance.core.ui.theme.AccentTheme
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.core.ui.theme.ThemeMode
import com.divafinance.core.ui.theme.diva
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
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accent: AccentTheme = AccentTheme.SUNSET,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onAccentChange: (AccentTheme) -> Unit = {},
    /** Null until the user has been asked, which leaves both options unselected. */
    locationCaptureMode: LocationCaptureMode? = null,
    onLocationCaptureModeChange: (LocationCaptureMode) -> Unit = {},
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppearanceSection(
                themeMode = themeMode,
                accent = accent,
                onThemeModeChange = onThemeModeChange,
                onAccentChange = onAccentChange,
            )

            LocationSection(
                mode = locationCaptureMode,
                onModeChange = onLocationCaptureModeChange,
            )

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

/**
 * When the add-expense sheet reads position. Neither option is shown as chosen until the
 * user has actually been asked — the sheet treats "never asked" as its own state, and
 * pre-selecting one here would misreport a choice nobody made.
 *
 * There is no off switch because "only when I tap" already is one: nothing is read until
 * the place line is tapped, and the OS permission is a separate gate on top.
 */
@Composable
private fun LocationSection(
    mode: LocationCaptureMode?,
    onModeChange: (LocationCaptureMode) -> Unit,
) {
    val options = LocationCaptureMode.entries
    DivaCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Location", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Tagging expenses with a place puts them on your spending map and lets " +
                    "the app suggest shops you've been to before.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            SegmentedControl(
                options = options.map { it.label },
                selectedIndex = options.indexOf(mode),
                onSelect = { onModeChange(options[it]) },
            )
        }
    }
}

/** Screen-facing wording for the stored enum, which stays free of UI copy. */
private val LocationCaptureMode.label: String
    get() = when (this) {
        LocationCaptureMode.ALWAYS -> "Every expense"
        LocationCaptureMode.ON_TAP -> "Only when I tap"
    }

@Composable
private fun AppearanceSection(
    themeMode: ThemeMode,
    accent: AccentTheme,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentChange: (AccentTheme) -> Unit,
) {
    DivaCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            val modes = ThemeMode.entries
            SegmentedControl(
                options = modes.map { it.label },
                selectedIndex = modes.indexOf(themeMode),
                onSelect = { onThemeModeChange(modes[it]) },
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Accent",
                style = MaterialTheme.typography.bodyMedium,
                color = diva.muted,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AccentTheme.entries.forEach { option ->
                    AccentSwatch(
                        option = option,
                        selected = option == accent,
                        onClick = { onAccentChange(option) },
                    )
                }
            }
        }
    }
}

/** The swatch paints the actual sweep, so the choice is the thing being previewed. */
@Composable
private fun AccentSwatch(
    option: AccentTheme,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(option.colors))
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else diva.fgHair,
                    shape = CircleShape,
                )
        )
        Text(
            option.label,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface else diva.muted,
        )
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    DivaTheme {
        SettingsScreen()
    }
}
