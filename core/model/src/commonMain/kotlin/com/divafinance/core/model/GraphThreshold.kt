package com.divafinance.core.model

import com.divafinance.core.model.enums.SpendingCategory
import kotlinx.serialization.Serializable

@Serializable
data class GraphThreshold(
    val id: String,
    val category: SpendingCategory,
    val thresholdPercent: Double,
    val isActive: Boolean = true,
)
