package com.divafinance.core.ui.component.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.divafinance.core.common.toFixed
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.core.ui.theme.NumericStyle
import com.divafinance.core.ui.theme.diva
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SpendingPieChart(
    slices: List<ChartSlice>,
    totalSpending: Double,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val centerTextStyle = NumericStyle.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize)
    val centerLabelStyle = MaterialTheme.typography.labelSmall
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = diva.muted
    val trackColor = diva.fgHair

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(220.dp)) {
                val strokeWidth = 40f
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(
                    (size.width - diameter) / 2f,
                    (size.height - diameter) / 2f,
                )
                val arcSize = Size(diameter, diameter)
                var startAngle = -90f

                if (slices.isEmpty()) {
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth),
                    )
                } else {
                    slices.forEach { slice ->
                        val sweep = slice.fraction * 360f
                        drawArc(
                            color = slice.color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth),
                        )
                        startAngle += sweep
                    }
                }

                val totalText = totalSpending.toFixed(0)
                val totalLayout = textMeasurer.measure(totalText, centerTextStyle)
                drawText(
                    textLayoutResult = totalLayout,
                    color = onSurfaceColor,
                    topLeft = Offset(
                        (size.width - totalLayout.size.width) / 2f,
                        (size.height - totalLayout.size.height) / 2f - 10f,
                    ),
                )

                val labelText = "Total Spent"
                val labelLayout = textMeasurer.measure(labelText, centerLabelStyle)
                drawText(
                    textLayoutResult = labelLayout,
                    color = onSurfaceVariantColor,
                    topLeft = Offset(
                        (size.width - labelLayout.size.width) / 2f,
                        (size.height - labelLayout.size.height) / 2f + totalLayout.size.height / 2f + 4f,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            slices.forEach { slice ->
                LegendItem(
                    color = slice.color,
                    label = slice.label,
                    value = "${(slice.fraction * 100f).toFixed(1)}%",
                )
            }
        }
    }
}

@Preview
@Composable
private fun SpendingPieChartPreview() {
    DivaTheme {
        SpendingPieChart(
            slices = listOf(
                ChartSlice("Dining", 450.0, 0.35f, Color(0xFFD9A05C)),
                ChartSlice("Travel", 320.0, 0.25f, Color(0xFF7CB0DA)),
                ChartSlice("Gas", 180.0, 0.14f, Color(0xFFC9B27A)),
                ChartSlice("Groceries", 200.0, 0.16f, Color(0xFFA8C282)),
                ChartSlice("Other", 130.0, 0.10f, Color(0xFFA9A3AE)),
            ),
            totalSpending = 1280.0,
        )
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(color = color)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = NumericStyle,
            textAlign = TextAlign.End,
        )
    }
}
