package com.divafinance.feature.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.divafinance.core.common.toFixed
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.enums.TransactionType
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.theme.DivaGreen
import com.divafinance.core.ui.theme.DivaRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transaction: Transaction,
    onBack: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DivaCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${if (transaction.type == TransactionType.DEBIT) "-" else "+"}${"$" + transaction.amount.toFixed(2)}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (transaction.type == TransactionType.DEBIT) DivaRed else DivaGreen,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = transaction.currency,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            DetailRow("Type", transaction.type.name)
            DetailRow("Category", transaction.category.displayName)
            DetailRow("Date", transaction.date.toString())

            transaction.merchantName?.let { DetailRow("Merchant", it) }
            transaction.note?.let { DetailRow("Note", it) }
            transaction.cardId?.let { DetailRow("Card ID", it) }

            transaction.location?.let { loc ->
                DivaCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Location", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        loc.name?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            text = "${loc.latitude.toFixed(4)}, ${loc.longitude.toFixed(4)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (transaction.isRecurring) {
                DetailRow("Recurring", "Yes")
            }

            transaction.receiptId?.let { DetailRow("Receipt", it) }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    DivaCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
