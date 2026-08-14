package com.divafinance.feature.map

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.divafinance.core.model.enums.TransactionType
import com.divafinance.core.ui.component.AmountDisplay
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.theme.DivaGreen
import com.divafinance.core.ui.theme.DivaRed
import com.divafinance.core.ui.theme.DivaTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun MapFallbackScreen(
    locationGroups: List<LocationSpending>,
    onGroupClick: (LocationSpending) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (locationGroups.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "No location-tagged transactions",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tag your transactions with locations to see spending grouped by place.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = "${locationGroups.size} locations",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        items(locationGroups, key = { it.name }) { group ->
            LocationGroupCard(group = group, onClick = { onGroupClick(group) })
        }
    }
}

@Composable
private fun LocationGroupCard(
    group: LocationSpending,
    onClick: () -> Unit = {},
) {
    DivaCard(onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "${group.transactions.size} transaction${if (group.transactions.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AmountDisplay(
                    amount = -group.totalAmount,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "%.4f, %.4f".format(group.location.latitude, group.location.longitude),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun LocationDetailSheet(
    group: LocationSpending,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = group.name,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(4.dp))
        AmountDisplay(
            amount = -group.totalAmount,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Transactions",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        group.transactions.forEach { transaction ->
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
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Preview
@Composable
private fun MapFallbackScreenPreview() {
    DivaTheme {
        MapFallbackScreen(locationGroups = emptyList())
    }
}
