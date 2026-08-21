package com.divafinance.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/*
 * Two corner scales.
 *
 * Material keeps the app's existing values verbatim — corners run larger than the M3
 * default because a 28dp `large` is what makes a full-width card read as a panel rather
 * than a box. Cupertino is tighter: HIG's grouped-inset card is r:14, and everything else
 * is scaled around it.
 *
 * Known fidelity gap: Compose has no continuous-corner (squircle) shape, so every
 * Cupertino corner here is a circular-arc approximation of the real thing.
 */
internal fun divaShapes(platform: DivaPlatform): Shapes = when (platform) {
    DivaPlatform.MATERIAL -> Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(18.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(34.dp),
    )
    DivaPlatform.CUPERTINO -> Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(14.dp),
        large = RoundedCornerShape(14.dp),
        extraLarge = RoundedCornerShape(20.dp),
    )
}
