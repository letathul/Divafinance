package com.divafinance.feature.cards

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.adaptive.DivaScaffold
import com.divafinance.core.model.enums.CapPeriod
import com.divafinance.core.model.enums.CardNetwork
import com.divafinance.core.model.enums.RewardType
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.ui.component.CategoryChip
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.component.DivaTextField

@Composable
fun AddEditCardScreen(
    onBack: () -> Unit = {},
    viewModel: CardsViewModel,
) {
    val formState by viewModel.formState.collectAsState()

    DivaScaffold(
        title = if (formState.isEditing) "Edit Card" else "Add Card",
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
                Text("Card Details", style = MaterialTheme.typography.titleMedium)

                DivaTextField(
                    value = formState.name,
                    onValueChange = viewModel::updateName,
                    label = "Card Name",
                )

                DivaTextField(
                    value = formState.lastFour,
                    onValueChange = viewModel::updateLastFour,
                    label = "Last 4 Digits",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )

                Text("Network", style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    CardNetwork.entries.forEach { network ->
                        CategoryChip(
                            label = network.displayName,
                            selected = formState.network == network,
                            onClick = { viewModel.updateNetwork(network) },
                        )
                    }
                }

                DivaTextField(
                    value = formState.creditLimit,
                    onValueChange = viewModel::updateCreditLimit,
                    label = "Credit Limit",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )

                DivaTextField(
                    value = formState.annualFee,
                    onValueChange = viewModel::updateAnnualFee,
                    label = "Annual Fee",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DivaTextField(
                        value = formState.statementDate,
                        onValueChange = viewModel::updateStatementDate,
                        label = "Statement Day",
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    DivaTextField(
                        value = formState.dueDate,
                        onValueChange = viewModel::updateDueDate,
                        label = "Due Day",
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }

                Spacer(Modifier.height(8.dp))
                RewardRulesSection(
                    rules = formState.rewardRules,
                    onAddRule = viewModel::addRewardRule,
                    onRemoveRule = viewModel::removeRewardRule,
                    onUpdateRule = viewModel::updateRewardRule,
                )

                Spacer(Modifier.height(16.dp))
            }

            DivaButton(
                text = if (formState.isSaving) "Saving..." else if (formState.isEditing) "Update Card" else "Add Card",
                onClick = { viewModel.saveCard(onSuccess = onBack) },
                modifier = Modifier.padding(16.dp),
                enabled = formState.name.isNotBlank() && !formState.isSaving,
            )
        }
    }
}

@Composable
private fun RewardRulesSection(
    rules: List<RewardRuleFormState>,
    onAddRule: () -> Unit,
    onRemoveRule: (Int) -> Unit,
    onUpdateRule: (Int, RewardRuleFormState) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Reward Rules", style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onAddRule) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("Add Rule")
        }
    }

    if (rules.isEmpty()) {
        Text(
            text = "No reward rules. Add rules to track cashback, points, or miles.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }

    rules.forEachIndexed { index, rule ->
        RewardRuleEditor(
            rule = rule,
            index = index,
            onUpdate = { onUpdateRule(index, it) },
            onRemove = { onRemoveRule(index) },
        )
    }
}

@Composable
private fun RewardRuleEditor(
    rule: RewardRuleFormState,
    index: Int,
    onUpdate: (RewardRuleFormState) -> Unit,
    onRemove: () -> Unit,
) {
    DivaCard {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Rule ${index + 1}", style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove rule",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Text("Category", style = MaterialTheme.typography.labelMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                SpendingCategory.entries.forEach { cat ->
                    CategoryChip(
                        label = cat.displayName,
                        selected = rule.category == cat,
                        onClick = { onUpdate(rule.copy(category = cat)) },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DivaTextField(
                    value = rule.multiplier,
                    onValueChange = { onUpdate(rule.copy(multiplier = it)) },
                    label = "Multiplier (e.g. 3.0)",
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                DivaTextField(
                    value = rule.capAmount,
                    onValueChange = { onUpdate(rule.copy(capAmount = it)) },
                    label = "Cap Amount",
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }

            Text("Reward Type", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RewardType.entries.forEach { type ->
                    CategoryChip(
                        label = type.displayName,
                        selected = rule.rewardType == type,
                        onClick = { onUpdate(rule.copy(rewardType = type)) },
                    )
                }
            }

            if (rule.capAmount.isNotBlank()) {
                Text("Cap Period", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CapPeriod.entries.forEach { period ->
                        CategoryChip(
                            label = period.name.lowercase().replaceFirstChar { it.uppercase() },
                            selected = rule.capPeriod == period,
                            onClick = { onUpdate(rule.copy(capPeriod = period)) },
                        )
                    }
                }
            }
        }
    }
}
