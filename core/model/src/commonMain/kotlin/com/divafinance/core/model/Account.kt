package com.divafinance.core.model

import com.divafinance.core.model.enums.AccountType
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val currency: String = "USD",
    val balance: Double = 0.0,
    val color: String? = null,
    val icon: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
)
