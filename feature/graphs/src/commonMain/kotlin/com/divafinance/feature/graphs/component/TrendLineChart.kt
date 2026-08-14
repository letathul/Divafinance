package com.divafinance.feature.graphs.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.divafinance.feature.graphs.TrendPoint

private val lineColor = Color(0xFFD4A843)
private val fillColorStart = Color(0x40D4A843)
private val fillColorEnd = Color(0x00D4A843)
private val dotColor = Color(0xFFD4A843)

@Composable
fun TrendLineChart(
    data: List<TrendPoint>,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    if (data.isEmpty()) {
        Column(
            modifier = modifier.fillMaxWidth().padding(32.dp),
        ) {
            Text(
                text = "No trend data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val maxAmount = data.maxOf { it.amount }.coerceAtLeast(1.0)
    val chartHeight = 220.dp
    val leftPadding = 48f
    val rightPadding = 24f
    val topPadding = 20f
    val bottomPadding = 40f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
            .padding(horizontal = 8.dp),
    ) {
        val drawableWidth = size.width - leftPadding - rightPadding
        val drawableHeight = size.height - topPadding - bottomPadding

        val points = data.mapIndexed { index, point ->
            val x = if (data.size > 1) {
                leftPadding + (index.toFloat() / (data.size - 1)) * drawableWidth
            } else {
                leftPadding + drawableWidth / 2f
            }
            val y = topPadding + drawableHeight - (point.amount / maxAmount * drawableHeight).toFloat()
            Offset(x, y)
        }

        if (points.size >= 2) {
            val fillPath = Path().apply {
                moveTo(points.first().x, topPadding + drawableHeight)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, topPadding + drawableHeight)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(fillColorStart, fillColorEnd),
                    startY = topPadding,
                    endY = topPadding + drawableHeight,
                ),
            )

            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(
                    width = 3f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }

        points.forEach { point ->
            drawCircle(
                color = dotColor,
                radius = 5f,
                center = point,
            )
        }

        data.forEachIndexed { index, point ->
            val labelLayout = textMeasurer.measure(point.monthLabel, labelStyle)
            drawText(
                textLayoutResult = labelLayout,
                color = onSurfaceVariant,
                topLeft = Offset(
                    points[index].x - labelLayout.size.width / 2f,
                    size.height - bottomPadding + 8f,
                ),
            )
        }

        val gridLines = 4
        for (i in 0..gridLines) {
            val y = topPadding + (drawableHeight / gridLines) * i
            drawLine(
                color = Color.LightGray.copy(alpha = 0.3f),
                start = Offset(leftPadding, y),
                end = Offset(leftPadding + drawableWidth, y),
                strokeWidth = 1f,
            )
            val value = maxAmount * (1.0 - i.toDouble() / gridLines)
            val valueLabel = if (value >= 1000) "${"%.0f".format(value / 1000)}k" else "${"%.0f".format(value)}"
            val valueLayout = textMeasurer.measure(valueLabel, labelStyle)
            drawText(
                textLayoutResult = valueLayout,
                color = onSurfaceVariant,
                topLeft = Offset(
                    leftPadding - valueLayout.size.width - 6f,
                    y - valueLayout.size.height / 2f,
                ),
            )
        }
    }
}
