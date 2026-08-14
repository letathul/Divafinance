package com.divafinance.core.domain.fake

import com.divafinance.core.data.repository.ThresholdRepository
import com.divafinance.core.model.GraphThreshold
import com.divafinance.core.model.enums.SpendingCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeThresholdRepository : ThresholdRepository {
    private val thresholds = MutableStateFlow<List<GraphThreshold>>(emptyList())

    override fun getAll(): Flow<List<GraphThreshold>> = thresholds

    override suspend fun getByCategory(category: SpendingCategory): GraphThreshold? =
        thresholds.value.find { it.category == category }

    override suspend fun insert(threshold: GraphThreshold) {
        thresholds.value = thresholds.value + threshold
    }

    override suspend fun update(threshold: GraphThreshold) {
        thresholds.value = thresholds.value.map { if (it.id == threshold.id) threshold else it }
    }

    override suspend fun delete(id: String) {
        thresholds.value = thresholds.value.filter { it.id != id }
    }

    fun getThresholds(): List<GraphThreshold> = thresholds.value

    fun setThresholds(list: List<GraphThreshold>) {
        thresholds.value = list
    }
}
