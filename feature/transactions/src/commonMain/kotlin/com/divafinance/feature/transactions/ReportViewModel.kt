package com.divafinance.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divafinance.core.domain.usecase.activity.ActivityItem
import com.divafinance.core.domain.usecase.activity.GetActivityUseCase
import com.divafinance.core.domain.usecase.cards.GetAllCardsUseCase
import com.divafinance.core.domain.usecase.reports.ReportPeriod
import com.divafinance.core.domain.usecase.reports.contains
import com.divafinance.core.domain.usecase.reports.end
import com.divafinance.core.domain.usecase.reports.monthAbbreviation
import com.divafinance.core.domain.usecase.reports.previousAnchor
import com.divafinance.core.domain.usecase.reports.previousLabel
import com.divafinance.core.domain.usecase.reports.start
import com.divafinance.core.domain.usecase.reports.title
import com.divafinance.core.model.CreditCard
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * Every attribute a report can be narrowed by.
 *
 * Set-valued rather than single-valued throughout, with **empty meaning "no restriction"**
 * — so "Dining and Travel" is expressible, and clearing a filter is the same operation as
 * never setting it. The tri-state `Boolean?` flags read as ignore / must / must-not.
 */
data class ReportFilter(
    val categories: Set<SpendingCategory> = emptySet(),
    val cardIds: Set<String> = emptySet(),
    val types: Set<TransactionType> = emptySet(),
    val merchants: Set<String> = emptySet(),
    val personIds: Set<String> = emptySet(),
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val isRecurring: Boolean? = null,
    val isSplit: Boolean? = null,
    val hasReceipt: Boolean? = null,
    val hasLocation: Boolean? = null,
    val query: String = "",
) {
    val activeCount: Int
        get() = listOf(
            categories.isNotEmpty(), cardIds.isNotEmpty(), types.isNotEmpty(),
            merchants.isNotEmpty(), personIds.isNotEmpty(),
            minAmount != null, maxAmount != null,
            isRecurring != null, isSplit != null, hasReceipt != null, hasLocation != null,
            query.isNotBlank(),
        ).count { it }

    val isActive: Boolean get() = activeCount > 0
}

/** One removable chip under the report header. */
data class ActiveFilterChip(val label: String, val clear: (ReportFilter) -> ReportFilter)

data class CategoryTotal(
    val category: SpendingCategory,
    val total: Double,
    val fraction: Float,
)

data class ReportUiState(
    val period: ReportPeriod = ReportPeriod.MONTH,
    val anchor: LocalDate = LocalDate(2026, 1, 1),
    val title: String = "",
    val total: Double = 0.0,
    val previousTotal: Double = 0.0,
    val previousLabel: String = "",
    val transactionCount: Int = 0,
    val breakdown: List<CategoryTotal> = emptyList(),
    /** The chart series for this period: hourly-free, so day = per category, else per unit of time. */
    val series: List<Pair<String, Double>> = emptyList(),
    val days: List<ReportDay> = emptyList(),
    val filter: ReportFilter = ReportFilter(),
    val availableCards: List<CreditCard> = emptyList(),
    val availableMerchants: List<String> = emptyList(),
    val availablePeople: List<Pair<String, String>> = emptyList(),
    val isLoading: Boolean = true,
) {
    val deltaFraction: Double?
        get() = if (previousTotal > 0.0) (total - previousTotal) / previousTotal else null
}

data class ReportDay(
    val date: LocalDate,
    val label: String,
    val total: Double,
    val transactions: List<Transaction>,
)

/**
 * A period's spend, filterable by every attribute a transaction carries.
 *
 * Reads through [GetActivityUseCase] rather than the transaction repository directly,
 * because the person filter needs the ledger entries a split created — those are what
 * connect a transaction to the people on its bill.
 */
class ReportViewModel(
    period: ReportPeriod,
    anchor: LocalDate,
    private val getActivity: GetActivityUseCase,
    getAllCards: GetAllCardsUseCase,
) : ViewModel() {

    private val filter = MutableStateFlow(ReportFilter())

    val uiState: StateFlow<ReportUiState> =
        combine(getActivity(), getAllCards(), filter) { items, cards, currentFilter ->
            buildState(period, anchor, items, cards, currentFilter)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ReportUiState(period = period, anchor = anchor, title = period.title(anchor)),
        )

    fun updateFilter(transform: (ReportFilter) -> ReportFilter) {
        filter.update(transform)
    }

    fun clearFilters() {
        filter.value = ReportFilter()
    }

    private fun buildState(
        period: ReportPeriod,
        anchor: LocalDate,
        items: List<ActivityItem>,
        cards: List<CreditCard>,
        currentFilter: ReportFilter,
    ): ReportUiState {
        val transactions = items.filterIsInstance<ActivityItem.Spend>().map { it.transaction }

        // transactionId -> the people a split put on that bill
        val peopleByTransaction: Map<String, Set<String>> = items
            .filterIsInstance<ActivityItem.Debt>()
            .mapNotNull { debt -> debt.entry.transactionId?.let { it to debt.entry.personId } }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.toSet() }

        val everyone = items.filterIsInstance<ActivityItem.Debt>()
            .mapNotNull { debt -> debt.person?.let { it.id to it.name } }
            .distinct()

        val inPeriod = transactions.filter { period.contains(anchor, it.date) }
        val matching = inPeriod.filter {
            it.matches(currentFilter, peopleByTransaction[it.id].orEmpty())
        }

        val previous = period.previousAnchor(anchor)
        val previousMatching = transactions
            .filter { period.contains(previous, it.date) }
            .filter { it.matches(currentFilter, peopleByTransaction[it.id].orEmpty()) }

        val total = matching.sumOf { it.ownShare }
        val breakdownTotal = total.takeIf { it > 0.0 }

        val breakdown = matching
            .groupBy { it.category }
            .map { (category, group) ->
                val subtotal = group.sumOf { it.ownShare }
                CategoryTotal(
                    category = category,
                    total = subtotal,
                    fraction = breakdownTotal?.let { (subtotal / it).toFloat() } ?: 0f,
                )
            }
            .sortedByDescending { it.total }

        return ReportUiState(
            period = period,
            anchor = anchor,
            title = period.title(anchor),
            total = total,
            previousTotal = previousMatching.sumOf { it.ownShare },
            previousLabel = period.previousLabel(anchor),
            transactionCount = matching.size,
            breakdown = breakdown,
            series = buildSeries(period, anchor, matching),
            days = matching
                .groupBy { it.date }
                .entries
                .sortedByDescending { it.key }
                .map { (date, group) ->
                    ReportDay(
                        date = date,
                        label = "${date.dayOfMonth} ${monthAbbreviation(date.monthNumber)}",
                        total = group.sumOf { it.ownShare },
                        transactions = group.sortedByDescending { it.createdAt },
                    )
                },
            filter = currentFilter,
            availableCards = cards,
            availableMerchants = inPeriod.mapNotNull { it.merchantName }.distinct().sorted(),
            availablePeople = everyone,
            isLoading = false,
        )
    }

    /**
     * The chart series, shaped to the window: a day breaks down by category (there are no
     * timestamps on a transaction, only a date, so an hourly axis would be a fiction), a
     * month by day, a year by month.
     */
    private fun buildSeries(
        period: ReportPeriod,
        anchor: LocalDate,
        transactions: List<Transaction>,
    ): List<Pair<String, Double>> = when (period) {
        ReportPeriod.DAY -> transactions
            .groupBy { it.category }
            .map { (category, group) -> category.displayName to group.sumOf { it.ownShare } }
            .sortedByDescending { it.second }

        ReportPeriod.MONTH -> {
            val first = period.start(anchor)
            val last = period.end(anchor)
            val dayCount = (last.toEpochDays() - first.toEpochDays()).toInt() + 1
            (0 until dayCount).map { offset ->
                val date = first.plus(offset, DateTimeUnit.DAY)
                date.dayOfMonth.toString() to
                    transactions.filter { it.date == date }.sumOf { it.ownShare }
            }
        }

        ReportPeriod.YEAR -> (1..12).map { month ->
            monthAbbreviation(month) to
                transactions.filter { it.date.monthNumber == month }.sumOf { it.ownShare }
        }
    }
}

/** What the user consumed, once other people's shares come off a split. */
private val Transaction.ownShare: Double get() = amount - othersShare

private fun Transaction.matches(filter: ReportFilter, people: Set<String>): Boolean {
    if (filter.categories.isNotEmpty() && category !in filter.categories) return false
    // A report is a *spending* report: with no explicit type chosen it means expenses,
    // matching the feed. Without this the header reads "Total spent" over a figure that
    // includes income, and the same month disagrees between the two screens.
    val effectiveTypes = filter.types.ifEmpty { setOf(TransactionType.DEBIT) }
    if (type !in effectiveTypes) return false
    if (filter.cardIds.isNotEmpty() && cardId !in filter.cardIds) return false
    if (filter.merchants.isNotEmpty() && merchantName !in filter.merchants) return false
    if (filter.personIds.isNotEmpty() && filter.personIds.none { it in people }) return false

    filter.minAmount?.let { if (amount < it) return false }
    filter.maxAmount?.let { if (amount > it) return false }

    filter.isRecurring?.let { if (isRecurring != it) return false }
    filter.isSplit?.let { if ((othersShare > 0.0) != it) return false }
    filter.hasReceipt?.let { if ((receiptId != null) != it) return false }
    filter.hasLocation?.let { if ((location != null) != it) return false }

    val query = filter.query.trim().lowercase()
    if (query.isNotEmpty()) {
        val haystack = listOfNotNull(
            merchantName, note, category.displayName, location?.name,
        )
        if (haystack.none { it.lowercase().contains(query) }) return false
    }
    return true
}
