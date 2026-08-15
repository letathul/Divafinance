package com.divafinance.core.model

import com.divafinance.core.model.enums.SpendingCategory
import kotlinx.serialization.Serializable

@Serializable
data class RewardCategory(
    val category: SpendingCategory,
    val displayName: String = category.displayName,
    val iconName: String? = null,
)
