package com.divafinance.feature.activity

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.adaptive.DivaScaffold
import com.divafinance.core.common.toFixed
import com.divafinance.core.model.LedgerEntry
import com.divafinance.core.ui.component.CategoryChip
import com.divafinance.core.ui.theme.diva
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.component.DivaTextField

/** One person's balance, everything behind it, and a way to settle up. */
@Composable
fun PersonDetailScreen(
    onBack: () -> Unit,
    viewModel: PersonDetailViewModel,
) {
    val state by viewModel.uiState.collectAsState()

    DivaScaffold(
        title = state.detail?.person?.name ?: "Person",
        onBack = onBack,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PersonDetailContent(
                state = state,
                onSettleAmountChange = viewModel::onSettleAmountChange,
                onSettleAll = viewModel::onSettleAll,
                onSettle = viewModel::onSettle,
            )
        }
    }
}

/** Stateless body, so tests can drive it without a ViewModel. */
@Composable
internal fun PersonDetailContent(
    state: PersonDetailUiState,
    onSettleAmountChange: (String) -> Unit = {},
    onSettleAll: () -> Unit = {},
    onSettle: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val detail = state.detail

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (detail == null) {
            item {
                Text(
                    text = "This person is no longer available.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    textAlign = TextAlign.Center,
                )
            }
            return@LazyColumn
        }

        item { BalanceHeader(detail.balance.balance, detail.balance.isSettled) }

        if (!detail.balance.isSettled) {
            item {
                SettleUpCard(
                    state = state,
                    onSettleAmountChange = onSettleAmountChange,
                    onSettleAll = onSettleAll,
                    onSettle = onSettle,
                )
            }
        }

        item {
            Text("History", style = MaterialTheme.typography.labelLarge)
        }

        if (detail.entries.isEmpty()) {
            item {
                Text(
                    text = "Nothing recorded yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(detail.entries, key = { it.id }) { entry -> EntryRow(entry) }

        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun BalanceHeader(balance: Double, isSettled: Boolean) {
    val theyOweMe = balance > 0.0

    DivaCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = when {
                    isSettled -> "All settled up"
                    theyOweMe -> "Owes you"
                    else -> "You owe"
                },
                style = MaterialTheme.typography.labelMedium,
            )
            if (!isSettled) {
                Text(
                    text = kotlin.math.abs(balance).toFixed(2),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (theyOweMe) diva.positive else diva.negative,
                )
            }
        }
    }
}

@Composable
private fun SettleUpCard(
    state: PersonDetailUiState,
    onSettleAmountChange: (String) -> Unit,
    onSettleAll: () -> Unit,
    onSettle: () -> Unit,
) {
    DivaCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Settle up", style = MaterialTheme.typography.labelLarge)

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DivaTextField(
                    value = state.settleAmount,
                    onValueChange = onSettleAmountChange,
                    label = "Amount",
                    modifier = Modifier.weight(1f),
                )
                CategoryChip(label = "All", onClick = onSettleAll)
            }

            state.error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            DivaButton(
                text = if (state.isSettling) "Recording..." else "Record repayment",
                onClick = onSettle,
                enabled = state.canSettle,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EntryRow(entry: LedgerEntry) {
    val increasesWhatTheyOwe = entry.signedAmount > 0.0

    DivaCard {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(entry.kind.displayName, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = listOfNotNull(entry.note, entry.date.toString()).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = (if (increasesWhatTheyOwe) "+" else "-") + entry.amount.toFixed(2),
                style = MaterialTheme.typography.titleSmall,
                color = if (increasesWhatTheyOwe) diva.positive else diva.negative,
            )
        }
    }
}
