package com.divafinance.feature.feed.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.theme.Space
import com.divafinance.core.ui.theme.diva

/**
 * One ring. [value] is the figure drawn inside it when there is no [icon] — a percentage,
 * a short total — and [ringed] is false for an entry that is a destination rather than a
 * reading, so it reads as a plain button.
 */
@Immutable
data class StoryRing(
    val id: String,
    val caption: String,
    val tint: Color,
    val value: String? = null,
    val icon: ImageVector? = null,
    val ringed: Boolean = true,
)

/**
 * The row of circles above the feed: what today cost, which period is running hot, and
 * the way through to the charts.
 *
 * It is a reading, not decoration — every ring is one figure the feed already computes,
 * placed where it can be scanned before any scrolling happens.
 */
@Composable
fun StoryRingRow(
    rings: List<StoryRing>,
    modifier: Modifier = Modifier,
    ringSize: Dp = 58.dp,
    onClick: (StoryRing) -> Unit = {},
) {
    if (rings.isEmpty()) return
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Space.pad, vertical = Space.sm),
        horizontalArrangement = Arrangement.spacedBy(Space.pad),
    ) {
        rings.forEach { ring ->
            RingItem(ring, ringSize) { onClick(ring) }
        }
    }
}

@Composable
private fun RingItem(ring: StoryRing, ringSize: Dp, onClick: () -> Unit) {
    Column(
        Modifier.widthIn(min = ringSize).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(ringSize)
                .clip(CircleShape)
                .then(
                    if (ring.ringed) {
                        Modifier.border(2.5.dp, ring.tint, CircleShape)
                    } else {
                        Modifier.border(diva.hairline, diva.separator, CircleShape)
                    }
                )
                .padding(4.dp)
                .clip(CircleShape)
                .background(diva.card),
            contentAlignment = Alignment.Center,
        ) {
            when {
                ring.icon != null -> Icon(
                    ring.icon,
                    contentDescription = null,
                    tint = ring.tint,
                    modifier = Modifier.size(20.dp),
                )
                ring.value != null -> Text(
                    ring.value,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ring.tint,
                    maxLines = 1,
                )
            }
        }
        Text(
            ring.caption,
            style = MaterialTheme.typography.labelMedium,
            color = if (ring.ringed) {
                MaterialTheme.colorScheme.onBackground
            } else {
                diva.muted
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
