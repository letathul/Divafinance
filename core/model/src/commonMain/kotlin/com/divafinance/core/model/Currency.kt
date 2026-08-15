package com.divafinance.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Currency(
    val code: String,
    val name: String,
    val symbol: String,
) {
    companion object {
        val USD = Currency("USD", "US Dollar", "$")
        val EUR = Currency("EUR", "Euro", "€")
        val GBP = Currency("GBP", "British Pound", "£")
        val INR = Currency("INR", "Indian Rupee", "₹")
        val JPY = Currency("JPY", "Japanese Yen", "¥")
        val CAD = Currency("CAD", "Canadian Dollar", "CA$")
        val AUD = Currency("AUD", "Australian Dollar", "A$")

        val supported = listOf(USD, EUR, GBP, INR, JPY, CAD, AUD)

        fun fromCode(code: String): Currency =
            supported.find { it.code == code } ?: Currency(code, code, code)
    }
}
