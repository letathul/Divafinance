package com.divafinance.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.divafinance.core.database.DivaFinanceDb
import com.divafinance.core.model.FeedPost
import com.divafinance.core.model.enums.FeedPostType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

class FeedRepositoryImpl(
    private val db: DivaFinanceDb
) : FeedRepository {

    override fun getAll(): Flow<List<FeedPost>> {
        return db.feedPostQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }
    }

    override suspend fun getRecent(limit: Long): List<FeedPost> {
        return db.feedPostQueries.selectRecent(limit)
            .executeAsList()
            .map { it.toDomain() }
    }

    override suspend fun getByType(type: FeedPostType): List<FeedPost> {
        return db.feedPostQueries.selectByType(type.name)
            .executeAsList()
            .map { it.toDomain() }
    }

    override suspend fun getLatestBotInsight(): FeedPost? {
        return db.feedPostQueries.selectLatestBotInsight().executeAsOneOrNull()?.toDomain()
    }

    override suspend fun insert(post: FeedPost) {
        db.feedPostQueries.insert(
            id = post.id,
            type = post.type.name,
            title = post.title,
            body = post.body,
            transaction_id = post.transactionId,
            metadata = post.metadata,
            created_at = post.createdAt.toString(),
        )
    }

    override suspend fun delete(id: String) {
        db.feedPostQueries.delete(id)
    }
}

private fun com.divafinance.core.database.FeedPost.toDomain() = FeedPost(
    id = id,
    type = runCatching { FeedPostType.valueOf(type) }.getOrDefault(FeedPostType.TRANSACTION),
    title = title,
    body = body,
    transactionId = transaction_id,
    metadata = metadata,
    createdAt = Instant.parse(created_at),
)
