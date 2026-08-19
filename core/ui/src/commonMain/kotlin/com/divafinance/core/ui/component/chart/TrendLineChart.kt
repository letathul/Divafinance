package com.divafinance.core.ui.component.chart

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
import com.divafinance.core.common.toFixed
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.core.ui.theme.diva
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun TrendLineChart(
    data: List<ChartPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
    val onSurfaceVariant = diva.muted
    val gridColor = diva.fgHair
    val fillColorStart = lineColor.copy(alpha = 0.25f)
    val fillColorEnd = lineColor.copy(alpha = 0f)

    if (data.isEmpty()) {
        Column(
            modifier = modifier.fillMaxWidth().padding(32.dp),
        ) {
            Text(
                text = "No trend data available",
                style = MaterialTheme.typography.bodyMedium,
                color = diva.muted,
            )
        }
        return
    }

    val maxAmount = data.maxOf { it.value }.coerceAtLeast(1.0)
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
            val y = topPadding + drawableHeight - (point.value / maxAmount * drawableHeight).toFloat()
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
                color = lineColor,
                radius = 5f,
                center = point,
            )
        }

        data.forEachIndexed { index, point ->
            val labelLayout = textMeasurer.measure(point.label, labelStyle)
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
                color = gridColor,
                start = Offset(leftPadding, y),
                end = Offset(leftPadding + drawableWidth, y),
                strokeWidth = 1f,
            )
            val value = maxAmount * (1.0 - i.toDouble() / gridLines)
            val valueLabel = if (value >= 1000) "${(value / 1000).toFixed(0)}k" else value.toFixed(0)
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

@Preview
@Composable
private fun TrendLineChartPreview() {
    DivaTheme {
        TrendLineChart(
            data = listOf(
                ChartPoint("Jan", 1200.0),
                ChartPoint("Feb", 980.0),
                ChartPoint("Mar", 1450.0),
                ChartPoint("Apr", 1100.0),
                ChartPoint("May", 1320.0),
                ChartPoint("Jun", 890.0),
            ),
        )
    }
}
