package com.divafinance.core.data.repository

import com.divafinance.core.model.CustomCategory
import kotlinx.coroutines.flow.Flow

interface CustomCategoryRepository {
    fun getAll(): Flow<List<CustomCategory>>
    suspend fun getById(id: String): CustomCategory?
    suspend fun insert(category: CustomCategory)
    suspend fun update(category: CustomCategory)

    /**
     * Hard delete. Transactions filed under this category keep their `custom_category_id`
     * — foreign keys are off app-wide, so nothing refuses the delete — but they still
     * carry the parent [com.divafinance.core.model.enums.SpendingCategory], so they go on
     * displaying and reporting as that built-in rather than becoming unreadable.
     */
    suspend fun delete(id: String)
}
