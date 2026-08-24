package com.divafinance.core.testing.fake

import com.divafinance.core.data.repository.CustomCategoryRepository
import com.divafinance.core.model.CustomCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCustomCategoryRepository : CustomCategoryRepository {
    private val categories = MutableStateFlow<List<CustomCategory>>(emptyList())

    override fun getAll(): Flow<List<CustomCategory>> = categories

    override suspend fun getById(id: String): CustomCategory? =
        categories.value.find { it.id == id }

    override suspend fun insert(category: CustomCategory) {
        categories.value = categories.value + category
    }

    override suspend fun update(category: CustomCategory) {
        categories.value = categories.value.map { if (it.id == category.id) category else it }
    }

    override suspend fun delete(id: String) {
        categories.value = categories.value.filterNot { it.id == id }
    }

    fun setCategories(list: List<CustomCategory>) {
        categories.value = list
    }
}
