package com.divafinance.core.model.enums

import kotlinx.serialization.Serializable

@Serializable
enum class AccountType(val displayName: String) {
    CHECKING("Checking"),
    SAVINGS("Savings"),
    WALLET("Wallet"),
    CREDIT("Credit");
}
