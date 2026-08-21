package com.divafinance.core.common

import kotlin.math.abs
import kotlin.math.floor

/**
 * Conversions between a display amount and exact minor units (cents, pence, paise).
 *
 * Money arithmetic that has to balance — splitting a bill, allocating a remainder — must
 * happen in whole minor units. `Double` cannot represent most decimal fractions, so
 * rounding each share independently loses or invents money: three shares of `333.33` are
 * `999.99`, not `1000.00`.
 *
 * [scale] is the currency's decimal digits, not a constant. The yen has none, so scaling
 * ¥1000 by 100 would inflate it a hundredfold — see [com.divafinance.core.model.Currency].
 */

/** Largest [scale] supported. Beyond this the multiplier stops fitting sensibly in a Long. */
private const val MAX_SCALE = 6

private fun multiplierFor(scale: Int): Long {
    require(scale in 0..MAX_SCALE) { "scale must be 0..$MAX_SCALE, was $scale" }
    var multiplier = 1L
    repeat(scale) { multiplier *= 10L }
    return multiplier
}

/**
 * Rounds to the nearest minor unit, half away from zero.
 *
 * Not `(this * multiplier).toLong()`, which truncates — `12.50` would become `1249` about
 * as often as `1250`.
 *
 * This rounds the `Double`, which is not always the decimal the user typed: `1.005` is
 * stored as `1.00499999999999989…`, genuinely below the midpoint, so it rounds down to
 * `100`. That is deliberate. The same formula backs [Double.toFixed] and
 * [Double.roundToCents], so a value always stores as the amount it displays as — a
 * cleverer rounding here would make the saved figure disagree with the one on screen,
 * which is worse than being a minor unit off a midpoint that was never representable.
 */
fun Double.toMinorUnits(scale: Int = 2): Long {
    require(isFinite()) { "cannot convert $this to minor units" }
    val scaled = floor(abs(this) * multiplierFor(scale) + 0.5)
    val magnitude = scaled.toLong()
    return if (this < 0) -magnitude else magnitude
}

/** Back to a display amount. Exact for every value that fits a Double's 53-bit mantissa. */
fun Long.toMajorUnits(scale: Int = 2): Double =
    this.toDouble() / multiplierFor(scale)
