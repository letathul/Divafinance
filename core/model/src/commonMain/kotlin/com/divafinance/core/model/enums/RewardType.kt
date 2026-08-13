package com.divafinance.core.model.enums

import kotlinx.serialization.Serializable

@Serializable
enum class RewardType(val displayName: String) {
    POINTS("Points"),
    CASHBACK("Cashback"),
    MILES("Miles");
}
