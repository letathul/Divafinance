package com.divafinance.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.divafinance.core.model.enums.LocationCaptureMode
import com.divafinance.core.ui.adaptive.DivaGroupedSection
import com.divafinance.core.ui.adaptive.DivaListRow
import com.divafinance.core.ui.adaptive.DivaRowDivider
import com.divafinance.core.ui.adaptive.DivaSwitch
import com.divafinance.core.ui.adaptive.DivaScaffold
import com.divafinance.core.ui.component.DivaOutlinedButton
import com.divafinance.core.ui.component.SegmentedControl
import com.divafinance.core.ui.theme.AccentTheme
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.core.ui.theme.Space
import com.divafinance.core.ui.theme.ThemeMode
import com.divafinance.core.ui.theme.diva
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SettingsScreen(
    onNavigateToBackup: () -> Unit = {},
    onNavigateToScanner: () -> Unit = {},
    onNavigateToAutomation: () -> Unit = {},
    /**
     * Null leaves the screen without a back affordance. iOS has no system back, so a
     * pushed screen that omits this is a dead end there.
     */
    onBack: (() -> Unit)? = null,
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
    accent: AccentTheme = AccentTheme.BLUE,
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

    DivaScaffold(title = "Settings", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = Space.xl),
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

            DivaGroupedSection(header = "Data") {
                DivaListRow(
                    title = "Backup & Restore",
                    subtitle = "Export your archive",
                    showChevron = true,
                    onClick = onNavigateToBackup,
                )
            }

            DivaGroupedSection(header = "Tools") {
                DivaListRow(
                    title = "Receipt Scanner",
                    showChevron = true,
                    onClick = onNavigateToScanner,
                )
                DivaRowDivider()
                DivaListRow(
                    title = "Automations",
                    showChevron = true,
                    onClick = onNavigateToAutomation,
                )
            }

            ServerSection(
                isServerRunning = isServerRunning,
                onToggleServer = onToggleServer,
                serverPort = serverPort,
                serverUrl = serverUrl,
                serverError = serverError,
            )

            // Only rendered while the demo is active. Once removed it never comes back,
            // so there is deliberately no "enable" path here.
            if (isDemoActive) {
                DemoSection(
                    isRemovingDemo = isRemovingDemo,
                    onRemove = { confirmRemoveDemo = true },
                )
            }
        }
    }
}

@Composable
private fun ServerSection(
    isServerRunning: Boolean,
    onToggleServer: (Boolean) -> Unit,
    serverPort: Int,
    serverUrl: String?,
    serverError: String?,
) {
    DivaGroupedSection(
        header = "Local Server",
        footer = when {
            serverError != null -> null
            serverUrl != null -> "Sign in with your app PIN. Both devices must be on the " +
                "same Wi-Fi network."
            else -> null
        },
    ) {
        DivaListRow(
            title = "Browser Access",
            subtitle = when {
                // The URL only exists once the socket is bound, so its absence while
                // "running" means still starting.
                serverUrl != null -> "Open $serverUrl"
                isServerRunning -> "Starting on port $serverPort…"
                else -> "Start to access from browser"
            },
            trailing = {
                DivaSwitch(checked = isServerRunning, onCheckedChange = onToggleServer)
            },
        )
        if (serverError != null) {
            DivaRowDivider()
            Text(
                serverError,
                Modifier.padding(horizontal = Space.pad, vertical = Space.md),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun DemoSection(isRemovingDemo: Boolean, onRemove: () -> Unit) {
    DivaGroupedSection(
        header = "Demo Data",
        footer = "You're exploring with sample data. Remove it when you're ready to " +
            "start tracking for real — this is one-way.",
    ) {
        Box(Modifier.padding(Space.pad)) {
            DivaOutlinedButton(
                text = if (isRemovingDemo) "Removing..." else "Remove demo data",
                onClick = onRemove,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRemovingDemo,
            )
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
    DivaGroupedSection(
        header = "Location",
        footer = "Tagging expenses with a place puts them on your spending map and lets " +
            "the app suggest shops you've been to before.",
    ) {
        Box(Modifier.padding(Space.pad)) {
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
    DivaGroupedSection(header = "Appearance") {
        Column(Modifier.padding(Space.pad)) {
            val modes = ThemeMode.entries
            SegmentedControl(
                options = modes.map { it.label },
                selectedIndex = modes.indexOf(themeMode),
                onSelect = { onThemeModeChange(modes[it]) },
            )

            Spacer(Modifier.height(Space.pad))
            Text("Accent", style = MaterialTheme.typography.bodyMedium, color = diva.muted)
            Spacer(Modifier.height(Space.sm))
            AccentSwatches(accent, onAccentChange)
        }
    }
}

/**
 * Wraps rather than scrolls: every accent has to be reachable without a gesture, and
 * there are few enough of them to fit two rows on the narrowest phone.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentSwatches(accent: AccentTheme, onAccentChange: (AccentTheme) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.md),
        verticalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        AccentTheme.entries.forEach { option ->
            AccentSwatch(
                option = option,
                selected = option == accent,
                onClick = { onAccentChange(option) },
            )
        }
    }
}

/** The swatch paints the accent itself, so the choice is the thing being previewed. */
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
                .background(option.tintFor(diva.isDark))
                .border(
                    width = if (selected) 2.dp else diva.hairline,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else diva.separator,
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
