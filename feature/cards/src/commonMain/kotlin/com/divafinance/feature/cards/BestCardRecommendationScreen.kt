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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.divafinance.core.domain.engine.CardRecommendation
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.ui.component.CategoryChip
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.component.DivaTextField
import com.divafinance.core.ui.component.LoadingIndicator
import com.divafinance.core.ui.theme.DivaGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BestCardRecommendationScreen(
    onBack: () -> Unit = {},
    viewModel: CardsViewModel,
) {
    val state by viewModel.recommendationState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Best Card to Pay") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Select a category and amount to find the best card.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                Text("Category", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    SpendingCategory.entries.forEach { cat ->
                        CategoryChip(
                            label = cat.displayName,
                            selected = state.selectedCategory == cat,
                            onClick = { viewModel.updateRecommendationCategory(cat) },
                        )
                    }
                }
            }

            item {
                DivaTextField(
                    value = state.amount,
                    onValueChange = viewModel::updateRecommendationAmount,
                    label = "Amount ($)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }

            item {
                DivaButton(
                    text = "Find Best Card",
                    onClick = viewModel::fetchRecommendations,
                    enabled = state.amount.toDoubleOrNull() != null && !state.isLoading,
                )
            }

            if (state.isLoading) {
                item { LoadingIndicator() }
            }

            if (state.recommendations.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("Recommendations", style = MaterialTheme.typography.titleMedium)
                }

                itemsIndexed(state.recommendations) { index, rec ->
                    RecommendationCard(recommendation = rec, rank = index + 1)
                }
            } else if (!state.isLoading && state.recommendations.isEmpty()) {
                item {
                    Text(
                        text = "No matching cards found. Try a different category or amount.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: CardRecommendation,
    rank: Int,
) {
    DivaCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "#$rank ${recommendation.card.name}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "+${"$%.2f".format(recommendation.estimatedRewardValue)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = DivaGreen,
                )
            }

            Spacer(Modifier.height(4.dp))

            if (recommendation.rule != null) {
                Text(
                    text = "${recommendation.rule.multiplier}x ${recommendation.rule.rewardType.displayName} on ${recommendation.rule.category.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = "Available credit: ${"$%.2f".format(recommendation.availableCredit)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (recommendation.remainingCap != null) {
                Text(
                    text = "Remaining cap: ${"$%.2f".format(recommendation.remainingCap)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
