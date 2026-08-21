package com.divafinance.feature.transactions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.adaptive.DivaScaffold
import com.divafinance.core.common.toFixed
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import com.divafinance.core.ui.component.CategoryChip
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.component.DivaTextField

@Composable
fun AddTransactionScreen(
    onBack: () -> Unit = {},
    viewModel: TransactionsViewModel,
) {
    val formState by viewModel.formState.collectAsState()
    val cards by viewModel.cards.collectAsState()

    DivaScaffold(
        title = "Add Transaction",
        onBack = onBack,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DivaTextField(
                    value = formState.amount,
                    onValueChange = viewModel::updateAmount,
                    label = "Amount",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )

                Text("Type", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryChip(
                        label = "Expense",
                        selected = formState.type == TransactionType.DEBIT,
                        onClick = { viewModel.updateType(TransactionType.DEBIT) },
                    )
                    CategoryChip(
                        label = "Income",
                        selected = formState.type == TransactionType.CREDIT,
                        onClick = { viewModel.updateType(TransactionType.CREDIT) },
                    )
                }

                Text("Category", style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    SpendingCategory.entries.forEach { cat ->
                        CategoryChip(
                            label = cat.displayName,
                            selected = formState.category == cat,
                            onClick = { viewModel.updateCategory(cat) },
                        )
                    }
                }

                DivaTextField(
                    value = formState.merchantName,
                    onValueChange = viewModel::updateMerchantName,
                    label = "Merchant Name (optional)",
                )

                DivaTextField(
                    value = formState.note,
                    onValueChange = viewModel::updateNote,
                    label = "Note (optional)",
                    singleLine = false,
                )

                if (cards.isNotEmpty() && formState.type == TransactionType.DEBIT) {
                    Text("Pay with Card (optional)", style = MaterialTheme.typography.labelLarge)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        CategoryChip(
                            label = "None",
                            selected = formState.selectedCardId == null,
                            onClick = { viewModel.updateSelectedCard(null) },
                        )
                        cards.forEach { card ->
                            CategoryChip(
                                label = card.name,
                                selected = formState.selectedCardId == card.id,
                                onClick = { viewModel.updateSelectedCard(card.id) },
                            )
                        }
                    }

                    if (formState.selectedCardId != null) {
                        val selectedCard = cards.find { it.id == formState.selectedCardId }
                        if (selectedCard != null) {
                            DivaCard {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = selectedCard.name,
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        text = "Available: ${"$" + selectedCard.availableCredit.toFixed(2)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            DivaButton(
                text = if (formState.isSaving) "Saving..." else "Add Transaction",
                onClick = { viewModel.saveTransaction(onSuccess = onBack) },
                modifier = Modifier.padding(16.dp),
                enabled = formState.amount.toDoubleOrNull() != null && !formState.isSaving,
            )
        }
    }
}
