package com.divafinance.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.divafinance.core.common.toFixed
import com.divafinance.core.domain.usecase.reports.ReportPeriod
import com.divafinance.core.ui.adaptive.DivaListScaffold
import com.divafinance.core.ui.component.BudgetTrack
import com.divafinance.core.ui.component.CategoryTile
import com.divafinance.core.ui.component.DayHeader
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.component.GlassSurface
import com.divafinance.core.ui.component.Meta
import com.divafinance.core.ui.component.SectionHeader
import com.divafinance.core.ui.component.StatPill
import com.divafinance.core.ui.component.TransactionRow
import com.divafinance.core.ui.component.chart.ChartPoint
import com.divafinance.core.ui.component.chart.TrendLineChart
import com.divafinance.core.ui.component.color
import com.divafinance.core.ui.theme.NumericStyle
import com.divafinance.core.ui.theme.Space
import com.divafinance.core.ui.theme.diva
import com.divafinance.core.ui.util.formatCurrency

/**
 * One period's spend, opened from a card on the feed.
 *
 * The header leads with the figure and how it compares, then the shape of the period,
 * then where the money went — so the answer to "was this a lot?" arrives before the list
 * of individual rows that cannot answer it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel,
    onBack: () -> Unit = {},
    onOpenTransaction: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    DivaListScaffold(
        title = state.title,
        onBack = onBack,
        actions = {
            Box {
                IconCapsule(
                    icon = Icons.Outlined.Tune,
                    description = "Filter this report",
                    onClick = { showFilters = true },
                )
                // A filtered report reads the same as an unfiltered one otherwise, and
                // mistaking one for the other misreads every figure below.
                if (state.filter.isActive) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(9.dp)
                            .clip(RoundedCornerShape(50))
                            .background(diva.accent)
                    )
                }
            }
        },
        contentPadding = PaddingValues(bottom = 48.dp),
    ) {
        item { ReportHeader(state) }

        if (state.filter.isActive) {
            item {
                ActiveFilterRow(
                    state = state,
                    onUpdate = viewModel::updateFilter,
                    onClearAll = viewModel::clearFilters,
                )
            }
        }

        if (state.series.isNotEmpty() && state.total > 0.0) {
            item {
                Column {
                    SectionHeader(
                        title = when (state.period) {
                            ReportPeriod.DAY -> "By category"
                            ReportPeriod.MONTH -> "Day by day"
                            ReportPeriod.YEAR -> "Month by month"
                        },
                    )
                    PeriodChart(state)
                }
            }
        }

        if (state.breakdown.isNotEmpty()) {
            item { SectionHeader("Where it went", trailing = "${state.breakdown.size} categories") }
            items(state.breakdown, key = { it.category.name }) { row ->
                BreakdownRow(row, state.total)
            }
        }

        item {
            SectionHeader(
                title = "Transactions",
                trailing = "${state.transactionCount} in this period",
            )
        }

        if (state.days.isEmpty() && !state.isLoading) {
            item {
                Text(
                    if (state.filter.isActive) {
                        "Nothing matches these filters."
                    } else {
                        "Nothing recorded in this period."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = diva.muted,
                    modifier = Modifier.padding(Space.pad),
                )
            }
        }

        state.days.forEach { day ->
            item(key = "day:${day.date}") { DayHeader(label = day.label, total = day.total) }
            items(day.transactions, key = { it.id }) { transaction ->
                TransactionRow(
                    transaction = transaction,
                    onClick = { onOpenTransaction(transaction.id) },
                )
            }
        }
    }

    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            ReportFilterSheet(
                state = state,
                onUpdate = viewModel::updateFilter,
                onClearAll = viewModel::clearFilters,
            )
        }
    }
}

@Composable
private fun IconCapsule(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    GlassSurface(Modifier.size(40.dp).clickable(onClick = onClick)) {
        Icon(
            icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Center).size(20.dp),
        )
    }
}

@Composable
private fun ReportHeader(state: ReportUiState) {
    DivaCard(Modifier.padding(horizontal = Space.pad)) {
        Column(Modifier.padding(Space.pad), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Meta("Total spent")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.md),
            ) {
                Text(
                    formatCurrency(state.total),
                    style = MaterialTheme.typography.displaySmall,
                )
                val delta = state.deltaFraction
                if (delta != null) {
                    StatPill(
                        text = "${if (delta >= 0) "+" else ""}${(delta * 100).toFixed(1)}% " +
                            "vs ${state.previousLabel}",
                        tint = if (delta > 0) diva.negative else diva.positive,
                    )
                }
            }
            Text(
                "${state.transactionCount} transactions",
                style = MaterialTheme.typography.bodySmall,
                color = diva.muted,
            )
        }
    }
}

/**
 * A day report has no time axis to plot — transactions carry a date, not a timestamp — so
 * it renders its categories as bars instead of pretending to a chronology it lacks.
 */
@Composable
private fun PeriodChart(state: ReportUiState) {
    when (state.period) {
        ReportPeriod.YEAR -> TrendLineChart(
            data = state.series.map { ChartPoint(it.first, it.second) },
            modifier = Modifier.padding(horizontal = Space.sm),
        )
        else -> MiniBars(state.series)
    }
}

@Composable
private fun MiniBars(series: List<Pair<String, Double>>) {
    val peak = series.maxOfOrNull { it.second }?.takeIf { it > 0.0 } ?: return
    Row(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Space.pad),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        series.forEach { (label, value) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.fillMaxHeight(),
            ) {
                Box(
                    Modifier
                        .width(if (series.size > 15) 8.dp else 22.dp)
                        .fillMaxHeight((value / peak).toFloat().coerceIn(0.02f, 1f))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            if (value > 0) MaterialTheme.colorScheme.onSurface else diva.fgHair
                        )
                )
                Spacer(Modifier.height(4.dp))
                if (series.size <= 15) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = diva.muted)
                }
            }
        }
    }
}

@Composable
private fun BreakdownRow(row: CategoryTotal, total: Double) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Space.pad, vertical = Space.sm),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            CategoryTile(row.category, size = 34.dp)
            Text(
                row.category.displayName,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Text(formatCurrency(row.total), style = NumericStyle)
        }
        BudgetTrack(fraction = row.fraction, color = row.category.color, height = 6.dp)
    }
}
