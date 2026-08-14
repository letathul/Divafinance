package com.divafinance.feature.graphs

import com.divafinance.core.model.enums.SpendingCategory
import kotlin.test.Test
import kotlin.test.assertEquals

class GraphsViewModelTest {

    @Test
    fun chartTabEnumValues() {
        assertEquals(3, ChartTab.entries.size)
        assertEquals("Spending", ChartTab.PIE.label)
        assertEquals("Thresholds", ChartTab.BAR.label)
        assertEquals("Trends", ChartTab.LINE.label)
    }

    @Test
    fun timePeriodEnumValues() {
        assertEquals(4, TimePeriod.entries.size)
        assertEquals(1, TimePeriod.ONE_MONTH.months)
        assertEquals(3, TimePeriod.THREE_MONTHS.months)
        assertEquals(6, TimePeriod.SIX_MONTHS.months)
        assertEquals(12, TimePeriod.ONE_YEAR.months)
    }

    @Test
    fun graphsUiStateDefaults() {
        val state = GraphsUiState()
        assertEquals(ChartTab.PIE, state.selectedTab)
        assertEquals(TimePeriod.ONE_MONTH, state.selectedPeriod)
        assertEquals(emptyList(), state.spendingSlices)
        assertEquals(emptyList(), state.thresholdData)
        assertEquals(emptyList(), state.trendData)
        assertEquals(0.0, state.totalSpending)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun spendingSlicePercentCalculation() {
        val slice = SpendingSlice(
            category = "Dining",
            amount = 250.0,
            percent = 50.0f,
        )
        assertEquals("Dining", slice.category)
        assertEquals(250.0, slice.amount)
        assertEquals(50.0f, slice.percent)
    }

    @Test
    fun trendPointData() {
        val point = TrendPoint(monthLabel = "Jan", amount = 1500.0)
        assertEquals("Jan", point.monthLabel)
        assertEquals(1500.0, point.amount)
    }

    @Test
    fun thresholdFormStateDefaults() {
        val form = ThresholdFormState()
        assertEquals(SpendingCategory.DINING, form.category)
        assertEquals("", form.thresholdPercent)
        assertEquals(false, form.isEditing)
        assertEquals(null, form.editingId)
    }

    @Test
    fun thresholdFormStateCopy() {
        val form = ThresholdFormState()
        val updated = form.copy(
            category = SpendingCategory.GROCERIES,
            thresholdPercent = "75",
            isEditing = true,
            editingId = "t1",
        )
        assertEquals(SpendingCategory.GROCERIES, updated.category)
        assertEquals("75", updated.thresholdPercent)
        assertEquals(true, updated.isEditing)
        assertEquals("t1", updated.editingId)
    }
}
