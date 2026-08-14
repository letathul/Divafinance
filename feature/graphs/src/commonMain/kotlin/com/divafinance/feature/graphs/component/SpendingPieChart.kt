package com.divafinance.feature.graphs.component

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
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.feature.graphs.SpendingSlice
import org.jetbrains.compose.ui.tooling.preview.Preview

private val chartColors = listOf(
    Color(0xFFD4A843),
    Color(0xFF2196F3),
    Color(0xFF4CAF50),
    Color(0xFFE53935),
    Color(0xFFFF9800),
    Color(0xFF9C27B0),
    Color(0xFF00BCD4),
    Color(0xFFFF5722),
    Color(0xFF607D8B),
    Color(0xFF795548),
    Color(0xFFCDDC39),
    Color(0xFFE91E63),
)

@Composable
fun SpendingPieChart(
    slices: List<SpendingSlice>,
    totalSpending: Double,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val centerTextStyle = MaterialTheme.typography.titleLarge
    val centerLabelStyle = MaterialTheme.typography.labelSmall
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

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
                        color = Color.LightGray,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth),
                    )
                } else {
                    slices.forEachIndexed { index, slice ->
                        val sweep = slice.percent / 100f * 360f
                        drawArc(
                            color = chartColors[index % chartColors.size],
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

                val totalText = "${"%.0f".format(totalSpending)}"
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
            slices.forEachIndexed { index, slice ->
                LegendItem(
                    color = chartColors[index % chartColors.size],
                    label = slice.category,
                    value = "${"%.1f".format(slice.percent)}%",
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
                SpendingSlice("Dining", 450.0, 35f),
                SpendingSlice("Travel", 320.0, 25f),
                SpendingSlice("Gas", 180.0, 14f),
                SpendingSlice("Groceries", 200.0, 16f),
                SpendingSlice("Other", 130.0, 10f),
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
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
        )
    }
}
