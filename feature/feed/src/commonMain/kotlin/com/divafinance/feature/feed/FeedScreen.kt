package com.divafinance.feature.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.automirrored.outlined.ShowChart
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
import androidx.compose.ui.unit.dp
import com.divafinance.core.common.toFixed
import com.divafinance.core.domain.usecase.reports.ReportPeriod
import com.divafinance.core.ui.adaptive.DivaGroupHeader
import com.divafinance.core.ui.adaptive.DivaGroupedSection
import com.divafinance.core.ui.adaptive.DivaListScaffold
import com.divafinance.core.ui.adaptive.DivaRowDivider
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.component.DivaLogo
import com.divafinance.core.ui.component.DivaLogoSize
import com.divafinance.core.ui.component.GlassSurface
import com.divafinance.core.ui.component.Meta
import com.divafinance.core.ui.component.MomentCard
import com.divafinance.core.ui.component.StatPill
import com.divafinance.core.ui.component.TransactionRow
import com.divafinance.core.ui.component.color
import com.divafinance.core.ui.theme.NumericStyle
import com.divafinance.core.ui.theme.Space
import com.divafinance.core.ui.theme.diva
import com.divafinance.core.ui.theme.isCupertino
import com.divafinance.core.ui.util.formatCurrency
import com.divafinance.feature.feed.component.BotInsightBubble
import com.divafinance.feature.feed.component.StoryRing
import com.divafinance.feature.feed.component.StoryRingRow
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
    contentPadding: PaddingValues = com.divafinance.core.ui.adaptive.divaContentPadding(),
) {
    val state by viewModel.uiState.collectAsState()

    DivaListScaffold(
        title = "Feed",
        // The wordmark stands in for the title: on Cupertino it is the 34sp large title
        // that scrolls away, on Material it sits in the app bar.
        titleContent = {
            DivaLogo(size = if (isCupertino) DivaLogoSize.Large else DivaLogoSize.Medium)
        },
        actions = { FeedActions(onOpenSearch, onOpenInsights, onOpenProfile) },
        contentPadding = contentPadding,
    ) {
        item(key = "rings") {
            StoryRingRow(
                rings = state.rings(),
                onClick = { ring ->
                    when (ring.id) {
                        RING_TRENDS -> onOpenInsights()
                        else -> onOpenReport(ReportPeriod.DAY, state.today)
                    }
                },
            )
        }

        item(key = "today") {
            TodayCard(
                state = state,
                modifier = Modifier.padding(horizontal = Space.pad, vertical = Space.sm),
                onClick = { onOpenReport(ReportPeriod.DAY, state.today) },
            )
        }

        item(key = "periods") {
            PeriodRow(periods = state.periods, onOpen = onOpenReport)
        }

        state.insight?.let { insight ->
            item(key = "insight:${insight.id}") {
                BotInsightBubble(
                    post = insight,
                    modifier = Modifier.padding(horizontal = Space.pad, vertical = Space.xs),
                    scope = state.insightScope(),
                )
            }
        }

        if (state.isEmpty) {
            item(key = "empty") { EmptyFeed() }
        }

        state.days.forEach { day ->
            item(key = "header:${day.date}") {
                DivaGroupHeader(
                    text = day.label,
                    trailing = formatCurrency(day.total),
                )
            }
            // One inset card per day, rather than a row per day with a rule under the
            // last one: the card's own edge is the group's boundary.
            item(key = "day:${day.date}") {
                DivaGroupedSection {
                    day.items.forEachIndexed { index, item ->
                        if (index > 0) DivaRowDivider(startInset = 69.dp)
                        LedgerEntry(item, onOpenTransaction)
                    }
                }
            }
        }
    }
}

/**
 * A split carries information a single line cannot hold — who owes what — so it gets the
 * full width instead of a row.
 */
@Composable
private fun LedgerEntry(item: LedgerItem, onOpenTransaction: (String) -> Unit) {
    if (item.isMoment) {
        MomentCard(
            title = item.transaction.merchantName ?: item.transaction.category.displayName,
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
            modifier = Modifier.padding(Space.sm),
            onClick = { onOpenTransaction(item.transaction.id) },
        )
    } else {
        TransactionRow(
            transaction = item.transaction,
            onClick = { onOpenTransaction(item.transaction.id) },
        )
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

private const val RING_TODAY = "today"
private const val RING_TRENDS = "trends"

/**
 * The rings read off figures the feed already computes — no new ViewModel state.
 *
 * They deliberately do **not** stand in for the period cards below: a ring holds one
 * glanceable number, a card holds a total, a delta and a count, and dropping the cards
 * for the rings would lose two of the three.
 */
@Composable
private fun FeedUiState.rings(): List<StoryRing> = buildList {
    add(
        StoryRing(
            id = RING_TODAY,
            caption = "Today",
            tint = diva.accent,
            icon = Icons.Outlined.Bolt,
        )
    )
    if (topCategoryToday != null) {
        val share = if (todayTotal > 0) (topAmountToday / todayTotal * 100).toFixed(0) else "0"
        add(
            StoryRing(
                id = "category",
                caption = topCategoryToday.displayName,
                tint = topCategoryToday.color,
                value = "$share%",
            )
        )
    }
    add(
        StoryRing(
            id = RING_TRENDS,
            caption = "Trends",
            tint = diva.muted,
            icon = Icons.AutoMirrored.Outlined.ShowChart,
            ringed = false,
        )
    )
}

/** What the insight was computed over — the question a reader asks of any such figure. */
private fun FeedUiState.insightScope(): List<String> = buildList {
    add("This week")
    add("All accounts")
    add(topCategoryToday?.displayName ?: "All categories")
}

@Composable
private fun RowScope.FeedActions(
    onSearch: () -> Unit,
    onInsights: () -> Unit,
    onProfile: () -> Unit,
) {
    GlassIconButton(Icons.Outlined.Search, "Search transactions", onSearch)
    GlassIconButton(Icons.Outlined.Tune, "Filter the feed", onInsights)
    GlassIconButton(Icons.Outlined.AutoAwesome, "Your profile", onProfile)
}

@Composable
internal fun GlassIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    if (isCupertino) {
        // A HIG bar button is a bare tinted glyph, not a chip.
        Icon(
            icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp).clickable(onClick = onClick),
        )
    } else {
        GlassSurface(Modifier.size(40.dp).clickable(onClick = onClick)) {
            Icon(
                icon,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Center).size(20.dp),
            )
        }
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
    if (periods.isEmpty()) return
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Space.pad, vertical = Space.sm),
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        periods.forEach { summary ->
            PeriodCard(summary) { onOpen(summary.period, summary.anchor) }
        }
    }
}

@Composable
private fun PeriodCard(summary: PeriodSummary, onClick: () -> Unit) {
    DivaCard(modifier = Modifier.width(168.dp), onClick = onClick) {
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
