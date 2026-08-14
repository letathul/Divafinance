package com.divafinance.feature.graphs

import kotlin.test.Test
import kotlin.test.assertEquals

class GraphsDashboardScreenTest {

    @Test
    fun chartTabEntries() {
        assertEquals(3, ChartTab.entries.size)
        assertEquals(ChartTab.PIE, ChartTab.entries[0])
        assertEquals(ChartTab.BAR, ChartTab.entries[1])
        assertEquals(ChartTab.LINE, ChartTab.entries[2])
    }

    @Test
    fun timePeriodEntries() {
        assertEquals(4, TimePeriod.entries.size)
        assertEquals("1M", TimePeriod.ONE_MONTH.label)
        assertEquals("3M", TimePeriod.THREE_MONTHS.label)
        assertEquals("6M", TimePeriod.SIX_MONTHS.label)
        assertEquals("1Y", TimePeriod.ONE_YEAR.label)
    }

    @Test
    fun graphsUiStateCopyUpdatesTab() {
        val state = GraphsUiState()
        val updated = state.copy(selectedTab = ChartTab.BAR)
        assertEquals(ChartTab.BAR, updated.selectedTab)
        assertEquals(TimePeriod.ONE_MONTH, updated.selectedPeriod)
    }
}
