package com.divafinance.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.component.AvatarRing
import com.divafinance.core.ui.component.BudgetTrack
import com.divafinance.core.ui.component.CategoryTile
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.component.DivaOutlinedButton
import com.divafinance.core.ui.component.Hairline
import com.divafinance.core.ui.component.Meta
import com.divafinance.core.ui.component.SectionHeader
import com.divafinance.core.ui.component.StatusBarSpacer
import com.divafinance.core.ui.component.color
import com.divafinance.core.ui.component.initialsOf
import com.divafinance.core.ui.theme.NumericStyle
import com.divafinance.core.ui.theme.Space
import com.divafinance.core.ui.theme.diva
import com.divafinance.core.ui.util.formatCurrency

/**
 * Identity, the month's plan, and the way through to everything that is not the feed.
 *
 * The navigation rows at the bottom are the whole reason this tab exists: with three tabs
 * there is nowhere else for cards, graphs, people, the map or settings to live.
 */
@Composable
fun YouScreen(
    viewModel: YouViewModel,
    onLogExpense: () -> Unit = {},
    onOpenBudgets: () -> Unit = {},
    onOpenCards: () -> Unit = {},
    onOpenGraphs: () -> Unit = {},
    onOpenPeople: () -> Unit = {},
    onOpenMap: () -> Unit = {},
    onOpenScanner: () -> Unit = {},
    onOpenBackup: () -> Unit = {},
    onOpenAutomation: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(bottom = 120.dp),
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = contentPadding,
    ) {
        item { StatusBarSpacer() }
        item { Identity(state) }
        item { Stats(state) }
        item { Actions(onLogExpense = onLogExpense, onOpenBudgets = onOpenBudgets) }

        item {
            SectionHeader(
                title = "${state.monthLabel} budget",
                trailing = "Manage",
                onTrailingClick = onOpenBudgets,
            )
        }
        item { BudgetHero(state, onClick = onOpenBudgets) }

        items(state.budgets, key = { it.category.name }) { row ->
            BudgetRowItem(row, onClick = onOpenBudgets)
        }

        item { SectionHeader("Everything else") }
        item {
            DivaCard(Modifier.padding(horizontal = Space.pad)) {
                NavRow(Icons.Outlined.CreditCard, "Cards", "Rules, limits and best-card", onOpenCards)
                Hairline()
                NavRow(Icons.Outlined.PieChart, "Graphs", "Charts and thresholds", onOpenGraphs)
                Hairline()
                NavRow(Icons.Outlined.Groups, "People & debts", "Who owes what", onOpenPeople)
                Hairline()
                NavRow(Icons.Outlined.Map, "Spending map", "Where the money goes", onOpenMap)
                Hairline()
                NavRow(Icons.Outlined.DocumentScanner, "Receipt scanner", "Scan and import", onOpenScanner)
                Hairline()
                NavRow(Icons.Outlined.Backup, "Backup & restore", "Export your archive", onOpenBackup)
                Hairline()
                NavRow(Icons.Outlined.AutoAwesome, "Automations", "Shortcuts and intents", onOpenAutomation)
                Hairline()
                NavRow(Icons.Outlined.Settings, "Settings", "Appearance, server, data", onOpenSettings)
            }
        }
    }
}

@Composable
private fun Identity(state: YouUiState) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Space.pad, vertical = Space.lg),
        verticalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.pad),
        ) {
            AvatarRing(initialsOf(state.displayName), diva.accent)
            Column {
                Text(
                    state.displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    state.location ?: "Local to this device",
                    style = MaterialTheme.typography.labelSmall,
                    color = diva.muted,
                )
            }
        }
    }
}

@Composable
private fun Stats(state: YouUiState) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Space.pad),
        horizontalArrangement = Arrangement.spacedBy(Space.pad),
    ) {
        Stat(state.expensesThisYear.toString(), "expenses this year", Modifier.weight(1f))
        VerticalHairline()
        Stat(state.placesThisMonth.toString(), "places this month", Modifier.weight(1f))
        VerticalHairline()
        Stat(state.daysOnPlanStreak.toString(), "days on plan in a row", Modifier.weight(1f))
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.bodySmall, color = diva.muted)
    }
}

@Composable
private fun VerticalHairline() {
    Box(Modifier.width(1.dp).height(46.dp).background(diva.fgHair))
}

@Composable
private fun Actions(onLogExpense: () -> Unit, onOpenBudgets: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(Space.pad),
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        DivaButton("Log an expense", onLogExpense, Modifier.weight(1f))
        DivaOutlinedButton("Budgets", onOpenBudgets, Modifier.weight(1f))
    }
}

@Composable
private fun BudgetHero(state: YouUiState, onClick: () -> Unit) {
    DivaCard(Modifier.padding(horizontal = Space.pad), onClick = onClick) {
        Column(Modifier.padding(Space.pad), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val budget = state.monthlyBudget
            if (budget == null) {
                Meta("No monthly plan yet")
                Text(
                    formatCurrency(state.spentThisMonth),
                    style = MaterialTheme.typography.displaySmall,
                )
                Text(
                    "spent this month · set a monthly plan to see what's left",
                    style = MaterialTheme.typography.bodySmall,
                    color = diva.muted,
                )
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Meta("Left to spend")
                    Text(
                        "${formatCurrency(state.spentThisMonth)} of ${formatCurrency(budget)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = diva.muted,
                    )
                }
                Text(
                    formatCurrency(state.leftToSpend ?: 0.0),
                    style = MaterialTheme.typography.displaySmall,
                    color = if ((state.leftToSpend ?: 0.0) < 0) diva.negative
                    else MaterialTheme.colorScheme.onSurface,
                )
                BudgetTrack(
                    fraction = state.budgetFraction,
                    color = MaterialTheme.colorScheme.onSurface,
                    isOver = state.budgetFraction > 1f,
                )
                Text(
                    buildString {
                        append(state.daysLeftInMonth).append(" days to go")
                        state.dailyAllowance?.let {
                            append(" · ").append(formatCurrency(it)).append(" a day keeps you on plan")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = diva.muted,
                )
            }
        }
    }
}

@Composable
private fun BudgetRowItem(row: BudgetRow, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Space.pad, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        CategoryTile(row.category, size = 38.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(row.category.displayName, style = MaterialTheme.typography.titleSmall)
                Text(
                    when {
                        row.left == null -> formatCurrency(row.spent)
                        row.isOver -> "${formatCurrency(-(row.left ?: 0.0))} over"
                        else -> "${formatCurrency(row.left ?: 0.0)} left"
                    },
                    style = NumericStyle,
                    color = if (row.isOver) diva.negative else diva.muted,
                )
            }
            BudgetTrack(
                fraction = row.fraction,
                color = row.category.color,
                isOver = row.isOver,
                height = 6.dp,
            )
        }
    }
}

@Composable
private fun NavRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Space.pad, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = diva.muted, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = diva.muted)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = diva.muted,
            modifier = Modifier.size(20.dp),
        )
    }
}
