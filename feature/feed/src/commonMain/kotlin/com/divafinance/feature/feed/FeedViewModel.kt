package com.divafinance.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divafinance.core.domain.usecase.activity.ActivityItem
import com.divafinance.core.domain.usecase.activity.GetActivityUseCase
import com.divafinance.core.domain.usecase.feed.GenerateDailyInsightUseCase
import com.divafinance.core.domain.usecase.reports.ReportPeriod
import com.divafinance.core.domain.usecase.reports.contains
import com.divafinance.core.domain.usecase.reports.previousAnchor
import com.divafinance.core.model.FeedPost
import com.divafinance.core.model.LedgerEntry
import com.divafinance.core.model.Person
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

/** One tappable summary card: today, this month, this year. */
data class PeriodSummary(
    val period: ReportPeriod,
    val anchor: LocalDate,
    val label: String,
    val total: Double,
    val transactionCount: Int,
    /** Change against the previous comparable period, as a fraction. Null when there is no prior. */
    val deltaFraction: Double?,
)

/** A spend in the ledger, plus whatever makes it worth showing larger. */
data class LedgerItem(
    val transaction: Transaction,
    /** Who else is on this bill, and what they still owe back. */
    val splitWith: List<String> = emptyList(),
    val owedBack: Double = 0.0,
) {
    /** What the user actually consumed, once other people's shares come off. */
    val ownShare: Double get() = transaction.amount - transaction.othersShare

    /**
     * Whether this gets the full-width treatment rather than a list row. Splits earn it
     * because they carry information a row cannot hold — who owes what.
     */
    val isMoment: Boolean get() = splitWith.isNotEmpty()
}

data class DaySection(
    val date: LocalDate,
    val label: String,
    val total: Double,
    val items: List<LedgerItem>,
)

data class WeekBar(val date: LocalDate, val total: Double, val isToday: Boolean)

data class FeedUiState(
    val isLoading: Boolean = true,
    val today: LocalDate = LocalDate(2026, 1, 1),
    val todayTotal: Double = 0.0,
    val todayCount: Int = 0,
    /** Average daily spend across the month so far — the "keeps you on plan" figure. */
    val dailyPace: Double = 0.0,
    /** Today measured against that pace. Positive means over. */
    val paceDelta: Double = 0.0,
    val week: List<WeekBar> = emptyList(),
    val periods: List<PeriodSummary> = emptyList(),
    val days: List<DaySection> = emptyList(),
    val insight: FeedPost? = null,
    val topCategoryToday: SpendingCategory? = null,
    val topMerchantToday: String? = null,
    val topAmountToday: Double = 0.0,
    val isGeneratingInsight: Boolean = false,
) {
    val isEmpty: Boolean get() = !isLoading && days.isEmpty()
}

/**
 * The feed's whole read model.
 *
 * Everything comes from [GetActivityUseCase], which already merges transactions, ledger
 * entries and insight posts into one stream with people resolved. Deriving the summaries
 * from that single flow rather than querying totals separately is what keeps the day
 * headers, the period cards and the ledger from ever disagreeing with each other.
 */
class FeedViewModel(
    private val getActivity: GetActivityUseCase,
    private val generateDailyInsight: GenerateDailyInsightUseCase,
) : ViewModel() {

    private val generating = MutableStateFlow(false)

    val uiState: StateFlow<FeedUiState> =
        combine(getActivity(), generating) { items, isGenerating ->
            buildState(items).copy(isGeneratingInsight = isGenerating)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedUiState())

    init {
        generateInsight()
    }

    fun generateInsight() {
        if (generating.value) return
        viewModelScope.launch {
            generating.value = true
            generateDailyInsight()
            generating.value = false
        }
    }

    private fun buildState(items: List<ActivityItem>): FeedUiState {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val spends = items.filterIsInstance<ActivityItem.Spend>().map { it.transaction }
        val debts = items.filterIsInstance<ActivityItem.Debt>()
        val insight = items.filterIsInstance<ActivityItem.Insight>()
            .maxByOrNull { it.post.createdAt }?.post

        // Debts created by a split all carry the transaction they came from, which is how
        // a ledger row learns who is on the bill.
        val debtsByTransaction: Map<String, List<ActivityItem.Debt>> = debts
            .filter { it.entry.transactionId != null }
            .groupBy { it.entry.transactionId!! }

        val expenses = spends.filter { it.type == TransactionType.DEBIT }

        val ledger = expenses.map { transaction ->
            val related = debtsByTransaction[transaction.id].orEmpty()
            LedgerItem(
                transaction = transaction,
                splitWith = related.map { it.personName }.distinct(),
                owedBack = related.sumOf { it.entry.signedAmount }.coerceAtLeast(0.0),
            )
        }

        val days = ledger
            .groupBy { it.transaction.date }
            .entries
            .sortedByDescending { it.key }
            .map { (date, dayItems) ->
                DaySection(
                    date = date,
                    label = dayLabel(date, today),
                    total = dayItems.sumOf { it.ownShare },
                    items = dayItems.sortedByDescending { it.transaction.createdAt },
                )
            }

        val todayItems = ledger.filter { it.transaction.date == today }
        val todayTotal = todayItems.sumOf { it.ownShare }

        // Pace is the month's own average so far, so it adapts instead of needing a budget.
        val monthItems = ledger.filter { ReportPeriod.MONTH.contains(today, it.transaction.date) }
        val daysElapsed = today.dayOfMonth.coerceAtLeast(1)
        val dailyPace = monthItems.sumOf { it.ownShare } / daysElapsed

        val week = (6 downTo 0).map { back ->
            val date = today.minus(back, DateTimeUnit.DAY)
            WeekBar(
                date = date,
                total = ledger.filter { it.transaction.date == date }.sumOf { it.ownShare },
                isToday = date == today,
            )
        }

        val biggestToday = todayItems.maxByOrNull { it.ownShare }

        return FeedUiState(
            isLoading = false,
            today = today,
            todayTotal = todayTotal,
            todayCount = todayItems.size,
            dailyPace = dailyPace,
            paceDelta = todayTotal - dailyPace,
            week = week,
            periods = ReportPeriod.entries.map { period ->
                summarise(period, today, ledger)
            },
            days = days,
            insight = insight,
            topCategoryToday = biggestToday?.transaction?.category,
            topMerchantToday = biggestToday?.transaction?.merchantName,
            topAmountToday = biggestToday?.ownShare ?: 0.0,
        )
    }

    private fun summarise(
        period: ReportPeriod,
        today: LocalDate,
        ledger: List<LedgerItem>,
    ): PeriodSummary {
        val current = ledger.filter { period.contains(today, it.transaction.date) }
        val previousAnchor = period.previousAnchor(today)
        val previous = ledger.filter { period.contains(previousAnchor, it.transaction.date) }

        val total = current.sumOf { it.ownShare }
        val previousTotal = previous.sumOf { it.ownShare }

        return PeriodSummary(
            period = period,
            anchor = today,
            label = when (period) {
                ReportPeriod.DAY -> "Today"
                ReportPeriod.MONTH -> "This month"
                ReportPeriod.YEAR -> "This year"
            },
            total = total,
            transactionCount = current.size,
            // A delta against zero is not a percentage, so it is left absent rather than
            // rendered as an infinite increase.
            deltaFraction = if (previousTotal > 0.0) (total - previousTotal) / previousTotal else null,
        )
    }

    private fun dayLabel(date: LocalDate, today: LocalDate): String {
        val diff = (today.toEpochDays() - date.toEpochDays()).toInt()
        return when (diff) {
            0 -> "Today"
            1 -> "Yesterday"
            else -> "${date.dayOfMonth} ${com.divafinance.core.domain.usecase.reports.monthName(date.monthNumber)}"
        }
    }
}
