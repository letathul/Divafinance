package com.divafinance.core.model.enums

import kotlinx.serialization.Serializable

@Serializable
enum class CapPeriod {
    MONTHLY,
    QUARTERLY,
    ANNUAL;
}
