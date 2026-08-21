package com.divafinance.core.ui.component.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.theme.diva

/**
 * A bare trend line: no axes, no labels, no empty state.
 *
 * [TrendLineChart] is the full chart — 220dp tall, with drawn axis labels and its own
 * empty state — and is the wrong shape for the inch-high line that sits inside a card.
 * This is the inline one.
 */
@Composable
fun Sparkline(
    points: List<Double>,
    modifier: Modifier = Modifier,
    color: Color = diva.accent,
    height: Dp = 50.dp,
    strokeWidth: Dp = 2.5.dp,
    fill: Boolean = false,
) {
    if (points.size < 2) return

    val min = points.min()
    val max = points.max()
    // A flat run would divide by zero; draw it through the middle instead.
    val span = (max - min).takeIf { it > 0.0 }

    Canvas(modifier.fillMaxWidth().height(height)) {
        val stepX = size.width / (points.size - 1)
        val stroke = strokeWidth.toPx()
        // Inset by half the stroke so the line's cap is not clipped at the edges.
        val top = stroke / 2f
        val usable = size.height - stroke

        val offsets = points.mapIndexed { i, value ->
            val fraction = if (span == null) 0.5 else (value - min) / span
            Offset(i * stepX, top + ((1.0 - fraction) * usable).toFloat())
        }

        val line = Path().apply {
            moveTo(offsets.first().x, offsets.first().y)
            offsets.drop(1).forEach { lineTo(it.x, it.y) }
        }

        if (fill) {
            val area = Path().apply {
                addPath(line)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                area,
                Brush.verticalGradient(listOf(color.copy(alpha = 0.22f), Color.Transparent)),
            )
        }

        drawPath(line, color, style = Stroke(width = stroke))
    }
}
