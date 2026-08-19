package com.divafinance.core.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.theme.Pill
import com.divafinance.core.ui.theme.diva

/** Which of the two tabs is showing. The compose button in the middle is not a tab. */
enum class DivaTab { FEED, YOU }

/**
 * The floating glass capsule.
 *
 * It **overlays** the content rather than displacing it, which is why callers add bottom
 * content padding themselves instead of handing this to `Scaffold(bottomBar = ...)`. The
 * compose button is the one place per screen the accent gradient is spent.
 */
@Composable
fun FloatingTabBar(
    selected: DivaTab,
    onSelect: (DivaTab) -> Unit,
    onCompose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(modifier = modifier, shape = Pill) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TabItem(
                label = "Feed",
                icon = if (selected == DivaTab.FEED) Icons.Filled.Home else Icons.Outlined.Home,
                selected = selected == DivaTab.FEED,
                onClick = { onSelect(DivaTab.FEED) },
            )
            ComposeButton(onClick = onCompose)
            TabItem(
                label = "You",
                icon = if (selected == DivaTab.YOU) Icons.Filled.Person else Icons.Outlined.Person,
                selected = selected == DivaTab.YOU,
                onClick = { onSelect(DivaTab.YOU) },
            )
        }
    }
}

@Composable
private fun TabItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tint by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else diva.muted,
        label = "tabTint",
    )
    Column(
        Modifier
            .width(72.dp)
            .clip(Pill)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tint,
        )
    }
}

@Composable
private fun ComposeButton(onClick: () -> Unit) {
    // No ripple: the gradient fill is the affordance, and a ripple over it reads as grime.
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(diva.sweep)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = "Add transaction",
            tint = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.size(28.dp),
        )
    }
}
