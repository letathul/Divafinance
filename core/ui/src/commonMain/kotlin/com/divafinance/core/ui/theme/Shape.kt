package com.divafinance.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/*
 * Corners run larger than the M3 default. The design leans on soft, near-superelliptical
 * cards, and a 28dp `large` is what makes a full-width card read as a panel rather than a
 * box. `Pill` (in Tokens.kt) covers everything fully rounded.
 */
val DivaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp),
)
