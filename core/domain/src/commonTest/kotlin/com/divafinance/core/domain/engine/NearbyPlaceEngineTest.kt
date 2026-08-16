package com.divafinance.core.domain.engine

import com.divafinance.core.model.LocationTag
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.testing.fake.TestData
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NearbyPlaceEngineTest {

    private val engine = NearbyPlaceEngine()

    /** Trafalgar Square, as an arbitrary but real reference point. */
    private val origin = LocationTag(51.5080, -0.1281)

    /**
     * Offsets by an approximate number of metres north/east. Good enough to place points
     * inside or outside a 150 m radius deliberately.
     */
    private fun near(metresNorth: Double, metresEast: Double = 0.0) = LocationTag(
        latitude = origin.latitude + metresNorth / 111_320.0,
        longitude = origin.longitude + metresEast / (111_320.0 * 0.622), // cos(51.5°)
    )

    private fun tx(
        id: String,
        merchant: String?,
        location: LocationTag?,
        category: SpendingCategory = SpendingCategory.DINING,
        date: LocalDate = LocalDate(2026, 6, 15),
    ) = TestData.transaction(
        id = id,
        merchantName = merchant,
        location = location,
        category = category,
        date = date,
    )

    @Test
    fun findsNothingWithoutHistory() {
        assertEquals(emptyList(), engine.nearby(emptyList(), origin))
    }

    @Test
    fun findsAShopAtTheSamePoint() {
        val history = listOf(tx("1", "Blue Bottle", origin))

        val found = engine.nearby(history, origin).single()
        assertEquals("Blue Bottle", found.name)
        assertEquals(SpendingCategory.DINING, found.category)
    }

    @Test
    fun excludesShopsBeyondTheRadius() {
        val history = listOf(tx("1", "Far Away Cafe", near(metresNorth = 900.0)))

        assertEquals(emptyList(), engine.nearby(history, origin))
    }

    @Test
    fun includesShopsJustInsideTheRadius() {
        val history = listOf(tx("1", "Corner Shop", near(metresNorth = 100.0)))

        assertEquals(1, engine.nearby(history, origin).size)
    }

    @Test
    fun ignoresTransactionsWithoutCoordinates() {
        val history = listOf(tx("1", "No Location Cafe", location = null))

        assertEquals(emptyList(), engine.nearby(history, origin))
    }

    @Test
    fun ignoresTransactionsWithoutAMerchantName() {
        val history = listOf(tx("1", merchant = null, location = origin))

        assertEquals(emptyList(), engine.nearby(history, origin))
    }

    /** Repeat visits never share coordinates exactly, so they must still be one place. */
    @Test
    fun groupsRepeatVisitsIntoOnePlace() {
        val history = listOf(
            tx("1", "Blue Bottle", near(10.0)),
            tx("2", "Blue Bottle", near(14.0)),
            tx("3", "Blue Bottle", near(8.0)),
        )

        val found = engine.nearby(history, origin).single()
        assertEquals("Blue Bottle", found.name)
        assertEquals(3, found.visits)
    }

    @Test
    fun groupsTheSameShopRegardlessOfCasing() {
        val history = listOf(
            tx("1", "Blue Bottle", near(10.0)),
            tx("2", "blue bottle", near(12.0)),
        )

        assertEquals(1, engine.nearby(history, origin).size)
    }

    @Test
    fun ordersByDistance() {
        val history = listOf(
            tx("1", "Further", near(120.0)),
            tx("2", "Nearer", near(20.0)),
        )

        assertEquals(listOf("Nearer", "Further"), engine.nearby(history, origin).map { it.name })
    }

    @Test
    fun reportsTheMostCommonCategoryForAPlace() {
        val history = listOf(
            tx("1", "Corner Shop", near(10.0), category = SpendingCategory.GROCERIES),
            tx("2", "Corner Shop", near(10.0), category = SpendingCategory.GROCERIES),
            tx("3", "Corner Shop", near(10.0), category = SpendingCategory.SHOPPING),
        )

        assertEquals(SpendingCategory.GROCERIES, engine.nearby(history, origin).single().category)
    }

    @Test
    fun reportsDistanceInMetres() {
        val history = listOf(tx("1", "Blue Bottle", near(100.0)))

        val metres = engine.nearby(history, origin).single().metresAway
        assertTrue(metres in 80.0..120.0, "expected roughly 100 m, got $metres")
    }

    @Test
    fun respectsTheLimit() {
        val history = (1..10).map { tx("$it", "Shop $it", near(it * 5.0)) }

        assertEquals(3, engine.nearby(history, origin, limit = 3).size)
    }

    @Test
    fun honoursACustomRadius() {
        val history = listOf(tx("1", "Corner Shop", near(300.0)))

        assertEquals(emptyList(), engine.nearby(history, origin, radiusMetres = 150.0))
        assertEquals(1, engine.nearby(history, origin, radiusMetres = 500.0).size)
    }

    /** Longitude degrees shrink towards the poles; the distance must account for it. */
    @Test
    fun accountsForLongitudeConvergence() {
        val equator = LocationTag(0.0, 0.0)
        val arctic = LocationTag(70.0, 0.0)
        val oneDegreeEast = 1.0

        val atEquator = engine.distanceMetres(equator, LocationTag(0.0, oneDegreeEast))
        val atArctic = engine.distanceMetres(arctic, LocationTag(70.0, oneDegreeEast))

        assertTrue(atArctic < atEquator / 2, "expected $atArctic to be far under $atEquator")
    }

    @Test
    fun distanceIsZeroForTheSamePoint() {
        assertEquals(0.0, engine.distanceMetres(origin, origin))
    }
}
