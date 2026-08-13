package com.divafinance.core.model.enums

import kotlinx.serialization.Serializable

@Serializable
enum class SpendingCategory(val displayName: String) {
    DINING("Dining"),
    TRAVEL("Travel"),
    GROCERIES("Groceries"),
    GAS("Gas"),
    ENTERTAINMENT("Entertainment"),
    SHOPPING("Shopping"),
    UTILITIES("Utilities"),
    HEALTHCARE("Healthcare"),
    EDUCATION("Education"),
    TRANSPORTATION("Transportation"),
    SUBSCRIPTIONS("Subscriptions"),
    OTHER("Other");
}
