package com.divafinance.feature.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divafinance.core.domain.usecase.activity.ActivityFilter
import com.divafinance.core.domain.usecase.activity.ActivityItem
import com.divafinance.core.domain.usecase.activity.ActivityKind
import com.divafinance.core.domain.usecase.activity.GetActivityUseCase
import com.divafinance.core.domain.usecase.people.GetPeopleBalancesUseCase
import com.divafinance.core.domain.usecase.people.PersonBalance
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Preset ranges, so the common cases need no date picker. */
enum class ActivityPeriod(val displayName: String) {
    ALL("All time"),
    THIS_MONTH("This month"),
    LAST_3_MONTHS("Last 3 months"),
    THIS_YEAR("This year"),
}

/**
 * What the user has narrowed the stream to.
 *
 * Deliberately separate from [ActivityUiState]: this is what the query is rebuilt from, so
 * it must not contain results. Folding the loaded items in here would make every emission
 * re-trigger the query that produced it — an endless loop.
 */
data class ActivityFilterState(
    val selectedKinds: Set<ActivityKind> = ActivityKind.entries.toSet(),
    val period: ActivityPeriod = ActivityPeriod.ALL,
    val query: String = "",
) {
    val isFiltered: Boolean
        get() = selectedKinds.size != ActivityKind.entries.size ||
            period != ActivityPeriod.ALL ||
            query.isNotBlank()
}

data class ActivityUiState(
    val items: List<ActivityItem> = emptyList(),
    val balances: List<PersonBalance> = emptyList(),
    val filter: ActivityFilterState = ActivityFilterState(),
) {
    val isFiltered: Boolean get() = filter.isFiltered
    val selectedKinds: Set<ActivityKind> get() = filter.selectedKinds
    val period: ActivityPeriod get() = filter.period
    val query: String get() = filter.query

    /** Only people with something outstanding are worth surfacing. */
    val openBalances: List<PersonBalance> get() = balances.filterNot { it.isSettled }

    val totalOwedToMe: Double get() = openBalances.filter { it.theyOweMe }.sumOf { it.balance }
    val totalIOwe: Double get() = -openBalances.filter { it.iOweThem }.sumOf { it.balance }
}

/**
 * Backs the single Activity tab, which merges spending, debts and insights into one
 * stream rather than splitting them across three tabs.
 *
 * The filter drives the query rather than being applied to an already-loaded list, so the
 * stream is always consistent with what the repositories hold.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ActivityViewModel(
    getActivityUseCase: GetActivityUseCase,
    getPeopleBalancesUseCase: GetPeopleBalancesUseCase,
) : ViewModel() {

    private val filter = MutableStateFlow(ActivityFilterState())

    val uiState: StateFlow<ActivityUiState> = combine(
        filter,
        filter.flatMapLatest { getActivityUseCase(it.toFilter()) },
        getPeopleBalancesUseCase(),
    ) { current, items, balances ->
        ActivityUiState(items = items, balances = balances, filter = current)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ActivityUiState())

    fun onToggleKind(kind: ActivityKind) = filter.update { state ->
        val next = if (kind in state.selectedKinds) {
            state.selectedKinds - kind
        } else {
            state.selectedKinds + kind
        }
        // Never leave every kind off — an empty stream reads as a bug, not a filter.
        state.copy(selectedKinds = next.ifEmpty { ActivityKind.entries.toSet() })
    }

    fun onPeriodChange(period: ActivityPeriod) = filter.update { it.copy(period = period) }

    fun onQueryChange(query: String) = filter.update { it.copy(query = query) }

    fun onClearFilters() = filter.update { ActivityFilterState() }
}

/** Resolves the preset period to concrete dates for the domain filter. */
internal fun ActivityFilterState.toFilter(
    today: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
): ActivityFilter = ActivityFilter(
    kinds = selectedKinds,
    from = when (period) {
        ActivityPeriod.ALL -> null
        ActivityPeriod.THIS_MONTH -> LocalDate(today.year, today.month, 1)
        ActivityPeriod.LAST_3_MONTHS -> LocalDate.fromEpochDays(today.toEpochDays() - 90)
        ActivityPeriod.THIS_YEAR -> LocalDate(today.year, 1, 1)
    },
    to = null,
    query = query,
)
