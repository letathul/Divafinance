package com.divafinance.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the two decisions that made location fail in practice before this was extracted:
 * only the first provider was ever tried, and satellite positioning was attempted with
 * coarse-only permission where it throws.
 */
class PositioningSourceTest {

    private val all = setOf(PositioningSource.NETWORK, PositioningSource.SATELLITE)

    @Test
    fun asksNetworkBeforeSatellite() {
        assertEquals(
            listOf(PositioningSource.NETWORK, PositioningSource.SATELLITE),
            usablePositioningSources(all, fineLocationGranted = true),
        )
    }

    /** Both are returned so the caller can fall back, not just the preferred one. */
    @Test
    fun offersAFallbackRatherThanASingleChoice() {
        assertTrue(usablePositioningSources(all, fineLocationGranted = true).size > 1)
    }

    @Test
    fun excludesSatelliteWithoutFinePermission() {
        assertEquals(
            listOf(PositioningSource.NETWORK),
            usablePositioningSources(all, fineLocationGranted = false),
        )
    }

    /** Coarse-only with no network provider genuinely has nothing to offer. */
    @Test
    fun isEmptyWhenOnlySatelliteIsAvailableAndFineIsDenied() {
        assertEquals(
            emptyList(),
            usablePositioningSources(setOf(PositioningSource.SATELLITE), fineLocationGranted = false),
        )
    }

    @Test
    fun usesSatelliteAloneWhenNetworkIsOff() {
        assertEquals(
            listOf(PositioningSource.SATELLITE),
            usablePositioningSources(setOf(PositioningSource.SATELLITE), fineLocationGranted = true),
        )
    }

    @Test
    fun usesNetworkAloneWhenSatelliteIsOff() {
        assertEquals(
            listOf(PositioningSource.NETWORK),
            usablePositioningSources(setOf(PositioningSource.NETWORK), fineLocationGranted = true),
        )
    }

    @Test
    fun isEmptyWhenLocationIsTurnedOff() {
        assertEquals(emptyList(), usablePositioningSources(emptySet(), fineLocationGranted = true))
    }

    @Test
    fun neverReturnsASourceThatIsNotEnabled() {
        val enabled = setOf(PositioningSource.NETWORK)
        assertTrue(usablePositioningSources(enabled, fineLocationGranted = true).all { it in enabled })
    }
}
