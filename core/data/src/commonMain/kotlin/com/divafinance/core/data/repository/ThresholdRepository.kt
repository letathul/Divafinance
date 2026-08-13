package com.divafinance.core.data.repository

import com.divafinance.core.model.GraphThreshold
import com.divafinance.core.model.enums.SpendingCategory
import kotlinx.coroutines.flow.Flow

interface ThresholdRepository {
    fun getAll(): Flow<List<GraphThreshold>>
    suspend fun getByCategory(category: SpendingCategory): GraphThreshold?
    suspend fun insert(threshold: GraphThreshold)
    suspend fun update(threshold: GraphThreshold)
    suspend fun delete(id: String)
}
