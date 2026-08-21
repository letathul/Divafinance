package com.divafinance.feature.graphs

import androidx.compose.runtime.Composable
import com.divafinance.core.common.toFixed
import com.divafinance.core.domain.usecase.graphs.CategoryThresholdData
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.ui.component.chart.ChartBar
import com.divafinance.core.ui.component.chart.ChartPoint
import com.divafinance.core.ui.component.chart.ChartSlice
import com.divafinance.core.ui.theme.diva

/*
 * The charts live in core:ui and take neutral data, so the category encoding is applied
 * here — one place, so a pie wedge and its bar are the same hue.
 *
 * Note the `category` field on both use-case types carries the enum *name*, not the
 * display string; getSpendingByCategory keys its map that way.
 */

@Composable
fun List<SpendingSlice>.toChartSlices(): List<ChartSlice> = map { slice ->
    val category = SpendingCategory.valueOf(slice.category)
    ChartSlice(
        label = category.displayName,
        value = slice.amount,
        fraction = slice.percent / 100f,
        color = diva.categoryColor(category),
    )
}

@Composable
fun List<CategoryThresholdData>.toChartBars(): List<ChartBar> {
    val total = sumOf { it.spending }.takeIf { it > 0.0 }
    return map { item ->
        val category = SpendingCategory.valueOf(item.category)
        // Bars stay in percent-of-total so a threshold, which is also a percentage, can
        // share the axis.
        val percent = total?.let { item.spending / it * 100.0 } ?: 0.0
        ChartBar(
            label = category.displayName,
            value = percent,
            color = diva.categoryColor(category),
            limit = item.threshold?.thresholdPercent,
            isOver = item.isOverThreshold,
            valueLabel = "${percent.toFixed(0)}%",
        )
    }
}

fun List<TrendPoint>.toChartPoints(): List<ChartPoint> = map {
    ChartPoint(label = it.monthLabel, value = it.amount)
}
