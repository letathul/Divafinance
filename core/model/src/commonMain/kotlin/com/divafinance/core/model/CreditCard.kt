package com.divafinance.core.model

import com.divafinance.core.model.enums.CardNetwork
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CreditCard(
    val id: String,
    val accountId: String,
    val name: String,
    val lastFour: String? = null,
    val network: CardNetwork = CardNetwork.OTHER,
    val color: String = "#1E1E2E",
    val creditLimit: Double = 0.0,
    val currentBalance: Double = 0.0,
    val statementDate: Int? = null,
    val dueDate: Int? = null,
    val annualFee: Double = 0.0,
    val rewardRules: List<CardRewardRule> = emptyList(),
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val availableCredit: Double get() = creditLimit - currentBalance
}
