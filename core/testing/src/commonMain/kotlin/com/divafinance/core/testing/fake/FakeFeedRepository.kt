package com.divafinance.core.testing.fake

import com.divafinance.core.data.repository.FeedRepository
import com.divafinance.core.model.FeedPost
import com.divafinance.core.model.enums.FeedPostType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeFeedRepository : FeedRepository {
    private val posts = MutableStateFlow<List<FeedPost>>(emptyList())

    override fun getAll(): Flow<List<FeedPost>> = posts

    override suspend fun getRecent(limit: Long): List<FeedPost> =
        posts.value.sortedByDescending { it.createdAt }.take(limit.toInt())

    override suspend fun getByType(type: FeedPostType): List<FeedPost> =
        posts.value.filter { it.type == type }

    override suspend fun getLatestBotInsight(): FeedPost? =
        posts.value.filter { it.type == FeedPostType.BOT_INSIGHT }
            .maxByOrNull { it.createdAt }

    override suspend fun insert(post: FeedPost) {
        posts.value = posts.value + post
    }

    override suspend fun delete(id: String) {
        posts.value = posts.value.filter { it.id != id }
    }

    fun getPosts(): List<FeedPost> = posts.value

    fun setPosts(list: List<FeedPost>) {
        posts.value = list
    }
}
