package com.divafinance.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.core.domain.usecase.activity.ActivityItem
import com.divafinance.core.domain.usecase.activity.GetActivityUseCase
import com.divafinance.core.domain.usecase.graphs.CategoryThresholdData
import com.divafinance.core.domain.usecase.graphs.GetThresholdGraphDataUseCase
import com.divafinance.core.domain.usecase.reports.ReportPeriod
import com.divafinance.core.domain.usecase.reports.contains
import com.divafinance.core.domain.usecase.reports.end
import com.divafinance.core.domain.usecase.reports.monthName
import com.divafinance.core.domain.usecase.reports.start
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.UserSettings
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

/** One category's slice of the monthly plan. */
data class BudgetRow(
    val category: SpendingCategory,
    val spent: Double,
    /** Null when no monthly plan is set — the row then shows a share, not an amount. */
    val planned: Double?,
    val shareOfSpending: Float,
) {
    val left: Double? get() = planned?.let { it - spent }
    val isOver: Boolean get() = left?.let { it < 0.0 } == true
    val fraction: Float
        get() = planned?.takeIf { it > 0.0 }?.let { (spent / it).toFloat() } ?: shareOfSpending
}

data class YouUiState(
    val displayName: String = "You",
    val location: String? = null,
    val expensesThisYear: Int = 0,
    val placesThisMonth: Int = 0,
    val daysOnPlanStreak: Int = 0,
    val monthLabel: String = "",
    /** Null when no plan is set; the panel then invites the user to set one. */
    val monthlyBudget: Double? = null,
    val spentThisMonth: Double = 0.0,
    val daysLeftInMonth: Int = 0,
    val budgets: List<BudgetRow> = emptyList(),
    val isLoading: Boolean = true,
) {
    val leftToSpend: Double? get() = monthlyBudget?.let { it - spentThisMonth }

    val dailyAllowance: Double?
        get() = leftToSpend?.let { left ->
            if (daysLeftInMonth > 0) (left / daysLeftInMonth).coerceAtLeast(0.0) else null
        }

    val budgetFraction: Float
        get() = monthlyBudget?.takeIf { it > 0.0 }?.let { (spentThisMonth / it).toFloat() } ?: 0f
}

/**
 * The profile: who this is, how the month is tracking, and the budgets behind it.
 *
 * Budgets are the existing [com.divafinance.core.model.GraphThreshold] records read in
 * currency: a threshold is a percentage of spending, so multiplying it by the monthly
 * plan turns "Dining should be 25%" into "$107.15 left" without a new table.
 */
class YouViewModel(
    private val getActivity: GetActivityUseCase,
    private val getThresholdGraphData: GetThresholdGraphDataUseCase,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val thresholds = MutableStateFlow<List<CategoryThresholdData>>(emptyList())

    val uiState: StateFlow<YouUiState> =
        combine(getActivity(), settingsRepository.getAll(), thresholds) { items, settings, limits ->
            buildState(items, settings.associate { it.key to it.value }, limits)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YouUiState())

    init {
        refreshThresholds()
    }

    /**
     * Thresholds come from a suspend use case rather than a flow, so they are pulled
     * rather than observed. Re-pulled whenever the screen is shown; a threshold changing
     * while You is open is rare enough not to warrant a repository-level flow.
     */
    fun refreshThresholds() {
        viewModelScope.launch {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            thresholds.value = getThresholdGraphData(
                ReportPeriod.MONTH.start(today),
                ReportPeriod.MONTH.end(today),
            )
        }
    }

    suspend fun setMonthlyBudget(amount: Double) {
        settingsRepository.set(UserSettings.KEY_MONTHLY_BUDGET, amount.toString())
    }

    suspend fun setDisplayName(name: String) {
        settingsRepository.set(UserSettings.KEY_DISPLAY_NAME, name)
    }

    private fun buildState(
        items: List<ActivityItem>,
        settings: Map<String, String>,
        limits: List<CategoryThresholdData>,
    ): YouUiState {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val expenses = items.filterIsInstance<ActivityItem.Spend>()
            .map { it.transaction }
            .filter { it.type == TransactionType.DEBIT }

        val thisMonth = expenses.filter { ReportPeriod.MONTH.contains(today, it.date) }
        val spentThisMonth = thisMonth.sumOf { it.ownShare }
        val budget = settings[UserSettings.KEY_MONTHLY_BUDGET]?.toDoubleOrNull()

        val monthEnd = ReportPeriod.MONTH.end(today)
        val daysLeft = (monthEnd.toEpochDays() - today.toEpochDays()).toInt()

        val monthTotal = spentThisMonth.takeIf { it > 0.0 }

        return YouUiState(
            displayName = settings[UserSettings.KEY_DISPLAY_NAME]?.takeIf { it.isNotBlank() } ?: "You",
            location = settings[UserSettings.KEY_DEFAULT_LOCATION]?.takeIf { it.isNotBlank() },
            expensesThisYear = expenses.count { ReportPeriod.YEAR.contains(today, it.date) },
            placesThisMonth = thisMonth.mapNotNull { it.location?.name ?: it.merchantName }
                .distinct().size,
            daysOnPlanStreak = streak(expenses, today, budget, monthEnd),
            monthLabel = monthName(today.monthNumber),
            monthlyBudget = budget,
            spentThisMonth = spentThisMonth,
            daysLeftInMonth = daysLeft,
            budgets = limits.map { limit ->
                val category = SpendingCategory.valueOf(limit.category)
                BudgetRow(
                    category = category,
                    spent = limit.spending,
                    planned = budget?.let { it * (limit.threshold?.thresholdPercent ?: 0.0) / 100.0 }
                        ?.takeIf { it > 0.0 },
                    shareOfSpending = monthTotal?.let { (limit.spending / it).toFloat() } ?: 0f,
                )
            }.sortedByDescending { it.spent },
            isLoading = false,
        )
    }

    /**
     * Consecutive days back from today on which spending stayed under the daily
     * allowance. Counting back from today rather than forward from the 1st means the
     * streak breaks the moment a day goes over, which is the behaviour a streak needs.
     */
    private fun streak(
        expenses: List<Transaction>,
        today: LocalDate,
        budget: Double?,
        monthEnd: LocalDate,
    ): Int {
        if (budget == null) return 0
        val daysInMonth = monthEnd.dayOfMonth
        val allowance = budget / daysInMonth
        val byDay = expenses.groupBy { it.date }.mapValues { (_, v) -> v.sumOf { it.ownShare } }

        var streak = 0
        var cursor = today
        // Cap at a year: a longer run is not worth the loop, and the number stops being
        // legible on a profile long before that.
        while (streak < 365) {
            if ((byDay[cursor] ?: 0.0) > allowance) break
            streak++
            cursor = cursor.minus(1, DateTimeUnit.DAY)
        }
        return streak
    }
}

private val Transaction.ownShare: Double get() = amount - othersShare
