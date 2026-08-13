package com.divafinance.core.model.enums

import kotlinx.serialization.Serializable

@Serializable
enum class CardNetwork(val displayName: String) {
    VISA("Visa"),
    MASTERCARD("Mastercard"),
    AMEX("American Express"),
    DISCOVER("Discover"),
    OTHER("Other");
}
