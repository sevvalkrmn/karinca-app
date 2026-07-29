package com.karinca.app.data.repository

import androidx.room.withTransaction
import com.karinca.app.data.local.CoachTone
import com.karinca.app.data.local.KarincaDatabase
import com.karinca.app.data.local.ReferenceType
import com.karinca.app.data.local.dao.OutboxDao
import com.karinca.app.data.local.dao.ProfileDao
import com.karinca.app.data.local.entity.OutboxEntity
import com.karinca.app.data.local.entity.ProfileEntity
import com.karinca.app.data.remote.dto.ProfileDto
import com.karinca.app.data.sync.SyncTable
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CancellationException
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
    private val postgrest: Postgrest,
) {
    fun observe(userId: String): Flow<ProfileEntity?> = profileDao.observe(userId)

    suspend fun get(userId: String): ProfileEntity? = profileDao.get(userId)

    // Used only for the KVKK-consent -> Onboarding vs Home routing decision. Trusting
    // local Room there could send an already-onboarded account — signed in on a new
    // device that hasn't synced yet — back through onboarding, risking overwriting
    // their real profile with fresh answers. A direct server check avoids that; every
    // other read in this repository stays local-first as normal. Falls back to the
    // local cache if the server can't be reached, so a legitimate offline cold start
    // isn't blocked by a network call that isn't essential to it.
    suspend fun hasProfileSetup(userId: String): Boolean = try {
        postgrest.from(SyncTable.PROFILES.remoteName)
            .select { filter { eq("id", userId) } }
            .decodeList<ProfileDto>()
            .isNotEmpty()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        get(userId) != null
    }

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
        // Only meaningful the very first time a profile is ever saved (onboarding) —
        // ProfileSetupViewModel's later edits never pass this, so it stays null there.
        initialAlreadySpentAmount: Double? = null,
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
            initialAlreadySpentAmount = initialAlreadySpentAmount,
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
