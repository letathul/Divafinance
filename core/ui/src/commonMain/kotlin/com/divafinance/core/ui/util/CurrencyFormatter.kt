package com.divafinance.core.ui.util

import com.divafinance.core.common.toFixed

fun formatCurrency(amount: Double, currency: String = "USD"): String {
    val symbol = currencySymbols[currency] ?: currency
    return "$symbol${kotlin.math.abs(amount).toFixed(2)}"
}

private val currencySymbols = mapOf(
    "USD" to "$",
    "EUR" to "€",
    "GBP" to "£",
    "JPY" to "¥",
    "INR" to "₹",
    "CAD" to "CA$",
    "AUD" to "A$",
)
