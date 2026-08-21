package com.divafinance.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.adaptive.DivaGroupedSection
import com.divafinance.core.ui.adaptive.DivaListRow
import com.divafinance.core.ui.adaptive.DivaListScaffold
import com.divafinance.core.ui.adaptive.DivaRowDivider
import com.divafinance.core.ui.adaptive.divaContentPadding
import com.divafinance.core.ui.component.AvatarRing
import com.divafinance.core.ui.component.BudgetTrack
import com.divafinance.core.ui.component.CategoryTile
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.component.DivaOutlinedButton
import com.divafinance.core.ui.component.Meta
import com.divafinance.core.ui.component.SectionHeader
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
    contentPadding: PaddingValues = divaContentPadding(),
) {
    val state by viewModel.uiState.collectAsState()

    DivaListScaffold(title = "You", contentPadding = contentPadding) {
        item(key = "identity") { Identity(state) }
        item(key = "stats") { Stats(state) }
        item(key = "actions") {
            Actions(onLogExpense = onLogExpense, onOpenBudgets = onOpenBudgets)
        }

        item(key = "budgetHeader") {
            SectionHeader(
                title = "${state.monthLabel} budget",
                trailing = "Manage",
                onTrailingClick = onOpenBudgets,
            )
        }
        item(key = "budgetHero") { BudgetHero(state, onClick = onOpenBudgets) }

        // One section for the whole list, not one card per row: a run of rows separated
        // by rules is a group, and a card each reads as five unrelated panels.
        if (state.budgets.isNotEmpty()) {
            item(key = "budgets") {
                DivaGroupedSection(Modifier.padding(top = Space.md)) {
                    state.budgets.forEachIndexed { index, row ->
                        if (index > 0) DivaRowDivider(startInset = 62.dp)
                        BudgetRowItem(row, onClick = onOpenBudgets)
                    }
                }
            }
        }

        item(key = "everythingElse") {
            DivaGroupedSection(header = "Everything else") {
                NavRow(Icons.Outlined.CreditCard, "Cards", "Rules, limits and best-card", onOpenCards)
                DivaRowDivider(startInset = 64.dp)
                NavRow(Icons.Outlined.PieChart, "Graphs", "Charts and thresholds", onOpenGraphs)
                DivaRowDivider(startInset = 64.dp)
                NavRow(Icons.Outlined.Groups, "People & debts", "Who owes what", onOpenPeople)
                DivaRowDivider(startInset = 64.dp)
                NavRow(Icons.Outlined.Map, "Spending map", "Where the money goes", onOpenMap)
                DivaRowDivider(startInset = 64.dp)
                NavRow(Icons.Outlined.DocumentScanner, "Receipt scanner", "Scan and import", onOpenScanner)
                DivaRowDivider(startInset = 64.dp)
                NavRow(Icons.Outlined.Backup, "Backup & restore", "Export your archive", onOpenBackup)
                DivaRowDivider(startInset = 64.dp)
                NavRow(Icons.Outlined.AutoAwesome, "Automations", "Shortcuts and intents", onOpenAutomation)
                DivaRowDivider(startInset = 64.dp)
                NavRow(Icons.Outlined.Settings, "Settings", "Appearance, server, data", onOpenSettings)
            }
        }
    }
}

@Composable
private fun Identity(state: YouUiState) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Space.pad, vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.pad),
    ) {
        AvatarRing(initialsOf(state.displayName), diva.accent, size = 64.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(state.displayName, style = MaterialTheme.typography.headlineMedium)
            Text(
                state.location ?: "Local to this device",
                style = MaterialTheme.typography.bodySmall,
                color = diva.muted,
            )
        }
    }
}

@Composable
private fun Stats(state: YouUiState) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Space.pad, vertical = Space.sm),
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
    Box(Modifier.width(diva.hairline).height(46.dp).background(diva.separator))
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
                        style = MaterialTheme.typography.bodySmall,
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
                    color = MaterialTheme.colorScheme.primary,
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
    Column {
        DivaListRow(
            title = row.category.displayName,
            leading = { CategoryTile(row.category, size = 34.dp) },
            trailing = {
                Text(
                    when {
                        row.left == null -> formatCurrency(row.spent)
                        row.isOver -> "${formatCurrency(-(row.left ?: 0.0))} over"
                        else -> "${formatCurrency(row.left ?: 0.0)} left"
                    },
                    style = NumericStyle,
                    color = if (row.isOver) diva.negative else diva.muted,
                )
            },
            onClick = onClick,
        )
        BudgetTrack(
            fraction = row.fraction,
            color = row.category.color,
            isOver = row.isOver,
            height = 6.dp,
            modifier = Modifier.padding(start = Space.pad, end = Space.pad, bottom = Space.md),
        )
    }
}

@Composable
private fun NavRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    DivaListRow(
        title = title,
        subtitle = subtitle,
        leading = {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        showChevron = true,
        onClick = onClick,
    )
}
