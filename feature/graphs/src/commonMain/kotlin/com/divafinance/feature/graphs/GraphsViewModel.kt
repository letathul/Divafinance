package com.divafinance.feature.graphs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divafinance.core.domain.usecase.graphs.CategoryThresholdData
import com.divafinance.core.domain.usecase.graphs.ConfigureThresholdUseCase
import com.divafinance.core.domain.usecase.graphs.GetThresholdGraphDataUseCase
import com.divafinance.core.domain.usecase.transactions.GetSpendingByCategoryUseCase
import com.divafinance.core.model.GraphThreshold
import com.divafinance.core.model.enums.SpendingCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

enum class ChartTab(val label: String) {
    PIE("Spending"),
    BAR("Thresholds"),
    LINE("Trends"),
}

enum class TimePeriod(val label: String, val months: Int) {
    ONE_MONTH("1M", 1),
    THREE_MONTHS("3M", 3),
    SIX_MONTHS("6M", 6),
    ONE_YEAR("1Y", 12),
}

data class SpendingSlice(
    val category: String,
    val amount: Double,
    val percent: Float,
)

data class TrendPoint(
    val monthLabel: String,
    val amount: Double,
)

data class GraphsUiState(
    val selectedTab: ChartTab = ChartTab.PIE,
    val selectedPeriod: TimePeriod = TimePeriod.ONE_MONTH,
    val spendingSlices: List<SpendingSlice> = emptyList(),
    val thresholdData: List<CategoryThresholdData> = emptyList(),
    val trendData: List<TrendPoint> = emptyList(),
    val totalSpending: Double = 0.0,
    val isLoading: Boolean = false,
)

data class ThresholdFormState(
    val category: SpendingCategory = SpendingCategory.DINING,
    val thresholdPercent: String = "",
    val isEditing: Boolean = false,
    val editingId: String? = null,
)

class GraphsViewModel(
    private val getSpendingByCategoryUseCase: GetSpendingByCategoryUseCase,
    private val getThresholdGraphDataUseCase: GetThresholdGraphDataUseCase,
    private val configureThresholdUseCase: ConfigureThresholdUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GraphsUiState())
    val uiState: StateFlow<GraphsUiState> = _uiState.asStateFlow()

    private val _thresholdForm = MutableStateFlow(ThresholdFormState())
    val thresholdForm: StateFlow<ThresholdFormState> = _thresholdForm.asStateFlow()

    init {
        loadData()
    }

    fun selectTab(tab: ChartTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun selectPeriod(period: TimePeriod) {
        _uiState.value = _uiState.value.copy(selectedPeriod = period)
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val period = _uiState.value.selectedPeriod
            val startDate = today.minus(period.months, DateTimeUnit.MONTH)

            val spending = getSpendingByCategoryUseCase(startDate, today)
            val total = spending.values.sum()

            val slices = if (total > 0) {
                spending.entries
                    .sortedByDescending { it.value }
                    .map { (cat, amount) ->
                        SpendingSlice(
                            category = cat,
                            amount = amount,
                            percent = (amount / total * 100).toFloat(),
                        )
                    }
            } else {
                emptyList()
            }

            val thresholdData = getThresholdGraphDataUseCase(startDate, today)

            val trendData = buildTrendData(period, today)

            _uiState.value = _uiState.value.copy(
                spendingSlices = slices,
                thresholdData = thresholdData,
                trendData = trendData,
                totalSpending = total,
                isLoading = false,
            )
        }
    }

    private suspend fun buildTrendData(
        period: TimePeriod,
        today: kotlinx.datetime.LocalDate,
    ): List<TrendPoint> {
        val points = mutableListOf<TrendPoint>()
        val monthNames = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
        )

        for (i in period.months - 1 downTo 0) {
            val monthStart = today.minus(i, DateTimeUnit.MONTH)
            val adjustedStart = kotlinx.datetime.LocalDate(
                monthStart.year, monthStart.monthNumber, 1
            )
            val nextMonth = adjustedStart.minus(-1, DateTimeUnit.MONTH)
            val monthEnd = nextMonth.minus(1, DateTimeUnit.DAY)

            val spending = getSpendingByCategoryUseCase(adjustedStart, monthEnd)
            val total = spending.values.sum()

            points.add(
                TrendPoint(
                    monthLabel = monthNames[adjustedStart.monthNumber - 1],
                    amount = total,
                )
            )
        }
        return points
    }

    fun updateThresholdCategory(category: SpendingCategory) {
        _thresholdForm.value = _thresholdForm.value.copy(category = category)
    }

    fun updateThresholdPercent(percent: String) {
        _thresholdForm.value = _thresholdForm.value.copy(thresholdPercent = percent)
    }

    fun startEditingThreshold(data: CategoryThresholdData) {
        val threshold = data.threshold ?: return
        _thresholdForm.value = ThresholdFormState(
            category = SpendingCategory.valueOf(data.category),
            thresholdPercent = threshold.thresholdPercent.toString(),
            isEditing = true,
            editingId = threshold.id,
        )
    }

    fun resetThresholdForm() {
        _thresholdForm.value = ThresholdFormState()
    }

    fun saveThreshold(onSuccess: () -> Unit = {}) {
        val form = _thresholdForm.value
        val percent = form.thresholdPercent.toDoubleOrNull() ?: return

        viewModelScope.launch {
            val threshold = GraphThreshold(
                id = form.editingId ?: "",
                category = form.category,
                thresholdPercent = percent,
                isActive = true,
            )
            configureThresholdUseCase.upsert(threshold)
            resetThresholdForm()
            loadData()
            onSuccess()
        }
    }

    fun deleteThreshold(id: String) {
        viewModelScope.launch {
            configureThresholdUseCase.delete(id)
            loadData()
        }
    }
}
