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
import kotlinx.coroutines.flow.first
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

    // One of two writes this client ever makes to the Kumbara ledger — everything
    // else (salary_limit_diff, monthly_leftover) is deposited server-side by the AI
    // service's monthly job. amount is stored negative; overage is the positive
    // amount the overspend would need. Never withdraws more than the Kumbara
    // actually has — the uncovered remainder isn't tracked here at all, since the
    // real expense transaction has already reduced the real balance by its full
    // amount regardless of what the Kumbara can cover. Tagged with transactionId so
    // deleting that transaction can find and reverse it later.
    suspend fun recordOverspendWithdrawal(userId: String, overage: Double, transactionId: String) {
        val currentBalance = piggyBankEntryDao.observeBalance(userId).first()
        val amountToWithdraw = minOf(overage, currentBalance).coerceAtLeast(0.0)
        if (amountToWithdraw <= 0.0) return

        insertEntry(userId, -amountToWithdraw, PiggyBankEntryReason.OVERSPEND_WITHDRAWAL, transactionId)
    }

    // Called when a transaction is deleted — if it ever triggered an overspend
    // withdrawal, undoes it with a compensating deposit (the ledger is append-only;
    // the original withdrawal is never edited or removed). A no-op for the vast
    // majority of deletions, which never withdrew anything, and safe to call twice
    // (checks for an existing reversal first).
    suspend fun reverseOverspendWithdrawalIfAny(userId: String, transactionId: String) {
        val entries = piggyBankEntryDao.getByTransactionId(transactionId)
        val withdrawal = entries.firstOrNull { it.reason == PiggyBankEntryReason.OVERSPEND_WITHDRAWAL } ?: return
        val alreadyReversed = entries.any { it.reason == PiggyBankEntryReason.OVERSPEND_REVERSAL }
        if (alreadyReversed) return

        insertEntry(userId, -withdrawal.amount, PiggyBankEntryReason.OVERSPEND_REVERSAL, transactionId)
    }

    private suspend fun insertEntry(userId: String, amount: Double, reason: PiggyBankEntryReason, transactionId: String?) {
        val now = Instant.now()
        val entity = PiggyBankEntryEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            amount = amount,
            reason = reason,
            transactionId = transactionId,
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
