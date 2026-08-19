package com.divafinance.feature.cards.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.divafinance.core.common.toFixed
import com.divafinance.core.domain.engine.CardRecommendation
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.theme.diva

@Composable
fun CardRecommendationCard(
    recommendation: CardRecommendation,
    rank: Int,
    modifier: Modifier = Modifier,
) {
    DivaCard(modifier = modifier) {
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
                    text = "+${"$" + recommendation.estimatedRewardValue.toFixed(2)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = diva.positive,
                )
            }

            Spacer(Modifier.height(4.dp))

            recommendation.rule?.let { rule ->
                Text(
                    text = "${rule.multiplier}x ${rule.rewardType.displayName} on ${rule.category.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = "Available credit: ${"$" + recommendation.availableCredit.toFixed(2)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            recommendation.remainingCap?.let { remainingCap ->
                Text(
                    text = "Remaining cap: ${"$" + remainingCap.toFixed(2)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
