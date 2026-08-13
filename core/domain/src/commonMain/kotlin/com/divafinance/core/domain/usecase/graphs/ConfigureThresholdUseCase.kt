package com.divafinance.core.domain.usecase.graphs

import com.divafinance.core.data.repository.ThresholdRepository
import com.divafinance.core.model.GraphThreshold

class ConfigureThresholdUseCase(
    private val thresholdRepository: ThresholdRepository
) {
    suspend fun upsert(threshold: GraphThreshold) {
        val existing = thresholdRepository.getByCategory(threshold.category)
        if (existing != null) {
            thresholdRepository.update(threshold.copy(id = existing.id))
        } else {
            thresholdRepository.insert(threshold)
        }
    }

    suspend fun delete(id: String) {
        thresholdRepository.delete(id)
    }
}
