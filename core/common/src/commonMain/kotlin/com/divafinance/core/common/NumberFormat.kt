package com.divafinance.core.common

import kotlin.math.abs
import kotlin.math.floor

/**
 * Multiplatform replacement for `"%.Nf".format(value)`.
 *
 * `String.format` is a JVM-only extension, so it compiles on Android but breaks the
 * iOS/native targets. This formats a fixed number of decimal places using HALF_UP
 * rounding, matching the JVM's `%f` conversion for the display-sized values this app
 * deals with.
 */
fun Double.toFixed(decimals: Int): String {
    require(decimals >= 0) { "decimals must be non-negative, was $decimals" }

    if (isNaN()) return "NaN"
    if (isInfinite()) return if (this > 0) "Infinity" else "-Infinity"

    var factor = 1L
    repeat(decimals) { factor *= 10L }

    // floor(x + 0.5) on a non-negative value gives HALF_UP, unlike kotlin.math.round.
    val scaled = floor(abs(this) * factor + 0.5)

    // Beyond Long range the fractional digits are meaningless anyway; fall back to
    // the platform's own rendering rather than overflowing.
    if (scaled > Long.MAX_VALUE.toDouble()) return toString()

    val scaledLong = scaled.toLong()
    val whole = scaledLong / factor
    val fraction = scaledLong % factor
    val sign = if (this < 0 && scaledLong != 0L) "-" else ""

    return if (decimals == 0) {
        "$sign$whole"
    } else {
        "$sign$whole.${fraction.toString().padStart(decimals, '0')}"
    }
}

/** [toFixed] for Float-typed values, e.g. chart percentages. */
fun Float.toFixed(decimals: Int): String = toDouble().toFixed(decimals)
