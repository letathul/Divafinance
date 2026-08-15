package com.divafinance.feature.feed.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
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
import kotlin.time.Clock
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun TransactionFeedItem(
    post: FeedPost,
    modifier: Modifier = Modifier,
) {
    DivaCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = post.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatTimestamp(post.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun TransactionFeedItemPreview() {
    DivaTheme {
        TransactionFeedItem(
            post = FeedPost(
                id = "1",
                type = FeedPostType.TRANSACTION,
                title = "Spent $42.50 at Starbucks",
                body = "Dining - paid with Chase Sapphire (3x points)",
                createdAt = Clock.System.now(),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
