package com.divafinance.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.divafinance.core.database.DivaFinanceDb
import com.divafinance.core.model.GraphThreshold
import com.divafinance.core.model.enums.SpendingCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ThresholdRepositoryImpl(
    private val db: DivaFinanceDb
) : ThresholdRepository {

    override fun getAll(): Flow<List<GraphThreshold>> {
        return db.graphThresholdQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }
    }

    override suspend fun getByCategory(category: SpendingCategory): GraphThreshold? {
        return db.graphThresholdQueries.selectByCategory(category.name)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override suspend fun insert(threshold: GraphThreshold) {
        db.graphThresholdQueries.insert(
            id = threshold.id,
            category = threshold.category.name,
            threshold_percent = threshold.thresholdPercent,
            is_active = if (threshold.isActive) 1L else 0L,
        )
    }

    override suspend fun update(threshold: GraphThreshold) {
        db.graphThresholdQueries.update(
            category = threshold.category.name,
            threshold_percent = threshold.thresholdPercent,
            is_active = if (threshold.isActive) 1L else 0L,
            id = threshold.id,
        )
    }

    override suspend fun delete(id: String) {
        db.graphThresholdQueries.delete(id)
    }
}

private fun com.divafinance.core.database.GraphThreshold.toDomain() = GraphThreshold(
    id = id,
    category = SpendingCategory.valueOf(category),
    thresholdPercent = threshold_percent,
    isActive = is_active == 1L,
)
