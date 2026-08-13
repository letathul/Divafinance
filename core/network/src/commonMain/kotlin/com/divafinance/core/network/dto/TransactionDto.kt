package com.divafinance.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class TransactionDto(
    val id: String,
    val accountId: String,
    val cardId: String? = null,
    val amount: Double,
    val currency: String,
    val category: String,
    val merchantName: String? = null,
    val note: String? = null,
    val date: String,
    val type: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
)
