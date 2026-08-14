package com.divafinance.feature.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.divafinance.core.model.CardRewardRule
import com.divafinance.core.model.CreditCard
import com.divafinance.core.ui.component.CreditCardVisual
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.component.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    cardId: String,
    onBack: () -> Unit = {},
    onEdit: (CreditCard) -> Unit = {},
    viewModel: CardsViewModel,
) {
    val cards by viewModel.cards.collectAsState()
    val card = cards.find { it.id == cardId }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(card?.name ?: "Card Details") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (card != null) {
                    IconButton(onClick = { onEdit(card) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Card")
                    }
                }
            },
        )

        if (card == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CreditCardVisual(
                name = card.name,
                lastFour = card.lastFour,
                network = card.network.displayName,
                color = parseCardColor(card.color),
            )

            CardInfoSection(card)

            if (card.rewardRules.isNotEmpty()) {
                RewardRulesSection(card.rewardRules)
            }
        }
    }
}

@Composable
private fun CardInfoSection(card: CreditCard) {
    DivaCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Card Information", style = MaterialTheme.typography.titleMedium)

            InfoRow("Network", card.network.displayName)
            if (card.creditLimit > 0) {
                InfoRow("Credit Limit", "${"$%.2f".format(card.creditLimit)}")
                InfoRow("Current Balance", "${"$%.2f".format(card.currentBalance)}")
                InfoRow("Available Credit", "${"$%.2f".format(card.availableCredit)}")
            }
            if (card.annualFee > 0) {
                InfoRow("Annual Fee", "${"$%.2f".format(card.annualFee)}")
            }
            if (card.statementDate != null) {
                InfoRow("Statement Day", "${card.statementDate}")
            }
            if (card.dueDate != null) {
                InfoRow("Due Day", "${card.dueDate}")
            }
            InfoRow("Status", if (card.isActive) "Active" else "Inactive")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun RewardRulesSection(rules: List<CardRewardRule>) {
    DivaCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Reward Rules", style = MaterialTheme.typography.titleMedium)

            rules.forEach { rule ->
                RewardRuleItem(rule)
            }
        }
    }
}

@Composable
private fun RewardRuleItem(rule: CardRewardRule) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rule.category.displayName,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = buildString {
                    append("${rule.multiplier}x ${rule.rewardType.displayName}")
                    if (rule.capAmount != null) {
                        append(" (cap: ${"$%.0f".format(rule.capAmount)}")
                        if (rule.capPeriod != null) {
                            append("/${rule.capPeriod.name.lowercase()}")
                        }
                        append(")")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun parseCardColor(hex: String): Color {
    return try {
        Color(("FF" + hex.removePrefix("#")).toLong(16))
    } catch (_: Exception) {
        Color(0xFF1E1E2E)
    }
}
