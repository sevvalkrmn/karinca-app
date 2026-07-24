package com.karinca.app.data.repository

import androidx.room.withTransaction
import com.karinca.app.data.local.CategoryKind
import com.karinca.app.data.local.KarincaDatabase
import com.karinca.app.data.local.dao.GoalDao
import com.karinca.app.data.local.dao.OutboxDao
import com.karinca.app.data.local.entity.GoalEntity
import com.karinca.app.data.local.entity.OutboxEntity
import com.karinca.app.data.sync.SyncTable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(
    private val db: KarincaDatabase,
    private val goalDao: GoalDao,
    private val outboxDao: OutboxDao,
    private val transactionRepository: TransactionRepository,
) {
    fun observeActive(userId: String): Flow<List<GoalEntity>> = goalDao.observeActive(userId)

    suspend fun get(id: String): GoalEntity? = goalDao.get(id)

    suspend fun create(
        userId: String,
        name: String,
        icon: String,
        targetAmount: Double,
        initialAmount: Double,
        targetDate: LocalDate,
    ): String {
        val now = Instant.now()
        val entity = GoalEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = name,
            icon = icon,
            targetAmount = targetAmount,
            currentAmount = initialAmount,
            targetDate = targetDate,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            completedAt = null,
            lastEvaluatedDate = null,
        )
        persistAndEnqueue(entity, now)
        return entity.id
    }

    suspend fun updateDetails(
        id: String,
        userId: String,
        name: String,
        targetAmount: Double,
        currentAmount: Double,
        targetDate: LocalDate,
    ) {
        val existing = requireOwned(id, userId)
        val now = Instant.now()
        persistAndEnqueue(
            existing.copy(
                name = name,
                targetAmount = targetAmount,
                currentAmount = currentAmount,
                targetDate = targetDate,
                updatedAt = now,
            ),
            now,
        )
    }

    suspend fun addContribution(id: String, userId: String, amount: Double) {
        val existing = requireOwned(id, userId)
        val now = Instant.now()
        persistAndEnqueue(existing.copy(currentAmount = existing.currentAmount + amount, updatedAt = now), now)
    }

    suspend fun complete(id: String, userId: String) {
        val existing = requireOwned(id, userId)
        val now = Instant.now()
        persistAndEnqueue(existing.copy(completedAt = now, updatedAt = now), now)
    }

    suspend fun softDelete(id: String, userId: String) {
        val existing = requireOwned(id, userId)
        val now = Instant.now()
        persistAndEnqueue(existing.copy(deletedAt = now, updatedAt = now), now)
    }

    private suspend fun requireOwned(id: String, userId: String): GoalEntity {
        val existing = goalDao.get(id)
        require(existing != null && existing.userId == userId) {
            "Goal $id is not owned by $userId"
        }
        return existing
    }

    private suspend fun persistAndEnqueue(entity: GoalEntity, now: Instant) {
        db.withTransaction {
            goalDao.upsert(entity)
            outboxDao.enqueue(
                OutboxEntity(
                    tableName = SyncTable.GOALS.remoteName,
                    entityId = entity.id,
                    queuedAt = now,
                )
            )
        }
    }

    // Ratchets each active goal's real, verified progress forward by however many full
    // days have elapsed since it was last checked — but only for days the user's actual
    // balance can still back. Oldest goal first, sharing one running balance pool, so an
    // older goal's already-confirmed amount is never silently double-claimed by a newer
    // one. A goal whose already-confirmed amount (or a newly-elapsed day) can no longer be
    // covered is deleted outright; its name comes back so the UI can say so.
    suspend fun evaluateProgress(userId: String): List<String> {
        val today = LocalDate.now()
        val goals = goalDao.observeActive(userId).first().sortedBy { it.createdAt }
        if (goals.isEmpty()) return emptyList()

        val income = transactionRepository.observeAllTimeTotal(userId, CategoryKind.INCOME).first()
        val expense = transactionRepository.observeAllTimeTotal(userId, CategoryKind.EXPENSE).first()
        var remainingBalance = income - expense
        val failedNames = mutableListOf<String>()

        for (goal in goals) {
            val startDate = goal.createdAt.atZone(ZoneId.systemDefault()).toLocalDate()
            val totalDays = ChronoUnit.DAYS.between(startDate, goal.targetDate).coerceAtLeast(1)
            val dailyRate = goal.targetAmount / totalDays
            val daysElapsed = ChronoUnit.DAYS.between(startDate, today).coerceIn(0, totalDays)
            val daysConfirmed = ChronoUnit.DAYS.between(startDate, goal.lastEvaluatedDate ?: startDate)

            // What this goal already claimed in past passes must still be real money.
            remainingBalance -= goal.currentAmount
            if (remainingBalance < 0.0) {
                softDelete(goal.id, userId)
                failedNames += goal.name
                continue
            }

            var confirmedAmount = goal.currentAmount
            var lastEvaluated = goal.lastEvaluatedDate ?: startDate
            var failed = false
            for (dayNum in (daysConfirmed + 1)..daysElapsed) {
                if (remainingBalance < dailyRate) {
                    failed = true
                    break
                }
                remainingBalance -= dailyRate
                confirmedAmount += dailyRate
                lastEvaluated = startDate.plusDays(dayNum)
            }

            if (failed) {
                softDelete(goal.id, userId)
                failedNames += goal.name
            } else if (lastEvaluated != goal.lastEvaluatedDate) {
                val now = Instant.now()
                persistAndEnqueue(
                    goal.copy(currentAmount = confirmedAmount, lastEvaluatedDate = lastEvaluated, updatedAt = now),
                    now,
                )
            }
        }
        return failedNames
    }
}
