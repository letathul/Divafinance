package com.divafinance.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Currency(
    val code: String,
    val name: String,
    val symbol: String,
    /**
     * Digits after the decimal point. Anything doing exact money arithmetic has to scale
     * by this rather than assuming 100 — the yen has no minor unit at all, so treating
     * ¥1000 as 100000 minor units would inflate it a hundredfold.
     */
    val minorUnits: Int = 2,
) {
    companion object {
        val USD = Currency("USD", "US Dollar", "$")
        val EUR = Currency("EUR", "Euro", "€")
        val GBP = Currency("GBP", "British Pound", "£")
        val INR = Currency("INR", "Indian Rupee", "₹")
        val JPY = Currency("JPY", "Japanese Yen", "¥", minorUnits = 0)
        val CAD = Currency("CAD", "Canadian Dollar", "CA$")
        val AUD = Currency("AUD", "Australian Dollar", "A$")

        val supported = listOf(USD, EUR, GBP, INR, JPY, CAD, AUD)

        /** Unknown codes fall back to a 2-decimal currency rather than throwing. */
        fun fromCode(code: String): Currency =
            supported.find { it.code == code } ?: Currency(code, code, code)
    }
}
