package com.divafinance.core.ui.util

fun formatCurrency(amount: Double, currency: String = "USD"): String {
    val symbol = currencySymbols[currency] ?: currency
    return "$symbol${"%.2f".format(kotlin.math.abs(amount))}"
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
