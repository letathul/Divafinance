package com.divafinance.feature.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.divafinance.core.common.toFixed
import com.divafinance.core.domain.usecase.reports.ReportPeriod
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.component.DayHeader
import com.divafinance.core.ui.component.GlassSurface
import com.divafinance.core.ui.component.Meta
import com.divafinance.core.ui.component.MomentCard
import com.divafinance.core.ui.component.StatPill
import com.divafinance.core.ui.component.TransactionRow
import com.divafinance.core.ui.component.color
import com.divafinance.core.ui.theme.NumericStyle
import com.divafinance.core.ui.theme.Pill
import com.divafinance.core.ui.theme.Space
import com.divafinance.core.ui.theme.diva
import com.divafinance.core.ui.util.formatCurrency
import com.divafinance.feature.feed.component.BotInsightBubble
import kotlinx.datetime.LocalDate

/**
 * The one screen that carries almost everything: what today cost, how the month is
 * tracking, and every spend in reverse-chronological order with the notable ones given
 * the full width.
 */
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onOpenReport: (ReportPeriod, LocalDate) -> Unit = { _, _ -> },
    onOpenTransaction: (String) -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenInsights: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(bottom = 120.dp),
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = contentPadding,
    ) {
        item {
            FeedAppBar(
                onSearch = onOpenSearch,
                onInsights = onOpenInsights,
                onProfile = onOpenProfile,
            )
        }

        item {
            TodayCard(
                state = state,
                modifier = Modifier.padding(horizontal = Space.pad),
                onClick = { onOpenReport(ReportPeriod.DAY, state.today) },
            )
        }

        item {
            PeriodRow(
                periods = state.periods,
                onOpen = onOpenReport,
            )
        }

        state.insight?.let { insight ->
            item(key = "insight:${insight.id}") {
                BotInsightBubble(
                    post = insight,
                    modifier = Modifier.padding(horizontal = Space.pad, vertical = Space.xs),
                )
            }
        }

        if (state.isEmpty) {
            item { EmptyFeed() }
        }

        state.days.forEach { day ->
            item(key = "header:${day.date}") {
                DayHeader(label = day.label, total = day.total)
            }
            items(day.items, key = { it.transaction.id }) { item ->
                if (item.isMoment) {
                    MomentCard(
                        title = item.transaction.merchantName
                            ?: item.transaction.category.displayName,
                        subtitle = listOfNotNull(
                            item.transaction.location?.name,
                            item.transaction.category.displayName,
                        ).joinToString(" · "),
                        amount = formatCurrency(item.ownShare, item.transaction.currency),
                        caption = "your share of a " +
                            "${formatCurrency(item.transaction.amount, item.transaction.currency)} bill",
                        category = item.transaction.category,
                        badge = "Split ${item.splitWith.size + 1} ways",
                        splitWith = item.splitWith,
                        footnote = owedFootnote(item),
                        modifier = Modifier.padding(horizontal = Space.pad, vertical = Space.md),
                        onClick = { onOpenTransaction(item.transaction.id) },
                    )
                } else {
                    TransactionRow(
                        transaction = item.transaction,
                        onClick = { onOpenTransaction(item.transaction.id) },
                    )
                }
            }
        }
    }
}

private fun owedFootnote(item: LedgerItem): String? {
    if (item.owedBack <= 0.0) return null
    val names = when (item.splitWith.size) {
        0 -> return null
        1 -> item.splitWith[0]
        2 -> "${item.splitWith[0]} and ${item.splitWith[1]}"
        else -> "${item.splitWith[0]} and ${item.splitWith.size - 1} others"
    }
    return "$names owe you ${formatCurrency(item.owedBack, item.transaction.currency)}"
}

@Composable
private fun FeedAppBar(
    onSearch: () -> Unit,
    onInsights: () -> Unit,
    onProfile: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.pad, vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "diva",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            GlassIconButton(Icons.Outlined.Search, "Search transactions", onSearch)
            GlassIconButton(Icons.Outlined.Tune, "Filter the feed", onInsights)
            GlassIconButton(Icons.Outlined.AutoAwesome, "Your profile", onProfile)
        }
    }
}

@Composable
internal fun GlassIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
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
private fun TodayCard(state: FeedUiState, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val over = state.paceDelta > 0
    DivaCard(modifier, onClick = onClick) {
        Column(Modifier.padding(Space.pad), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Meta("Today · ${state.today.dayOfMonth} ${monthShort(state.today)}")
                if (state.dailyPace > 0.0) {
                    StatPill(
                        text = "${formatCurrency(state.paceDelta)} ${if (over) "over" else "under"} pace",
                        tint = if (over) diva.negative else diva.positive,
                        leading = {
                            Icon(
                                if (over) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (over) diva.negative else diva.positive,
                            )
                        },
                    )
                }
            }

            Text(formatCurrency(state.todayTotal), style = MaterialTheme.typography.displayMedium)

            Text(
                buildString {
                    append(state.todayCount)
                    append(if (state.todayCount == 1) " expense" else " expenses")
                    if (state.dailyPace > 0.0) {
                        append(" · ").append(formatCurrency(state.dailyPace)).append(" a day so far")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = diva.muted,
            )

            WeekSparkline(state.week)

            if (state.topMerchantToday != null) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    Text(
                        "${state.topCategoryToday?.displayName ?: "Spending"} led today — " +
                            "${state.topMerchantToday} at ${formatCurrency(state.topAmountToday)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = diva.muted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/**
 * Seven days at a glance. Bars are scaled against the week's own peak rather than a fixed
 * ceiling, so a quiet week still shows shape instead of seven flat stubs.
 */
@Composable
private fun WeekSparkline(week: List<WeekBar>) {
    val peak = week.maxOfOrNull { it.total }?.takeIf { it > 0.0 } ?: return
    Row(
        Modifier.fillMaxWidth().height(34.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        week.forEach { bar ->
            val fraction = (bar.total / peak).toFloat().coerceIn(0.08f, 1f)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (bar.isToday) MaterialTheme.colorScheme.onSurface else diva.fgHair
                    )
            )
        }
    }
}

@Composable
private fun PeriodRow(
    periods: List<PeriodSummary>,
    onOpen: (ReportPeriod, LocalDate) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Space.pad, vertical = Space.md),
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        periods.forEach { summary ->
            PeriodCard(summary) { onOpen(summary.period, summary.anchor) }
        }
    }
}

@Composable
private fun PeriodCard(summary: PeriodSummary, onClick: () -> Unit) {
    DivaCard(
        modifier = Modifier.width(168.dp),
        shape = MaterialTheme.shapes.medium,
        onClick = onClick,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Meta(summary.label)
            Text(
                formatCurrency(summary.total),
                style = NumericStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize),
            )
            val delta = summary.deltaFraction
            Text(
                if (delta == null) {
                    "${summary.transactionCount} entries"
                } else {
                    val pct = (delta * 100).toFixed(1)
                    "${if (delta >= 0) "+" else ""}$pct% · ${summary.transactionCount} entries"
                },
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    delta == null -> diva.muted
                    delta > 0 -> diva.negative
                    else -> diva.positive
                },
            )
        }
    }
}

@Composable
private fun EmptyFeed() {
    Column(
        Modifier.fillMaxWidth().padding(Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        Text("Nothing logged yet", style = MaterialTheme.typography.titleMedium)
        Text(
            "Tap the button below to record your first expense.",
            style = MaterialTheme.typography.bodySmall,
            color = diva.muted,
        )
    }
}

private val MONTHS_SHORT = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

private fun monthShort(date: LocalDate): String = MONTHS_SHORT[date.monthNumber - 1]
