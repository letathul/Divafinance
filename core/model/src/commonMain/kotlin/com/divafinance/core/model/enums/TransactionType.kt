package com.divafinance.core.model.enums

import kotlinx.serialization.Serializable

@Serializable
enum class TransactionType {
    DEBIT,
    CREDIT;
}
