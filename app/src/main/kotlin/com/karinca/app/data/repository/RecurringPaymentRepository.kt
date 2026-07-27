package com.karinca.app.data.repository

import androidx.room.withTransaction
import com.karinca.app.data.local.KarincaDatabase
import com.karinca.app.data.local.dao.OutboxDao
import com.karinca.app.data.local.dao.RecurringPaymentDao
import com.karinca.app.data.local.entity.OutboxEntity
import com.karinca.app.data.local.entity.RecurringPaymentEntity
import com.karinca.app.data.sync.SyncTable
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringPaymentRepository @Inject constructor(
    private val db: KarincaDatabase,
    private val recurringPaymentDao: RecurringPaymentDao,
    private val outboxDao: OutboxDao,
) {
    fun observeActive(userId: String): Flow<List<RecurringPaymentEntity>> = recurringPaymentDao.observeActive(userId)

    // Called alongside the normal expense transaction when the "tekrar eden ödeme"
    // checkbox is on — this manual entry IS this month's payment; the AI service's
    // monthly job only starts auto-creating from next month on.
    suspend fun create(userId: String, name: String, amount: Double, categoryId: String?, dayOfMonth: Int): String {
        val now = Instant.now()
        val entity = RecurringPaymentEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = name,
            amount = amount,
            categoryId = categoryId,
            dayOfMonth = dayOfMonth.coerceIn(1, 28),
            active = true,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
        )
        persistAndEnqueue(entity, now)
        return entity.id
    }

    // Cancel flips active to false rather than deleting, so past auto-created
    // transactions stay attributable to something; the monthly job simply stops
    // picking it up.
    suspend fun cancel(id: String, userId: String) {
        val existing = recurringPaymentDao.get(id)
        require(existing != null && existing.userId == userId) {
            "Recurring payment $id is not owned by $userId"
        }
        val now = Instant.now()
        persistAndEnqueue(existing.copy(active = false, updatedAt = now), now)
    }

    private suspend fun persistAndEnqueue(entity: RecurringPaymentEntity, now: Instant) {
        db.withTransaction {
            recurringPaymentDao.upsert(entity)
            outboxDao.enqueue(
                OutboxEntity(
                    tableName = SyncTable.RECURRING_PAYMENTS.remoteName,
                    entityId = entity.id,
                    queuedAt = now,
                )
            )
        }
    }
}
