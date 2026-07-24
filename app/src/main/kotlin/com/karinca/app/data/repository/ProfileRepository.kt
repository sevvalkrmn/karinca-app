package com.karinca.app.data.repository

import androidx.room.withTransaction
import com.karinca.app.data.local.CoachTone
import com.karinca.app.data.local.KarincaDatabase
import com.karinca.app.data.local.ReferenceType
import com.karinca.app.data.local.dao.OutboxDao
import com.karinca.app.data.local.dao.ProfileDao
import com.karinca.app.data.local.entity.OutboxEntity
import com.karinca.app.data.local.entity.ProfileEntity
import com.karinca.app.data.sync.SyncTable
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val db: KarincaDatabase,
    private val profileDao: ProfileDao,
    private val outboxDao: OutboxDao,
) {
    fun observe(userId: String): Flow<ProfileEntity?> = profileDao.observe(userId)

    suspend fun get(userId: String): ProfileEntity? = profileDao.get(userId)

    // profiles.id == auth user id, so this is always an upsert. referenceSetAt only
    // advances when the goal/limit itself changes, not on every unrelated edit (e.g.
    // just switching coach tone shouldn't reset "when did I set this goal").
    suspend fun save(
        userId: String,
        referenceType: ReferenceType,
        referenceAmount: Double,
        targetDate: LocalDate?,
        regularMonthlyIncome: Double,
        variableMonthlyIncome: Double?,
        coachTone: CoachTone,
    ) {
        val now = Instant.now()
        val existing = profileDao.get(userId)
        val referenceUnchanged = existing != null &&
            existing.referenceType == referenceType &&
            existing.referenceAmount == referenceAmount

        val entity = ProfileEntity(
            id = userId,
            referenceType = referenceType,
            referenceAmount = referenceAmount,
            targetDate = targetDate,
            referenceSetAt = if (referenceUnchanged) existing!!.referenceSetAt else now,
            regularMonthlyIncome = regularMonthlyIncome,
            variableMonthlyIncome = variableMonthlyIncome,
            coachTone = coachTone,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )

        db.withTransaction {
            profileDao.upsert(entity)
            outboxDao.enqueue(
                OutboxEntity(
                    tableName = SyncTable.PROFILES.remoteName,
                    entityId = entity.id,
                    queuedAt = now,
                )
            )
        }
    }
}
