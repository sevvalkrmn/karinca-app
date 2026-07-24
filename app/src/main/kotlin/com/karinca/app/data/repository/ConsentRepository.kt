package com.karinca.app.data.repository

import androidx.room.withTransaction
import com.karinca.app.data.local.KarincaDatabase
import com.karinca.app.data.local.dao.ConsentDao
import com.karinca.app.data.local.dao.OutboxDao
import com.karinca.app.data.local.entity.ConsentEntity
import com.karinca.app.data.local.entity.OutboxEntity
import com.karinca.app.data.sync.SyncTable
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsentRepository @Inject constructor(
    private val db: KarincaDatabase,
    private val consentDao: ConsentDao,
    private val outboxDao: OutboxDao,
) {
    fun observe(userId: String): Flow<ConsentEntity?> = consentDao.observe(userId)

    suspend fun get(userId: String): ConsentEntity? = consentDao.get(userId)

    // The caller supplies requiredVersion — the current KVKK policy text version is
    // product content, not something this layer should hardcode.
    suspend fun hasConsented(userId: String, requiredVersion: String): Boolean =
        consentDao.get(userId)?.kvkkConsentVersion == requiredVersion

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
