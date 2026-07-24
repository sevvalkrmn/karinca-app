package com.karinca.app.data.repository

import androidx.room.withTransaction
import com.karinca.app.data.local.CategoryKind
import com.karinca.app.data.local.KarincaDatabase
import com.karinca.app.data.local.dao.CategoryDao
import com.karinca.app.data.local.dao.OutboxDao
import com.karinca.app.data.local.entity.CategoryEntity
import com.karinca.app.data.local.entity.OutboxEntity
import com.karinca.app.data.sync.SyncTable
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val db: KarincaDatabase,
    private val categoryDao: CategoryDao,
    private val outboxDao: OutboxDao,
) {
    fun observeSelectable(userId: String): Flow<List<CategoryEntity>> =
        categoryDao.observeSelectable(userId)

    suspend fun get(id: String): CategoryEntity? = categoryDao.get(id)

    suspend fun create(
        userId: String,
        kind: CategoryKind,
        name: String,
        color: String,
        icon: String,
    ): String {
        val now = Instant.now()
        val entity = CategoryEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            kind = kind,
            name = name,
            color = color,
            icon = icon,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
        )
        persistAndEnqueue(entity, now)
        return entity.id
    }

    suspend fun update(id: String, userId: String, name: String, color: String, icon: String) {
        val existing = requireOwned(id, userId)
        val now = Instant.now()
        persistAndEnqueue(existing.copy(name = name, color = color, icon = icon, updatedAt = now), now)
    }

    suspend fun softDelete(id: String, userId: String) {
        val existing = requireOwned(id, userId)
        val now = Instant.now()
        persistAndEnqueue(existing.copy(deletedAt = now, updatedAt = now), now)
    }

    // System default categories (userId == null) are seeded/managed server-side and
    // are not writable through this repository — RLS would reject the push anyway,
    // but failing fast locally avoids a push that retries forever.
    private suspend fun requireOwned(id: String, userId: String): CategoryEntity {
        val existing = categoryDao.get(id)
        require(existing != null && existing.userId == userId) {
            "Category $id is not owned by $userId"
        }
        return existing
    }

    private suspend fun persistAndEnqueue(entity: CategoryEntity, now: Instant) {
        db.withTransaction {
            categoryDao.upsert(entity)
            outboxDao.enqueue(
                OutboxEntity(
                    tableName = SyncTable.CATEGORIES.remoteName,
                    entityId = entity.id,
                    queuedAt = now,
                )
            )
        }
    }
}
