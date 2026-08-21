package com.divafinance.core.ui.component.chart

import androidx.compose.ui.graphics.Color

/*
 * The charts take these rather than domain or feature types, so `core:ui` stays at the
 * bottom of the graph and any screen — graphs, a period report, the feed — can feed the
 * same renderer. Callers map their own model into these and choose the colours, which is
 * what keeps the category encoding consistent across every chart in the app.
 */

/** One wedge of a pie / donut. [fraction] is 0..1 of the whole. */
data class ChartSlice(
    val label: String,
    val value: Double,
    val fraction: Float,
    val color: Color,
)

/** One point on a line or one column of a bar series. */
data class ChartPoint(
    val label: String,
    val value: Double,
    val highlighted: Boolean = false,
)

/** A bar with an optional limit marker across it. [limit] is in the same units as [value]. */
data class ChartBar(
    val label: String,
    val value: Double,
    val color: Color,
    val limit: Double? = null,
    val isOver: Boolean = false,
    /** Drawn above the bar. Null falls back to the rounded value. */
    val valueLabel: String? = null,
)
