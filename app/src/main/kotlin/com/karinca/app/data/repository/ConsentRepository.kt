package com.karinca.app.data.repository

import androidx.room.withTransaction
import com.karinca.app.data.local.KarincaDatabase
import com.karinca.app.data.local.dao.ConsentDao
import com.karinca.app.data.local.dao.OutboxDao
import com.karinca.app.data.local.entity.ConsentEntity
import com.karinca.app.data.local.entity.OutboxEntity
import com.karinca.app.data.remote.dto.ConsentDto
import com.karinca.app.data.sync.SyncTable
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsentRepository @Inject constructor(
    private val db: KarincaDatabase,
    private val consentDao: ConsentDao,
    private val outboxDao: OutboxDao,
    private val postgrest: Postgrest,
) {
    fun observe(userId: String): Flow<ConsentEntity?> = consentDao.observe(userId)

    suspend fun get(userId: String): ConsentEntity? = consentDao.get(userId)

    // Used only for the KVKK-consent -> Onboarding/Home routing decision. signOut() wipes
    // local Room, so right after signing back in (or on a second, not-yet-synced device)
    // the local cache is empty even though the account already consented server-side —
    // trusting it there would wrongly re-show the KVKK screen (and, worse, re-run
    // onboarding on top of real data once past it). A direct server check avoids that,
    // same reasoning as ProfileRepository.hasProfileSetup; every other read here stays
    // local-first. Falls back to the local cache if the server can't be reached, so a
    // legitimate offline cold start isn't blocked by a network call that isn't essential.
    suspend fun hasConsented(userId: String, requiredVersion: String): Boolean = try {
        postgrest.from(SyncTable.CONSENTS.remoteName)
            .select { filter { eq("id", userId) } }
            .decodeList<ConsentDto>()
            .any { it.kvkkConsentVersion == requiredVersion }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        consentDao.get(userId)?.kvkkConsentVersion == requiredVersion
    }

    suspend fun recordConsent(userId: String, consentVersion: String) {
        val now = Instant.now()
        val existing = consentDao.get(userId)
        val entity = ConsentEntity(
            id = userId,
            kvkkConsentAt = now,
            kvkkConsentVersion = consentVersion,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )

        db.withTransaction {
            consentDao.upsert(entity)
            outboxDao.enqueue(
                OutboxEntity(
                    tableName = SyncTable.CONSENTS.remoteName,
                    entityId = entity.id,
                    queuedAt = now,
                )
            )
        }
    }
}
