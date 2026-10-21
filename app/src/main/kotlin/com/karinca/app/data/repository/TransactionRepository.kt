package com.karinca.app.data.repository

import androidx.room.withTransaction
import com.karinca.app.data.local.CategoryKind
import com.karinca.app.data.local.KarincaDatabase
import com.karinca.app.data.local.dao.CategoryTotal
import com.karinca.app.data.local.dao.CategoryUsageCount
import com.karinca.app.data.local.dao.OutboxDao
import com.karinca.app.data.local.dao.TransactionDao
import com.karinca.app.data.local.entity.OutboxEntity
import com.karinca.app.data.local.entity.TransactionEntity
import com.karinca.app.data.sync.SyncTable
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val db: KarincaDatabase,
    private val transactionDao: TransactionDao,
    private val outboxDao: OutboxDao,
) {
    fun observeBetween(userId: String, from: LocalDate, to: LocalDate): Flow<List<TransactionEntity>> =
        transactionDao.observeBetween(userId, from, to)

    fun observeTotal(userId: String, type: CategoryKind, from: LocalDate, to: LocalDate): Flow<Double> =
        transactionDao.observeTotal(userId, type, from, to)

    fun observeCategoryTotals(userId: String, type: CategoryKind, from: LocalDate, to: LocalDate): Flow<List<CategoryTotal>> =
        transactionDao.observeCategoryTotals(userId, type, from, to)

    fun observeRecentByCategory(userId: String, categoryId: String, limit: Int = 20): Flow<List<TransactionEntity>> =
        transactionDao.observeRecentByCategory(userId, categoryId, limit)

    fun observeAllTimeTotal(userId: String, type: CategoryKind): Flow<Double> =
        transactionDao.observeAllTimeTotal(userId, type)

    fun observeCategoryUsageCounts(userId: String): Flow<List<CategoryUsageCount>> =
        transactionDao.observeCategoryUsageCounts(userId)

    fun observeRecentAll(userId: String, limit: Int = 5): Flow<List<TransactionEntity>> =
        transactionDao.observeRecentAll(userId, limit)

    fun observeAll(userId: String): Flow<List<TransactionEntity>> = transactionDao.observeAll(userId)

    suspend fun get(id: String): TransactionEntity? = transactionDao.get(id)

    suspend fun create(
        userId: String,
        categoryId: String,
        type: CategoryKind,
        amount: Double,
        occurredAt: LocalDate,
        note: String?,
    ): String {
        val now = Instant.now()
        val entity = TransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            categoryId = categoryId,
            type = type,
            amount = amount,
            occurredAt = occurredAt,
            note = note,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
        )
        persistAndEnqueue(entity, now)
        return entity.id
    }

    suspend fun update(
        id: String,
        userId: String,
        categoryId: String,
        type: CategoryKind,
        amount: Double,
        occurredAt: LocalDate,
        note: String?,
    ) {
        val existing = requireOwned(id, userId)
        val now = Instant.now()
        persistAndEnqueue(
            existing.copy(
                categoryId = categoryId,
                type = type,
                amount = amount,
                occurredAt = occurredAt,
                note = note,
                updatedAt = now,
            ),
            now,
        )
    }

    suspend fun softDelete(id: String, userId: String) {
        val existing = requireOwned(id, userId)
        val now = Instant.now()
        persistAndEnqueue(existing.copy(deletedAt = now, updatedAt = now), now)
    }

    private suspend fun requireOwned(id: String, userId: String): TransactionEntity {
        val existing = transactionDao.get(id)
        require(existing != null && existing.userId == userId) {
            "Transaction $id is not owned by $userId"
        }
        return existing
    }

    private suspend fun persistAndEnqueue(entity: TransactionEntity, now: Instant) {
        db.withTransaction {
            transactionDao.upsert(entity)
            outboxDao.enqueue(
                OutboxEntity(
                    tableName = SyncTable.TRANSACTIONS.remoteName,
                    entityId = entity.id,
                    queuedAt = now,
                )
            )
        }
    }
}
