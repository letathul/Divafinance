package com.divafinance.feature.graphs.component

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
import com.divafinance.core.domain.usecase.graphs.CategoryThresholdData
import com.divafinance.core.model.GraphThreshold
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.ui.theme.DivaTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

private val barGreen = Color(0xFF4CAF50)
private val barRed = Color(0xFFE53935)
private val barGray = Color(0xFFBDBDBD)
private val thresholdLineColor = Color(0xFFFF9800)

@Composable
fun ThresholdBarChart(
    data: List<CategoryThresholdData>,
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
                text = "No spending data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val totalSpending = data.sumOf { it.spending }
    val maxPercent = if (totalSpending > 0) {
        data.maxOf { it.spending / totalSpending * 100.0 }.coerceAtLeast(10.0)
    } else {
        100.0
    }
    val chartMax = (maxPercent * 1.2).coerceAtLeast(10.0)

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
                val percent = if (totalSpending > 0) item.spending / totalSpending * 100.0 else 0.0
                val barHeight = (percent / chartMax * drawableHeight).toFloat().coerceAtLeast(2f)
                val x = barSpacing + index * (barWidth + barSpacing)
                val y = topPadding + drawableHeight - barHeight

                val barColor = when {
                    item.isOverThreshold -> barRed
                    item.threshold != null -> barGreen
                    else -> barGray
                }

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4f, 4f),
                )

                if (item.threshold != null) {
                    val thresholdY = topPadding + drawableHeight -
                        (item.threshold.thresholdPercent / chartMax * drawableHeight).toFloat()
                    drawLine(
                        color = thresholdLineColor,
                        start = Offset(x - 4f, thresholdY),
                        end = Offset(x + barWidth + 4f, thresholdY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)),
                    )
                }

                val label = if (item.category.length > 6) {
                    item.category.take(5) + "."
                } else {
                    item.category
                }
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

                val percentLabel = "${"%.0f".format(percent)}%"
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
            text = "Orange dashed line = threshold limit",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                CategoryThresholdData(
                    category = "Dining",
                    spending = 450.0,
                    threshold = GraphThreshold("1", SpendingCategory.DINING, 30.0),
                    percentOfThreshold = 117.0,
                    isOverThreshold = true,
                ),
                CategoryThresholdData(
                    category = "Travel",
                    spending = 320.0,
                    threshold = GraphThreshold("2", SpendingCategory.TRAVEL, 25.0),
                    percentOfThreshold = 80.0,
                    isOverThreshold = false,
                ),
                CategoryThresholdData(
                    category = "Gas",
                    spending = 180.0,
                    threshold = null,
                    percentOfThreshold = null,
                    isOverThreshold = false,
                ),
            ),
        )
    }
}
