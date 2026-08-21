package com.divafinance.core.ui.component.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.divafinance.core.common.toFixed
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.core.ui.theme.diva
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ThresholdBarChart(
    data: List<ChartBar>,
    modifier: Modifier = Modifier,
    limitCaption: String = "Dashed line = limit",
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
    val onSurfaceVariant = diva.muted
    val overColor = diva.negative
    val limitLineColor = diva.muted

    if (data.isEmpty()) {
        Column(
            modifier = modifier.fillMaxWidth().padding(32.dp),
        ) {
            Text(
                text = "No spending data available",
                style = MaterialTheme.typography.bodyMedium,
                color = diva.muted,
            )
        }
        return
    }

    val peak = data.maxOf { maxOf(it.value, it.limit ?: 0.0) }
    val chartMax = (peak * 1.2).coerceAtLeast(1.0)

    val barCount = data.size
    val barWidth = 48f
    val barSpacing = 24f
    val chartWidthDp = ((barWidth + barSpacing) * barCount + barSpacing).dp
    val chartHeight = 220.dp
    val bottomPadding = 40f
    val topPadding = 20f

    Column(modifier = modifier.fillMaxWidth()) {
        val scrollState = rememberScrollState()
        Canvas(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .width(chartWidthDp.coerceAtLeast(300.dp))
                .height(chartHeight),
        ) {
            val drawableHeight = size.height - bottomPadding - topPadding

            data.forEachIndexed { index, item ->
                val barHeight = (item.value / chartMax * drawableHeight).toFloat().coerceAtLeast(2f)
                val x = barSpacing + index * (barWidth + barSpacing)
                val y = topPadding + drawableHeight - barHeight

                val barColor = if (item.isOver) overColor else item.color

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4f, 4f),
                )

                item.limit?.let { limit ->
                    val thresholdY = topPadding + drawableHeight -
                        (limit / chartMax * drawableHeight).toFloat()
                    drawLine(
                        color = limitLineColor,
                        start = Offset(x - 4f, thresholdY),
                        end = Offset(x + barWidth + 4f, thresholdY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)),
                    )
                }

                val label = if (item.label.length > 6) item.label.take(5) + "." else item.label
                val labelLayout = textMeasurer.measure(
                    text = label,
                    style = labelStyle,
                    constraints = Constraints(maxWidth = (barWidth + barSpacing).toInt()),
                )
                drawText(
                    textLayoutResult = labelLayout,
                    color = onSurfaceVariant,
                    topLeft = Offset(
                        x + (barWidth - labelLayout.size.width) / 2f,
                        size.height - bottomPadding + 8f,
                    ),
                )

                val percentLabel = item.valueLabel ?: item.value.toFixed(0)
                val percentLayout = textMeasurer.measure(percentLabel, labelStyle)
                drawText(
                    textLayoutResult = percentLayout,
                    color = onSurfaceVariant,
                    topLeft = Offset(
                        x + (barWidth - percentLayout.size.width) / 2f,
                        y - percentLayout.size.height - 4f,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = limitCaption,
            style = MaterialTheme.typography.labelSmall,
            color = diva.muted,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Preview
@Composable
private fun ThresholdBarChartPreview() {
    DivaTheme {
        ThresholdBarChart(
            data = listOf(
                ChartBar("Dining", 450.0, Color(0xFFD9A05C), limit = 380.0, isOver = true),
                ChartBar("Travel", 320.0, Color(0xFF7CB0DA), limit = 400.0),
                ChartBar("Gas", 180.0, Color(0xFFC9B27A)),
            ),
        )
    }
}
