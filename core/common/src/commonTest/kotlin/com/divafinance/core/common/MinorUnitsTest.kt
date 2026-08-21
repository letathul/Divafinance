package com.divafinance.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MinorUnitsTest {

    @Test
    fun convertsWholeAmounts() {
        assertEquals(1_000_00L, 1000.0.toMinorUnits())
        assertEquals(0L, 0.0.toMinorUnits())
    }

    @Test
    fun convertsFractionalAmounts() {
        assertEquals(12_50L, 12.50.toMinorUnits())
        assertEquals(1L, 0.01.toMinorUnits())
    }

    /** The reason this exists rather than `(x * 100).toLong()`, which truncates. */
    @Test
    fun roundsRatherThanTruncating() {
        assertEquals(12_50L, 12.499999999.toMinorUnits())
        assertEquals(2_35L, 2.345.toMinorUnits())
    }

    /**
     * Pins the documented limit: `1.005` is stored as `1.00499999999999989…`, genuinely
     * below the midpoint, so it rounds down. Locked in because storage must agree with
     * what [toFixed] renders — a value must never display as one amount and save as
     * another.
     */
    @Test
    fun roundsTheDoubleNotTheDecimalTheUserTyped() {
        assertEquals(100L, 1.005.toMinorUnits())
        assertEquals("1.00", 1.005.toFixed(2))
    }

    @Test
    fun preservesSign() {
        assertEquals(-12_50L, (-12.50).toMinorUnits())
        assertEquals(0L, (-0.001).toMinorUnits())
    }

    /** The yen has no minor unit; scaling it by 100 would inflate it a hundredfold. */
    @Test
    fun honoursCurrenciesWithNoMinorUnit() {
        assertEquals(1000L, 1000.0.toMinorUnits(scale = 0))
        assertEquals(1000.0, 1000L.toMajorUnits(scale = 0))
    }

    @Test
    fun roundTripsBack() {
        assertEquals(12.50, 12.50.toMinorUnits().toMajorUnits())
        assertEquals(1000.0, 1000.0.toMinorUnits().toMajorUnits())
        assertEquals(0.01, 0.01.toMinorUnits().toMajorUnits())
    }

    @Test
    fun rejectsNonFiniteInput() {
        assertFailsWith<IllegalArgumentException> { Double.NaN.toMinorUnits() }
        assertFailsWith<IllegalArgumentException> { Double.POSITIVE_INFINITY.toMinorUnits() }
    }

    @Test
    fun rejectsAnUnsupportedScale() {
        assertFailsWith<IllegalArgumentException> { 1.0.toMinorUnits(scale = -1) }
        assertFailsWith<IllegalArgumentException> { 1.0.toMinorUnits(scale = 7) }
    }
}
