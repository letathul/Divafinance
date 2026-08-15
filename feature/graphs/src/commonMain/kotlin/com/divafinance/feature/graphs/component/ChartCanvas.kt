package com.divafinance.feature.graphs.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ChartCanvas(
    modifier: Modifier = Modifier,
    chartHeight: Dp = 200.dp,
    onDraw: DrawScope.() -> Unit,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight),
        onDraw = onDraw,
    )
}

fun DrawScope.drawGridLines(
    lineCount: Int = 5,
    color: Color = Color.LightGray.copy(alpha = 0.3f),
    strokeWidth: Float = 1f,
) {
    val spacing = size.height / (lineCount + 1)
    for (i in 1..lineCount) {
        val y = spacing * i
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokeWidth,
        )
    }
}

fun DrawScope.drawLinePath(
    points: List<Offset>,
    color: Color,
    strokeWidth: Float = 3f,
) {
    if (points.size < 2) return

    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
}

fun DrawScope.drawDataPoints(
    points: List<Offset>,
    color: Color,
    radius: Float = 4f,
) {
    points.forEach { point ->
        drawCircle(color = color, radius = radius, center = point)
    }
}

fun normalizePoints(
    values: List<Double>,
    width: Float,
    height: Float,
    paddingHorizontal: Float = 16f,
    paddingVertical: Float = 16f,
): List<Offset> {
    if (values.isEmpty()) return emptyList()

    val max = values.max().coerceAtLeast(1.0)
    val min = values.min().coerceAtMost(0.0)
    val range = (max - min).coerceAtLeast(1.0)

    val drawWidth = width - 2 * paddingHorizontal
    val drawHeight = height - 2 * paddingVertical

    return values.mapIndexed { index, value ->
        val x = if (values.size > 1) {
            paddingHorizontal + (index.toFloat() / (values.size - 1)) * drawWidth
        } else {
            width / 2f
        }
        val y = paddingVertical + drawHeight - ((value - min) / range * drawHeight).toFloat()
        Offset(x, y)
    }
}
