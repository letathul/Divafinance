package com.divafinance.core.data.repository

import com.divafinance.core.model.FeedPost
import com.divafinance.core.model.enums.FeedPostType
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun getAll(): Flow<List<FeedPost>>
    suspend fun getRecent(limit: Long): List<FeedPost>
    suspend fun getByType(type: FeedPostType): List<FeedPost>
    suspend fun getLatestBotInsight(): FeedPost?
    suspend fun insert(post: FeedPost)
    suspend fun delete(id: String)
}
