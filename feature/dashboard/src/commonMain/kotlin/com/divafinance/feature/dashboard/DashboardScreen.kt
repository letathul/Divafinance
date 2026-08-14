package com.divafinance.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.enums.TransactionType
import com.divafinance.core.ui.component.AmountDisplay
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.component.DivaOutlinedButton
import com.divafinance.core.ui.theme.DivaGreen
import com.divafinance.core.ui.theme.DivaRed
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DashboardScreen(
    onNavigateToCards: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToBestCard: () -> Unit = {},
    onNavigateToGraphs: () -> Unit = {},
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val cards by viewModel.cards.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val totalSpending by viewModel.totalSpending.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        item { OverviewCard(totalSpending = totalSpending, totalIncome = totalIncome) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DivaCard(modifier = Modifier.weight(1f), onClick = onNavigateToCards) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${cards.size}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text("Cards", style = MaterialTheme.typography.bodySmall)
                    }
                }
                DivaCard(modifier = Modifier.weight(1f), onClick = onNavigateToTransactions) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${recentTransactions.size}+",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text("Transactions", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            QuickActionsSection(
                onNavigateToCards = onNavigateToCards,
                onNavigateToBestCard = onNavigateToBestCard,
                onNavigateToGraphs = onNavigateToGraphs,
            )
        }

        item {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (recentTransactions.isEmpty()) {
            item {
                Text(
                    text = "No transactions yet. Add your first transaction to get started.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }

        items(recentTransactions, key = { it.id }) { transaction ->
            RecentTransactionItem(transaction = transaction)
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun OverviewCard(totalSpending: Double, totalIncome: Double) {
    DivaCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Overview", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        "Income",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AmountDisplay(amount = totalIncome, showSign = true)
                }
                Column {
                    Text(
                        "Spending",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AmountDisplay(amount = -totalSpending)
                }
            }
            Spacer(Modifier.height(8.dp))
            val net = totalIncome - totalSpending
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    "Net: ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AmountDisplay(
                    amount = net,
                    showSign = true,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    onNavigateToCards: () -> Unit,
    onNavigateToBestCard: () -> Unit,
    onNavigateToGraphs: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Quick Actions", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DivaOutlinedButton(
                text = "My Cards",
                onClick = onNavigateToCards,
                modifier = Modifier.weight(1f),
            )
            DivaOutlinedButton(
                text = "Best Card",
                onClick = onNavigateToBestCard,
                modifier = Modifier.weight(1f),
            )
            DivaOutlinedButton(
                text = "Graphs",
                onClick = onNavigateToGraphs,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RecentTransactionItem(transaction: Transaction) {
    DivaCard {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchantName ?: transaction.category.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "${transaction.category.displayName} • ${transaction.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${if (transaction.type == TransactionType.DEBIT) "-" else "+"}${"$%.2f".format(transaction.amount)}",
                style = MaterialTheme.typography.titleSmall,
                color = if (transaction.type == TransactionType.DEBIT) DivaRed else DivaGreen,
            )
        }
    }
}
