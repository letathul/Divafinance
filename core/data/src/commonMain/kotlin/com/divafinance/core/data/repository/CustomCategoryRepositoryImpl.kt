package com.divafinance.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.divafinance.core.data.mapper.TransactionMapper
import com.divafinance.core.database.DivaFinanceDb
import com.divafinance.core.model.CustomCategory
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CustomCategoryRepositoryImpl(
    private val db: DivaFinanceDb,
) : CustomCategoryRepository {

    override fun getAll(): Flow<List<CustomCategory>> =
        db.customCategoryQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getById(id: String): CustomCategory? =
        db.customCategoryQueries.selectById(id).executeAsOneOrNull()?.toDomain()

    override suspend fun insert(category: CustomCategory) {
        db.customCategoryQueries.insert(
            id = category.id,
            name = category.name,
            icon_key = category.iconKey,
            color_hex = category.colorHex,
            parent = category.parent.name,
            created_at = category.createdAt.toString(),
        )
    }

    override suspend fun update(category: CustomCategory) {
        db.customCategoryQueries.update(
            name = category.name,
            icon_key = category.iconKey,
            color_hex = category.colorHex,
            parent = category.parent.name,
            id = category.id,
        )
    }

    override suspend fun delete(id: String) {
        db.customCategoryQueries.delete(id)
    }
}

/**
 * `parent` decodes through [TransactionMapper.categoryOf] rather than `valueOf` for the
 * same reason transaction rows do: a removed enum constant should degrade to `OTHER`, not
 * make the row throw on read.
 */
private fun com.divafinance.core.database.CustomCategory.toDomain(): CustomCategory =
    CustomCategory(
        id = id,
        name = name,
        iconKey = icon_key,
        colorHex = color_hex,
        parent = TransactionMapper.categoryOf(parent),
        createdAt = Instant.parse(created_at),
    )
