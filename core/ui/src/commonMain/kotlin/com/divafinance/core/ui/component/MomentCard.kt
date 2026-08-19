package com.divafinance.core.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.ui.theme.Pill
import com.divafinance.core.ui.theme.Space
import com.divafinance.core.ui.theme.diva

/**
 * The feed's "post": a spend worth remembering, given the full width.
 *
 * The cover is a tonal duotone derived from the category rather than a photograph. There
 * is no image pipeline in this app, and for a ledger a derived wash reads as product
 * rather than as missing stock art.
 */
@Composable
fun MomentCard(
    title: String,
    subtitle: String,
    amount: String,
    caption: String,
    category: SpendingCategory,
    modifier: Modifier = Modifier,
    badge: String? = null,
    splitWith: List<String> = emptyList(),
    footnote: String? = null,
    onClick: () -> Unit = {},
) {
    var flagged by remember { mutableStateOf(false) }
    var burst by remember { mutableIntStateOf(0) }
    val tint = category.color

    DivaCard(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = Space.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Avatar(initialsOf(title), tint)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = diva.muted)
            }
            Icon(Icons.Outlined.MoreHoriz, "More options", tint = diva.muted)
        }

        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(
                    Brush.linearGradient(
                        listOf(tint.copy(alpha = 0.30f), tint.copy(alpha = 0.07f))
                    )
                )
                .pointerInput(Unit) {
                    // Double-tap to flag: the one gesture from that other feed worth keeping.
                    detectTapGestures(
                        onDoubleTap = { flagged = true; burst++ },
                        onTap = { onClick() },
                    )
                },
        ) {
            Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                Text(amount, style = MaterialTheme.typography.displayMedium)
                Text(
                    caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                )
            }
            if (badge != null) {
                GlassSurface(
                    Modifier.align(Alignment.TopStart).padding(14.dp),
                    shape = Pill,
                ) {
                    Text(
                        badge.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    )
                }
            }
            HeartBurst(trigger = burst, modifier = Modifier.align(Alignment.Center))
        }

        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = Space.md, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IconButton(onClick = { flagged = !flagged }) {
                Icon(
                    if (flagged) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (flagged) "Unflag" else "Flag this expense",
                    tint = if (flagged) diva.accent else diva.muted,
                )
            }
            if (splitWith.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy((-9).dp)) {
                    splitWith.take(3).forEachIndexed { index, name ->
                        Avatar(
                            initialsOf(name),
                            diva.categoryColor(SpendingCategory.entries[index % SpendingCategory.entries.size]),
                            size = 26.dp,
                        )
                    }
                }
                Spacer(Modifier.width(Space.sm))
            }
            if (footnote != null) {
                Text(footnote, style = MaterialTheme.typography.bodySmall, color = diva.muted)
            }
        }
    }
}

@Composable
private fun HeartBurst(trigger: Int, modifier: Modifier = Modifier) {
    if (trigger == 0) return
    val scale = remember(trigger) { Animatable(0.4f) }
    val alpha = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) {
        alpha.snapTo(1f)
        scale.snapTo(0.4f)
        scale.animateTo(1.14f, tween(180, easing = FastOutSlowInEasing))
        scale.animateTo(1f, tween(160))
        alpha.animateTo(0f, tween(320, delayMillis = 180))
    }
    Icon(
        Icons.Filled.Favorite,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha.value),
        modifier = modifier.size(92.dp).scale(scale.value),
    )
}
