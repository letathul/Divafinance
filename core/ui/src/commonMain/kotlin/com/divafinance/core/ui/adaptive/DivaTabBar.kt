package com.divafinance.core.ui.adaptive

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.ripple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.theme.Pill
import com.divafinance.core.ui.theme.Space
import com.divafinance.core.ui.theme.diva
import com.divafinance.core.ui.theme.isCupertino

/** Which of the two tabs is showing. The compose button in the middle is not a tab. */
enum class DivaTab { FEED, YOU }

/** The one place the bottom bar's geometry is named. */
object DivaBottomBar {
    /** The bar body, above the navigation-bar inset it adds for itself. */
    val Height: Dp = 56.dp

    val CenterButton: Dp = 56.dp

    /** How far the compose button pokes above the bar's top edge. */
    val CenterRaise: Dp = 22.dp

    val IconSize: Dp = 24.dp
}

/**
 * The bottom inset a screen behind the bar must leave.
 *
 * Replaces the 120dp / 104dp magic numbers that used to be copied into the feed, the You
 * tab and the snackbar independently.
 */
@Composable
fun divaContentPadding(top: Dp = 0.dp, extraBottom: Dp = Space.pad): PaddingValues =
    PaddingValues(
        top = top,
        bottom = DivaBottomBar.Height + DivaBottomBar.CenterRaise + extraBottom +
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
    )

/**
 * The shell's bottom bar: edge-pinned, full width, translucent, with a raised circular
 * compose button straddling its top edge.
 *
 * It **overlays** the content rather than displacing it — which is why it is aligned into
 * a `Box` rather than handed to `Scaffold(bottomBar = …)`, and why callers leave room
 * with [divaContentPadding] instead. It consumes the navigation-bar inset itself and
 * draws its fill all the way to the screen edge, so on Android it *is* the navigation
 * bar's background.
 *
 * There is no backdrop blur on any Compose target, so the translucency here is a flat
 * alpha over whatever is behind it plus a hairline top edge — the same honest fallback
 * [com.divafinance.core.ui.component.GlassSurface] uses.
 */
@Composable
fun DivaTabBar(
    selected: DivaTab,
    onSelect: (DivaTab) -> Unit,
    onCompose: () -> Unit,
    modifier: Modifier = Modifier,
    composeLabel: String = "Add transaction",
) {
    val separator = diva.separator
    val hairline = diva.hairline
    // Translucent is a HIG idiom and it needs a real blur to work, which no Compose
    // target has. Material's bottom bar is opaque, so only Cupertino pays the cost of
    // the honest fallback — on Android the ledger was plainly readable through it.
    val fill = if (isCupertino) diva.barFill.copy(alpha = 0.94f) else diva.barFill

    // The Box is taller than the bar and its top half is transparent, so the raised
    // compose button has somewhere to live: a child offset above an opaque parent's
    // bounds gets clipped away.
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(fill)
                .drawBehind {
                    drawLine(
                        color = separator,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = hairline.toPx(),
                    )
                }
                .navigationBarsPadding(),
        ) {
            Row(
                Modifier.fillMaxWidth().height(DivaBottomBar.Height),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TabItem(
                    label = "Feed",
                    icon = if (selected == DivaTab.FEED) Icons.Filled.Home else Icons.Outlined.Home,
                    selected = selected == DivaTab.FEED,
                    onClick = { onSelect(DivaTab.FEED) },
                )
                // The compose button is drawn in the overlay below; this reserves its
                // slot so the two tabs sit either side of it rather than under it.
                Box(Modifier.width(DivaBottomBar.CenterButton))
                TabItem(
                    label = "You",
                    icon = if (selected == DivaTab.YOU) Icons.Filled.Person else Icons.Outlined.Person,
                    selected = selected == DivaTab.YOU,
                    onClick = { onSelect(DivaTab.YOU) },
                )
            }
        }

        ComposeButton(
            onClick = onCompose,
            label = composeLabel,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(bottom = DivaBottomBar.CenterRaise),
        )
    }
}

@Composable
private fun TabItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // On Cupertino the selected tab is the accent; on Material it is the foreground,
    // which is what `primary` resolves to there.
    val tint by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else diva.muted,
        label = "tabTint",
    )
    Column(
        Modifier
            .width(88.dp)
            .clip(Pill)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = Space.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(DivaBottomBar.IconSize))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = tint,
        )
    }
}

/**
 * Solid accent on both platforms — this is the one piece of chrome that stays branded
 * whichever language the app is speaking.
 */
@Composable
private fun ComposeButton(onClick: () -> Unit, label: String, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(DivaBottomBar.CenterButton)
            .clip(CircleShape)
            .background(diva.accent)
            .clickable(
                interactionSource = interaction,
                // No ripple on Cupertino: iOS controls do not ripple, and one over a
                // saturated fill reads as grime.
                indication = if (isCupertino) null else ripple(),
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = label,
            tint = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.size(28.dp),
        )
    }
}
