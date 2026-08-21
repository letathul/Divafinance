package com.divafinance.core.ui.adaptive

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.theme.Pill
import com.divafinance.core.ui.theme.diva
import com.divafinance.core.ui.theme.isCupertino

/**
 * A switch in the platform's own shape.
 *
 * Material delegates to M3. Cupertino draws the HIG control — a 51×31 track with a 27dp
 * thumb — because M3's switch, with its icon and its shrinking thumb, is unmistakably
 * not an iOS switch.
 *
 * The hand-drawn branch applies [toggleable] with `Role.Switch` so its semantics are
 * identical to M3's: `assertIsOn`, `assertIsOff` and `assertIsToggleable` all keep
 * working, which they would not if this were a plain `clickable`.
 */
@Composable
fun DivaSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (!isCupertino) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
        )
        return
    }

    val trackOff = if (diva.isDark) Color(0xFF39393D) else Color(0xFFE9E9EA)
    val track by animateColorAsState(
        if (checked) diva.positive else trackOff,
        label = "switchTrack",
    )
    val thumbOffset by animateDpAsState(if (checked) 20.dp else 0.dp, label = "switchThumb")

    Box(
        modifier
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .width(51.dp)
            .height(31.dp)
            .clip(Pill)
            .background(if (enabled) track else track.copy(alpha = 0.5f))
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = thumbOffset)
                .size(27.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
