package com.divafinance.core.model

import com.divafinance.core.model.enums.CapPeriod
import com.divafinance.core.model.enums.RewardType
import com.divafinance.core.model.enums.SpendingCategory
import kotlinx.serialization.Serializable

@Serializable
data class CardRewardRule(
    val id: String,
    val cardId: String,
    val category: SpendingCategory,
    val multiplier: Double,
    val rewardType: RewardType,
    val capAmount: Double? = null,
    val capPeriod: CapPeriod? = null,
    val isActive: Boolean = true,
)
