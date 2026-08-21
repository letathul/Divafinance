package com.divafinance.core.domain.engine

import kotlin.math.roundToLong

/**
 * Someone the bill is being split between.
 *
 * [personId] is null for the user themselves. Convention throughout this engine: the user
 * is the payer and sits at index 0 of the participant list.
 */
data class SplitParticipant(
    val personId: String?,
    val name: String,
    /** Relative share. Ignored by [SplitMethod.Evenly]; used by [SplitMethod.ByShares]. */
    val weight: Int = 1,
)

sealed interface SplitMethod {
    /** Equal shares for everyone, whatever their weight says. */
    data object Evenly : SplitMethod

    /** Proportional to [SplitParticipant.weight] — "I had two courses, you had one". */
    data object ByShares : SplitMethod

    /**
     * Caller-supplied exact minor-unit amounts, positionally matching the participants.
     * Rejected unless they sum to the grand total, since the point of this engine is that
     * money is never lost.
     */
    data class ByExactAmounts(val amountsMinor: List<Long>) : SplitMethod
}

data class SplitShare(
    val participant: SplitParticipant,
    val amountMinor: Long,
)

data class SplitResult(
    val subtotalMinor: Long,
    val tipMinor: Long,
    val totalMinor: Long,
    val shares: List<SplitShare>,
) {
    /** The payer's own consumption — the only part that belongs in spending reports. */
    val payerShareMinor: Long get() = shares.firstOrNull()?.amountMinor ?: 0L

    /** What everyone else owes back. Becomes the transaction's `others_share`. */
    val othersShareMinor: Long get() = totalMinor - payerShareMinor
}

/**
 * Divides a bill, with tip, between people so that the shares always add back up.
 *
 * Works entirely in minor units. Rounding each share independently would lose money —
 * £1000 three ways is £333.333, and three shares of £333.33 leave a penny unaccounted —
 * so the remainder is allocated explicitly rather than left to rounding.
 *
 * Pure and history-free like [RewardRecommendationEngine] and [CategoryPredictionEngine]:
 * everything comes in as arguments, so every case is testable without repositories.
 *
 * Percentage-based splitting is deliberately absent; it needs its own "must sum to 100"
 * validation and nothing asks for it yet. [SplitMethod.ByExactAmounts] covers the cases
 * a percentage would.
 */
class BillSplitEngine {

    /**
     * Returns null for input that has no correct answer: no participants, a negative
     * subtotal or tip, non-positive total weight, or exact amounts that do not reconcile.
     * Null means "do not save this", never "treat it as zero".
     */
    fun split(
        subtotalMinor: Long,
        participants: List<SplitParticipant>,
        tipPercent: Double = 0.0,
        method: SplitMethod = SplitMethod.Evenly,
    ): SplitResult? {
        if (participants.isEmpty()) return null
        if (subtotalMinor < 0L) return null
        if (!tipPercent.isFinite() || tipPercent < 0.0) return null

        val tipMinor = tipOf(subtotalMinor, tipPercent) ?: return null
        val totalMinor = subtotalMinor + tipMinor

        val amounts = when (method) {
            SplitMethod.Evenly -> allocate(totalMinor, participants.map { 1 })
            SplitMethod.ByShares -> allocate(totalMinor, participants.map { it.weight })
            is SplitMethod.ByExactAmounts -> {
                if (method.amountsMinor.size != participants.size) return null
                if (method.amountsMinor.any { it < 0L }) return null
                if (method.amountsMinor.sum() != totalMinor) return null
                method.amountsMinor
            }
        } ?: return null

        return SplitResult(
            subtotalMinor = subtotalMinor,
            tipMinor = tipMinor,
            totalMinor = totalMinor,
            shares = participants.mapIndexed { index, participant ->
                SplitShare(participant, amounts[index])
            },
        )
    }

    /**
     * Tip in whole minor units, half-up.
     *
     * Computed once against the subtotal and then folded into the total before any
     * splitting. Allocating subtotal and tip separately and adding the two results gives
     * two rounding events per person, which can drift several minor units away from what
     * the card was actually charged.
     */
    private fun tipOf(subtotalMinor: Long, tipPercent: Double): Long? {
        if (tipPercent == 0.0) return 0L
        // Basis points keep the whole calculation in integers: 12.5% becomes 1250.
        val basisPoints = (tipPercent * 100.0).roundToLong()
        if (basisPoints < 0L) return null
        return (subtotalMinor * basisPoints + 5_000L) / 10_000L
    }

    /**
     * Largest-remainder (Hamilton) allocation.
     *
     * Everyone gets the floor of their exact share, then the leftover units go one each to
     * the largest fractional remainders. Ties break towards the lower index, and the payer
     * is index 0 — so on a clean three-way split of £1000 the payer absorbs the odd penny.
     * That falls out of the ordering rather than needing a special case, which is why it
     * stays correct for weighted splits too.
     */
    private fun allocate(totalMinor: Long, weights: List<Int>): List<Long>? {
        if (weights.any { it < 0 }) return null
        val totalWeight = weights.sumOf { it.toLong() }
        if (totalWeight <= 0L) return null

        val base = weights.map { totalMinor * it / totalWeight }
        val remainders = weights.map { totalMinor * it % totalWeight }

        var leftover = totalMinor - base.sum()
        val result = base.toMutableList()

        // Descending remainder, then ascending index — deterministic for equal remainders.
        val order = remainders.indices.sortedWith(
            compareByDescending<Int> { remainders[it] }.thenBy { it },
        )
        var cursor = 0
        while (leftover > 0L && cursor < order.size) {
            result[order[cursor]] += 1L
            leftover -= 1L
            cursor++
        }

        // Each base loses strictly less than one unit, so leftover is always < N and the
        // loop above drains it. Refusing to return here rather than trusting that keeps a
        // silent shortfall from ever reaching a saved amount.
        if (leftover != 0L) return null

        return result
    }
}
