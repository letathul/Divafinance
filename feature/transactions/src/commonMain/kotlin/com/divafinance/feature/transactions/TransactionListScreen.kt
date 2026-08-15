package com.divafinance.feature.transactions

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import com.divafinance.core.ui.component.CategoryChip
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.component.DivaTextField
import com.divafinance.core.ui.theme.DivaGreen
import com.divafinance.core.ui.theme.DivaRed
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TransactionListScreen(
    onAddTransaction: () -> Unit = {},
    viewModel: TransactionsViewModel = koinViewModel(),
) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val filterState by viewModel.filterState.collectAsState()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text("Transactions", style = MaterialTheme.typography.headlineSmall)
            }

            item {
                DivaTextField(
                    value = filterState.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    label = "Search transactions",
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                )
            }

            item {
                FilterSection(
                    filterState = filterState,
                    onCategoryFilter = viewModel::updateCategoryFilter,
                    onTypeFilter = viewModel::updateTypeFilter,
                    onSortOrder = viewModel::updateSortOrder,
                )
            }

            if (transactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "No Transactions",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Add your first transaction to start tracking spending.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            items(transactions, key = { it.id }) { transaction ->
                TransactionItem(transaction = transaction)
            }

            item { Spacer(Modifier.height(72.dp)) }
        }

        FloatingActionButton(
            onClick = {
                viewModel.resetForm()
                onAddTransaction()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Transaction")
        }
    }
}

@Composable
private fun FilterSection(
    filterState: TransactionFilterState,
    onCategoryFilter: (SpendingCategory?) -> Unit,
    onTypeFilter: (TransactionType?) -> Unit,
    onSortOrder: (TransactionSortOrder) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            CategoryChip(
                label = "All",
                selected = filterState.categoryFilter == null,
                onClick = { onCategoryFilter(null) },
            )
            SpendingCategory.entries.forEach { cat ->
                CategoryChip(
                    label = cat.displayName,
                    selected = filterState.categoryFilter == cat,
                    onClick = { onCategoryFilter(cat) },
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryChip(
                label = "All Types",
                selected = filterState.typeFilter == null,
                onClick = { onTypeFilter(null) },
            )
            CategoryChip(
                label = "Expenses",
                selected = filterState.typeFilter == TransactionType.DEBIT,
                onClick = { onTypeFilter(TransactionType.DEBIT) },
            )
            CategoryChip(
                label = "Income",
                selected = filterState.typeFilter == TransactionType.CREDIT,
                onClick = { onTypeFilter(TransactionType.CREDIT) },
            )

            Spacer(Modifier.weight(1f))

            SortDropdown(
                currentOrder = filterState.sortOrder,
                onOrderSelected = onSortOrder,
            )
        }
    }
}

@Composable
private fun SortDropdown(
    currentOrder: TransactionSortOrder,
    onOrderSelected: (TransactionSortOrder) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(currentOrder.label, style = MaterialTheme.typography.labelMedium)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TransactionSortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(order.label) },
                    onClick = {
                        onOrderSelected(order)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun TransactionItem(transaction: Transaction) {
    DivaCard {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchantName ?: transaction.category.displayName,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = buildString {
                        append(transaction.category.displayName)
                        append(" • ")
                        append(transaction.date.toString())
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                transaction.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = "${if (transaction.type == TransactionType.DEBIT) "-" else "+"}${"$%.2f".format(transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                color = if (transaction.type == TransactionType.DEBIT) DivaRed else DivaGreen,
            )
        }
    }
}
