package com.divafinance.feature.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.divafinance.core.ui.adaptive.DivaScaffold
import com.divafinance.core.model.enums.CapPeriod
import com.divafinance.core.model.enums.RewardType
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.component.DivaTextField

@Composable
fun RewardMapperScreen(
    onBack: () -> Unit = {},
    viewModel: CardsViewModel,
) {
    val formState by viewModel.formState.collectAsState()

    DivaScaffold(
        title = "Reward Rules",
        onBack = onBack,
        actions = {
            IconButton(onClick = viewModel::addRewardRule) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = "Map spending categories to reward multipliers for this card.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (formState.rewardRules.isEmpty()) {
                    item {
                        Text(
                            text = "No reward rules. Tap + to add one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                }

                itemsIndexed(formState.rewardRules) { index, rule ->
                    RewardRuleEditor(
                        rule = rule,
                        onUpdate = { updated -> viewModel.updateRewardRule(index, updated) },
                        onRemove = { viewModel.removeRewardRule(index) },
                    )
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun RewardRuleEditor(
    rule: RewardRuleFormState,
    onUpdate: (RewardRuleFormState) -> Unit,
    onRemove: () -> Unit,
) {
    DivaCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Reward Rule", style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            EnumDropdown(
                label = "Category",
                selected = rule.category,
                entries = SpendingCategory.entries,
                displayName = { it.displayName },
                onSelect = { onUpdate(rule.copy(category = it)) },
            )

            Spacer(Modifier.height(8.dp))

            DivaTextField(
                value = rule.multiplier,
                onValueChange = { onUpdate(rule.copy(multiplier = it)) },
                label = "Multiplier (e.g., 3.0)",
            )

            Spacer(Modifier.height(8.dp))

            EnumDropdown(
                label = "Reward Type",
                selected = rule.rewardType,
                entries = RewardType.entries,
                displayName = { it.displayName },
                onSelect = { onUpdate(rule.copy(rewardType = it)) },
            )

            Spacer(Modifier.height(8.dp))

            DivaTextField(
                value = rule.capAmount,
                onValueChange = { onUpdate(rule.copy(capAmount = it)) },
                label = "Cap Amount (optional)",
            )

            Spacer(Modifier.height(8.dp))

            EnumDropdown(
                label = "Cap Period",
                selected = rule.capPeriod,
                entries = CapPeriod.entries,
                displayName = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
                onSelect = { onUpdate(rule.copy(capPeriod = it)) },
                nullable = true,
            )
        }
    }
}

@Composable
private fun <T> EnumDropdown(
    label: String,
    selected: T?,
    entries: List<T>,
    displayName: (T) -> String,
    onSelect: (T) -> Unit,
    nullable: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        TextButton(onClick = { expanded = true }) {
            Text(selected?.let(displayName) ?: "None")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(displayName(entry)) },
                    onClick = {
                        onSelect(entry)
                        expanded = false
                    },
                )
            }
        }
    }
}
