package com.karinca.app.data.repository

import androidx.room.withTransaction
import com.karinca.app.data.local.KarincaDatabase
import com.karinca.app.data.local.PiggyBankEntryReason
import com.karinca.app.data.local.dao.OutboxDao
import com.karinca.app.data.local.dao.PiggyBankEntryDao
import com.karinca.app.data.local.entity.OutboxEntity
import com.karinca.app.data.local.entity.PiggyBankEntryEntity
import com.karinca.app.data.sync.SyncTable
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PiggyBankRepository @Inject constructor(
    private val db: KarincaDatabase,
    private val piggyBankEntryDao: PiggyBankEntryDao,
    private val outboxDao: OutboxDao,
) {
    fun observeBalance(userId: String): Flow<Double> = piggyBankEntryDao.observeBalance(userId)

    fun observeEntries(userId: String): Flow<List<PiggyBankEntryEntity>> = piggyBankEntryDao.observeAll(userId)

    // The only write this client ever makes to the Kumbara ledger — everything else
    // (salary_limit_diff, monthly_leftover) is deposited server-side by the AI
    // service's monthly job. amount is stored negative; caller passes the positive
    // overage being withdrawn.
    suspend fun recordOverspendWithdrawal(userId: String, overage: Double) {
        val now = Instant.now()
        val entity = PiggyBankEntryEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            amount = -overage,
            reason = PiggyBankEntryReason.OVERSPEND_WITHDRAWAL,
            createdAt = now,
            updatedAt = now,
        )
        db.withTransaction {
            piggyBankEntryDao.upsert(entity)
            outboxDao.enqueue(
                OutboxEntity(
                    tableName = SyncTable.PIGGY_BANK_ENTRIES.remoteName,
                    entityId = entity.id,
                    queuedAt = now,
                )
            )
        }
    }
}
