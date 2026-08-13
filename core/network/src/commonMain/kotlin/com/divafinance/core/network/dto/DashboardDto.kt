package com.divafinance.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class DashboardDto(
    val totalBalance: Double,
    val totalCreditUsed: Double,
    val totalCreditAvailable: Double,
    val accountCount: Int,
    val cardCount: Int,
    val recentTransactions: List<TransactionDto>,
    val categorySpending: List<CategorySpendingDto>,
)

@Serializable
data class CategorySpendingDto(
    val category: String,
    val total: Double,
    val percentage: Double,
)

@Serializable
data class AuthRequestDto(
    val pin: String,
)

@Serializable
data class AuthResponseDto(
    val success: Boolean,
    val message: String? = null,
)
