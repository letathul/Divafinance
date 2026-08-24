package com.divafinance.core.domain.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BillSplitEngineTest {

    private val engine = BillSplitEngine()

    /** Index 0 is the payer by convention. */
    private fun people(count: Int, vararg weights: Int) = List(count) { index ->
        SplitParticipant(
            personId = if (index == 0) null else "p$index",
            name = if (index == 0) "You" else "Person $index",
            weight = weights.getOrElse(index) { 1 },
        )
    }

    // --- conservation: the invariant that matters --------------------------

    @Test
    fun sharesAlwaysSumToTheTotal() {
        val result = assertNotNull(engine.split(100_000L, people(3)))
        assertEquals(100_000L, result.shares.sumOf { it.amountMinor })
    }

    /**
     * The case that motivates integer arithmetic: £1000 three ways is £333.333, and three
     * rounded shares of £333.33 come to £999.99.
     */
    @Test
    fun allocatesTheOddPennyRatherThanLosingIt() {
        val result = assertNotNull(engine.split(100_000L, people(3)))

        assertEquals(listOf(33_334L, 33_333L, 33_333L), result.shares.map { it.amountMinor })
        assertEquals(100_000L, result.shares.sumOf { it.amountMinor })
    }

    /** Property test across the whole realistic input space. */
    @Test
    fun conservesMoneyForEveryCombination() {
        val random = Random(20260816)
        repeat(2_000) {
            val subtotal = random.nextLong(0L, 5_000_00L)
            val count = random.nextInt(1, 21)
            val tip = random.nextInt(0, 31).toDouble()

            val result = assertNotNull(
                engine.split(subtotal, people(count), tipPercent = tip),
                "split returned null for subtotal=$subtotal count=$count tip=$tip",
            )
            assertEquals(
                result.totalMinor,
                result.shares.sumOf { it.amountMinor },
                "shares lost money: subtotal=$subtotal count=$count tip=$tip",
            )
        }
    }

    @Test
    fun conservesMoneyForWeightedSplits() {
        val random = Random(99)
        repeat(1_000) {
            val subtotal = random.nextLong(1L, 200_000L)
            val weights = List(random.nextInt(1, 8)) { random.nextInt(1, 6) }
            val participants = weights.mapIndexed { i, w ->
                SplitParticipant(if (i == 0) null else "p$i", "P$i", w)
            }

            val result = assertNotNull(
                engine.split(subtotal, participants, method = SplitMethod.ByShares),
            )
            assertEquals(result.totalMinor, result.shares.sumOf { it.amountMinor })
        }
    }

    // --- fairness -----------------------------------------------------------

    @Test
    fun evenSharesDifferByAtMostOneMinorUnit() {
        val shares = assertNotNull(engine.split(100_001L, people(7))).shares.map { it.amountMinor }

        assertTrue(shares.max() - shares.min() <= 1L, "spread too wide: $shares")
    }

    @Test
    fun weightsAreRespected() {
        // 1:3 on 400 minor units → 100 / 300.
        val participants = listOf(
            SplitParticipant(null, "You", weight = 1),
            SplitParticipant("p1", "Them", weight = 3),
        )
        val result = assertNotNull(
            engine.split(400L, participants, method = SplitMethod.ByShares),
        )

        assertEquals(listOf(100L, 300L), result.shares.map { it.amountMinor })
    }

    @Test
    fun evenlyIgnoresWeights() {
        val result = assertNotNull(engine.split(400L, people(2, 1, 3), method = SplitMethod.Evenly))

        assertEquals(listOf(200L, 200L), result.shares.map { it.amountMinor })
    }

    // --- determinism --------------------------------------------------------

    @Test
    fun isDeterministic() {
        val first = assertNotNull(engine.split(100_000L, people(3), tipPercent = 17.5))
        repeat(5) {
            val again = assertNotNull(engine.split(100_000L, people(3), tipPercent = 17.5))
            assertEquals(first.shares.map { it.amountMinor }, again.shares.map { it.amountMinor })
        }
    }

    // --- tip ----------------------------------------------------------------

    @Test
    fun addsTipToTheTotal() {
        val result = assertNotNull(engine.split(100_00L, people(1), tipPercent = 18.0))

        assertEquals(100_00L, result.subtotalMinor)
        assertEquals(18_00L, result.tipMinor)
        assertEquals(118_00L, result.totalMinor)
    }

    @Test
    fun handlesFractionalTipPercentages() {
        // 12.5% of 100.00 is exactly 12.50.
        val result = assertNotNull(engine.split(100_00L, people(1), tipPercent = 12.5))

        assertEquals(12_50L, result.tipMinor)
    }

    @Test
    fun roundsTipHalfUp() {
        // 10% of 0.05 is 0.005 → rounds to 0.01.
        val result = assertNotNull(engine.split(5L, people(1), tipPercent = 10.0))

        assertEquals(1L, result.tipMinor)
    }

    /**
     * Tip must be folded in before splitting. Allocating subtotal and tip separately would
     * round twice per person and can drift from what the card was charged.
     */
    @Test
    fun splitsTheTippedTotalNotTheSubtotalAndTipSeparately() {
        val result = assertNotNull(engine.split(100_00L, people(3), tipPercent = 15.0))

        assertEquals(115_00L, result.totalMinor)
        assertEquals(115_00L, result.shares.sumOf { it.amountMinor })
    }

    @Test
    fun zeroTipIsExact() {
        val result = assertNotNull(engine.split(100_00L, people(2)))

        assertEquals(0L, result.tipMinor)
        assertEquals(100_00L, result.totalMinor)
    }

    // --- payer / others share ----------------------------------------------

    @Test
    fun reportsThePayerShareAndTheRest() {
        val result = assertNotNull(engine.split(120_00L, people(3)))

        assertEquals(40_00L, result.ownShareMinor)
        assertEquals(80_00L, result.othersShareMinor)
        assertEquals(result.totalMinor, result.ownShareMinor + result.othersShareMinor)
    }

    /** Splitting with nobody else owes nothing — the whole bill is your own spending. */
    @Test
    fun aSoloSplitOwesNothing() {
        val result = assertNotNull(engine.split(100_00L, people(1)))

        assertEquals(100_00L, result.ownShareMinor)
        assertEquals(0L, result.othersShareMinor)
    }

    // --- exact amounts ------------------------------------------------------

    @Test
    fun acceptsExactAmountsThatReconcile() {
        val result = assertNotNull(
            engine.split(
                100_00L,
                people(3),
                method = SplitMethod.ByExactAmounts(listOf(50_00L, 30_00L, 20_00L)),
            ),
        )

        assertEquals(listOf(50_00L, 30_00L, 20_00L), result.shares.map { it.amountMinor })
    }

    @Test
    fun rejectsExactAmountsThatDoNotSumToTheTotal() {
        assertNull(
            engine.split(
                100_00L,
                people(3),
                method = SplitMethod.ByExactAmounts(listOf(50_00L, 30_00L, 19_99L)),
            ),
        )
    }

    /** Exact amounts must reconcile against the tipped total, not the subtotal. */
    @Test
    fun exactAmountsMustCoverTheTipToo() {
        assertNull(
            engine.split(
                100_00L,
                people(2),
                tipPercent = 10.0,
                method = SplitMethod.ByExactAmounts(listOf(50_00L, 50_00L)),
            ),
        )
        assertNotNull(
            engine.split(
                100_00L,
                people(2),
                tipPercent = 10.0,
                method = SplitMethod.ByExactAmounts(listOf(55_00L, 55_00L)),
            ),
        )
    }

    @Test
    fun rejectsTheWrongNumberOfExactAmounts() {
        assertNull(
            engine.split(100_00L, people(3), method = SplitMethod.ByExactAmounts(listOf(50_00L, 50_00L))),
        )
    }

    @Test
    fun rejectsNegativeExactAmounts() {
        assertNull(
            engine.split(100L, people(2), method = SplitMethod.ByExactAmounts(listOf(200L, -100L))),
        )
    }

    // --- rejection ----------------------------------------------------------

    @Test
    fun rejectsNoParticipants() {
        assertNull(engine.split(100_00L, emptyList()))
    }

    @Test
    fun rejectsANegativeSubtotal() {
        assertNull(engine.split(-1L, people(2)))
    }

    @Test
    fun rejectsANegativeTip() {
        assertNull(engine.split(100_00L, people(2), tipPercent = -5.0))
    }

    @Test
    fun rejectsANonFiniteTip() {
        assertNull(engine.split(100_00L, people(2), tipPercent = Double.NaN))
        assertNull(engine.split(100_00L, people(2), tipPercent = Double.POSITIVE_INFINITY))
    }

    @Test
    fun rejectsZeroTotalWeight() {
        val participants = listOf(
            SplitParticipant(null, "You", weight = 0),
            SplitParticipant("p1", "Them", weight = 0),
        )
        assertNull(engine.split(100_00L, participants, method = SplitMethod.ByShares))
    }

    @Test
    fun rejectsNegativeWeights() {
        val participants = listOf(
            SplitParticipant(null, "You", weight = 2),
            SplitParticipant("p1", "Them", weight = -1),
        )
        assertNull(engine.split(100_00L, participants, method = SplitMethod.ByShares))
    }

    // --- degenerate ---------------------------------------------------------

    @Test
    fun splitsASingleMinorUnitThreeWays() {
        val result = assertNotNull(engine.split(1L, people(3)))

        assertEquals(listOf(1L, 0L, 0L), result.shares.map { it.amountMinor })
        assertEquals(1L, result.shares.sumOf { it.amountMinor })
    }

    @Test
    fun handlesAZeroBill() {
        val result = assertNotNull(engine.split(0L, people(3), tipPercent = 20.0))

        assertEquals(0L, result.totalMinor)
        assertTrue(result.shares.all { it.amountMinor == 0L })
    }

    // --- who paid ------------------------------------------------------------

    /**
     * The odd unit goes to whoever put the money down. Rounding against the person holding
     * the receipt is the one direction nobody has to be asked about.
     */
    @Test
    fun thePayerAbsorbsTheOddMinorUnit() {
        val result = assertNotNull(engine.split(10_00L, people(3), payerIndex = 2))

        assertEquals(listOf(333L, 333L, 334L), result.shares.map { it.amountMinor })
        assertEquals(10_00L, result.shares.sumOf { it.amountMinor })
    }

    /** With no payer named, index 0 — the user — absorbs it, as it always did. */
    @Test
    fun theUserAbsorbsTheOddMinorUnitByDefault() {
        val result = assertNotNull(engine.split(10_00L, people(3)))

        assertEquals(listOf(334L, 333L, 333L), result.shares.map { it.amountMinor })
    }

    /**
     * Consumption is not a function of who paid: the user ate a third of the bill whether
     * they reached for it or not, and that third is what reaches spending reports.
     */
    @Test
    fun theOwnShareIsIndependentOfWhoPaid() {
        val mine = assertNotNull(engine.split(90_00L, people(3), payerIndex = 0))
        val theirs = assertNotNull(engine.split(90_00L, people(3), payerIndex = 1))

        assertEquals(30_00L, mine.ownShareMinor)
        assertEquals(30_00L, theirs.ownShareMinor)
        assertEquals(60_00L, theirs.othersShareMinor)
    }

    @Test
    fun namesThePayerOnlyWhenItIsNotTheUser() {
        assertNull(assertNotNull(engine.split(90_00L, people(3))).payer)
        assertEquals(
            "Person 1",
            assertNotNull(engine.split(90_00L, people(3), payerIndex = 1)).payer?.name,
        )
    }

    /** A payer who is not on the bill has no correct answer, so nothing is returned. */
    @Test
    fun refusesAPayerOutsideTheParticipants() {
        assertNull(engine.split(90_00L, people(3), payerIndex = 3))
        assertNull(engine.split(90_00L, people(3), payerIndex = -1))
    }

    /** A zero-weight participant pays nothing but still appears on the bill. */
    @Test
    fun allowsAZeroWeightParticipant() {
        val participants = listOf(
            SplitParticipant(null, "You", weight = 1),
            SplitParticipant("p1", "Guest", weight = 0),
        )
        val result = assertNotNull(
            engine.split(100_00L, participants, method = SplitMethod.ByShares),
        )

        assertEquals(listOf(100_00L, 0L), result.shares.map { it.amountMinor })
    }
}
