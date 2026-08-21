package com.divafinance.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.theme.diva

/** Initials on a flat colour. No photos anywhere in this app. */
@Composable
fun Avatar(
    initials: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
) {
    Box(
        modifier.size(size).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.background,
        )
    }
}

/** Sweep-ringed avatar — the profile's headline treatment, and one of its two accents. */
@Composable
fun AvatarRing(
    initials: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
) {
    Box(
        modifier.size(size + 6.dp).clip(CircleShape).background(diva.sweepConic),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(size - 6.dp).clip(CircleShape).background(color),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    initials,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.background,
                )
            }
        }
    }
}

/** Initials from a display name: "Athul Babu" -> "AB", "Mara" -> "MA". */
fun initialsOf(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}
