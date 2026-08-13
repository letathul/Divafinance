package com.divafinance.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CardDto(
    val id: String,
    val accountId: String,
    val name: String,
    val lastFour: String? = null,
    val network: String,
    val color: String,
    val creditLimit: Double,
    val currentBalance: Double,
    val availableCredit: Double,
    val statementDate: Int? = null,
    val dueDate: Int? = null,
    val annualFee: Double,
    val rewardRules: List<RewardRuleDto> = emptyList(),
)

@Serializable
data class RewardRuleDto(
    val id: String,
    val category: String,
    val multiplier: Double,
    val rewardType: String,
    val capAmount: Double? = null,
    val capPeriod: String? = null,
)

@Serializable
data class CardRecommendationDto(
    val cardId: String,
    val cardName: String,
    val cardColor: String,
    val category: String,
    val multiplier: Double,
    val estimatedRewardValue: Double,
    val availableCredit: Double,
    val rank: Int,
)
