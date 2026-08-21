package com.divafinance.feature.feed.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.divafinance.core.model.FeedPost
import com.divafinance.core.model.enums.FeedPostType
import com.divafinance.core.ui.adaptive.DivaChip
import com.divafinance.core.ui.adaptive.DivaChipStyle
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.core.ui.theme.Space
import com.divafinance.core.ui.theme.diva
import kotlin.time.Clock
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * The generated insight, as the feed's one editorial card: a tinted eyebrow, the finding
 * in a size worth reading, the detail underneath, and the scope it was computed over.
 *
 * The chips are not filters — they state what the figure covers, which is the question a
 * reader asks of any number that claims to know something.
 */
@Composable
fun BotInsightBubble(
    post: FeedPost,
    modifier: Modifier = Modifier,
    scope: List<String> = emptyList(),
) {
    DivaCard(modifier = modifier) {
        Column(
            Modifier.fillMaxWidth().padding(Space.pad),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = diva.accent,
                )
                Text(
                    EYEBROW,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = diva.accent,
                )
            }

            Text(post.title, style = MaterialTheme.typography.headlineSmall)

            Text(
                post.body,
                style = MaterialTheme.typography.bodySmall,
                color = diva.muted,
            )

            if (scope.isNotEmpty()) {
                ScopeChips(scope)
            } else {
                Text(
                    formatTimestamp(post.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = diva.muted,
                    modifier = Modifier.padding(top = Space.xs),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScopeChips(scope: List<String>) {
    FlowRow(
        Modifier.fillMaxWidth().padding(top = Space.xs),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        scope.forEach { DivaChip(it, style = DivaChipStyle.Tonal) }
    }
}

private const val EYEBROW = "Today's insight"

@Preview
@Composable
private fun BotInsightBubblePreview() {
    DivaTheme {
        BotInsightBubble(
            post = FeedPost(
                id = "1",
                type = FeedPostType.BOT_INSIGHT,
                title = "You're pacing 18% under last week's spend",
                body = "Dining is the outlier — most of it landed at two places.",
                createdAt = Clock.System.now(),
            ),
            modifier = Modifier.padding(16.dp),
            scope = listOf("This week", "All accounts", "All categories"),
        )
    }
}
