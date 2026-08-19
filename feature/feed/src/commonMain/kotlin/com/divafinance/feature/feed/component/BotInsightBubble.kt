package com.divafinance.feature.feed.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.divafinance.core.model.FeedPost
import com.divafinance.core.model.enums.FeedPostType
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.core.ui.theme.Space
import com.divafinance.core.ui.theme.diva
import kotlin.time.Clock
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun BotInsightBubble(
    post: FeedPost,
    modifier: Modifier = Modifier,
) {
    DivaCard(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = diva.accent,
            )
            Spacer(modifier = Modifier.width(Space.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = post.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = diva.muted,
                )
                Text(
                    text = formatTimestamp(post.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = diva.muted,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun BotInsightBubblePreview() {
    DivaTheme {
        BotInsightBubble(
            post = FeedPost(
                id = "1",
                type = FeedPostType.BOT_INSIGHT,
                title = "Weekly Insight",
                body = "Your dining spending is up 15% this week compared to your average. Consider using your Amex Gold for 4x points.",
                createdAt = Clock.System.now(),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
