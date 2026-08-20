package com.divafinance.feature.activity

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.divafinance.core.common.toFixed
import com.divafinance.core.domain.usecase.activity.ActivityItem
import com.divafinance.core.domain.usecase.activity.ActivityKind
import com.divafinance.core.domain.usecase.people.PersonBalance
import com.divafinance.core.model.enums.TransactionType
import com.divafinance.core.ui.component.CategoryChip
import com.divafinance.core.ui.theme.diva
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.component.DivaTextField
import com.divafinance.core.ui.component.StatusBarSpacer
import org.koin.compose.viewmodel.koinViewModel

/**
 * Everything that has happened, in one stream: what you spent, what you lent or borrowed,
 * and what the app noticed. Replaces the separate Transactions and Feed tabs — the three
 * were the same reverse-chronological list read three ways.
 */
@Composable
fun ActivityScreen(
    onPersonClick: (String) -> Unit = {},
    viewModel: ActivityViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    ActivityContent(
        state = state,
        onToggleKind = viewModel::onToggleKind,
        onPeriodChange = viewModel::onPeriodChange,
        onQueryChange = viewModel::onQueryChange,
        onClearFilters = viewModel::onClearFilters,
        onPersonClick = onPersonClick,
    )
}

/** Stateless body, so tests and previews can drive it without a ViewModel. */
@Composable
internal fun ActivityContent(
    state: ActivityUiState,
    onToggleKind: (ActivityKind) -> Unit = {},
    onPeriodChange: (ActivityPeriod) -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onClearFilters: () -> Unit = {},
    onPersonClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { StatusBarSpacer() }
        item { Text("Activity", style = MaterialTheme.typography.headlineSmall) }

        if (state.openBalances.isNotEmpty()) {
            item { BalancesSummary(state, onPersonClick) }
        }

        item {
            DivaTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = "Search activity",
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            )
        }

        item { FilterRow(state, onToggleKind, onPeriodChange, onClearFilters) }

        if (state.items.isEmpty()) {
            item { EmptyState(isFiltered = state.isFiltered) }
        }

        items(state.items, key = { it.id }) { item ->
            when (item) {
                is ActivityItem.Spend -> SpendRow(item)
                is ActivityItem.Debt -> DebtRow(item, onPersonClick)
                is ActivityItem.Insight -> InsightRow(item)
            }
        }

        // Clears the quick-add FAB that MainScreen overlays.
        item { Spacer(Modifier.height(72.dp)) }
    }
}

/** Who owes what, at a glance. Hidden entirely when everything is settled. */
@Composable
private fun BalancesSummary(state: ActivityUiState, onPersonClick: (String) -> Unit) {
    DivaCard {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                BalanceTotal("Owed to you", state.totalOwedToMe, diva.positive)
                BalanceTotal("You owe", state.totalIOwe, diva.negative)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                state.openBalances.forEach { balance ->
                    CategoryChip(
                        label = "${balance.person.name} ${balance.signedLabel()}",
                        onClick = { onPersonClick(balance.person.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BalanceTotal(label: String, amount: Double, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(
            text = amount.toFixed(2),
            style = MaterialTheme.typography.titleMedium,
            color = color,
        )
    }
}

@Composable
private fun FilterRow(
    state: ActivityUiState,
    onToggleKind: (ActivityKind) -> Unit,
    onPeriodChange: (ActivityPeriod) -> Unit,
    onClearFilters: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            ActivityKind.entries.forEach { kind ->
                CategoryChip(
                    label = kind.displayName,
                    selected = kind in state.selectedKinds,
                    onClick = { onToggleKind(kind) },
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            ActivityPeriod.entries.forEach { period ->
                CategoryChip(
                    label = period.displayName,
                    selected = state.period == period,
                    onClick = { onPeriodChange(period) },
                )
            }
            if (state.isFiltered) {
                CategoryChip(label = "Clear", onClick = onClearFilters)
            }
        }
    }
}

@Composable
private fun EmptyState(isFiltered: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (isFiltered) "Nothing matches those filters" else "Nothing here yet",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (isFiltered) {
                "Try widening the date range or turning a type back on."
            } else {
                "Add a transaction to start tracking spending and who owes what."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Spending: a plain card, amount coloured by direction. */
@Composable
private fun SpendRow(item: ActivityItem.Spend) {
    val transaction = item.transaction
    val isDebit = transaction.type == TransactionType.DEBIT

    DivaCard {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KindDot(MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = transaction.merchantName ?: transaction.category.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "${transaction.category.displayName} • ${transaction.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // A split shows both figures: what left the account, and what was yours.
                if (item.isShared) {
                    Text(
                        text = "Your share ${item.ownShare.toFixed(2)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Text(
                text = (if (isDebit) "-" else "+") + transaction.amount.toFixed(2),
                style = MaterialTheme.typography.titleSmall,
                color = if (isDebit) diva.negative else diva.positive,
            )
        }
    }
}

/** Debts: tinted container so they read as a different kind of event entirely. */
@Composable
private fun DebtRow(item: ActivityItem.Debt, onPersonClick: (String) -> Unit) {
    val theyOweMe = item.signedAmount > 0.0

    Surface(
        onClick = { item.person?.let { onPersonClick(it.id) } },
        enabled = item.person != null,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KindDot(if (theyOweMe) diva.positive else diva.negative)
            Spacer(Modifier.size(12.dp))

            Column(Modifier.weight(1f)) {
                Text(item.personName, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${item.entry.kind.displayName} • ${item.entry.date}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = item.entry.amount.toFixed(2),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (theyOweMe) diva.positive else diva.negative,
                )
                Text(
                    text = if (theyOweMe) "owes you" else "you owe",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

/** Insights: the tertiary bubble the feed already used, so they stay recognisable. */
@Composable
private fun InsightRow(item: ActivityItem.Insight) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(item.post.title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(item.post.body, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Small colour flash so the three kinds are separable at a glance while scrolling. */
@Composable
private fun KindDot(color: androidx.compose.ui.graphics.Color) {
    Surface(shape = CircleShape, color = color, modifier = Modifier.size(8.dp)) {}
}

private fun PersonBalance.signedLabel(): String =
    if (theyOweMe) "+${balance.toFixed(2)}" else "-${(-balance).toFixed(2)}"
