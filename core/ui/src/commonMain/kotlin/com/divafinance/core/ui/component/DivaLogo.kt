package com.divafinance.core.ui.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.divafinance.core.ui.theme.diva

/**
 * The wordmark, drawn in type rather than shipped as an asset.
 *
 * There is no `composeResources/` directory in this project and this deliberately does
 * not introduce one — swapping in a real mark later means replacing this one file and
 * the two brand colours in `Color.kt`.
 */
enum class DivaLogoSize(val fontSize: TextUnit, val tracking: TextUnit) {
    Small(17.sp, (-0.4).sp),
    Medium(24.sp, (-0.8).sp),
    Large(34.sp, (-1.2).sp),
}

@Composable
fun DivaLogo(
    modifier: Modifier = Modifier,
    size: DivaLogoSize = DivaLogoSize.Medium,
    color: Color = diva.brand,
) {
    Text(
        text = WORDMARK,
        // Read as one word rather than four letters.
        modifier = modifier.semantics { contentDescription = WORDMARK },
        color = color,
        fontSize = size.fontSize,
        fontWeight = FontWeight.Black,
        letterSpacing = size.tracking,
    )
}

private const val WORDMARK = "diva"
