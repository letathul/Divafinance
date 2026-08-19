package com.divafinance.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.theme.Pill

/**
 * A lens over whatever it sits on: heavy translucency plus a specular top edge.
 *
 * The genuine article blurs what is *behind* the element. Compose has no backdrop filter
 * on any target — `Modifier.blur` and `RenderEffect` both blur the composable's own
 * content, so neither reproduces it. This is the honest fallback, and the specular edge
 * does most of the perceptual work regardless.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = Pill,
    content: @Composable BoxScope.() -> Unit,
) {
    val fg = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .background(
                Brush.verticalGradient(
                    0f to fg.copy(alpha = 0.10f),
                    0.4f to Color.Transparent,
                    1f to fg.copy(alpha = 0.03f),
                )
            )
            .border(BorderStroke(1.dp, fg.copy(alpha = 0.14f)), shape),
        content = content,
    )
}
