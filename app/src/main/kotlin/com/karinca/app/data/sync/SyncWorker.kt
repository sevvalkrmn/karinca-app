package com.karinca.app.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.karinca.app.data.local.KARINCA_DB_VERSION
import com.karinca.app.data.local.dao.CategoryDao
import com.karinca.app.data.local.dao.ConsentDao
import com.karinca.app.data.local.dao.GoalDao
import com.karinca.app.data.local.dao.OutboxDao
import com.karinca.app.data.local.dao.PiggyBankEntryDao
import com.karinca.app.data.local.dao.ProfileDao
import com.karinca.app.data.local.dao.RecurringPaymentDao
import com.karinca.app.data.local.dao.TransactionDao
import com.karinca.app.data.local.dao.WeeklyReportDao
import com.karinca.app.data.remote.dto.CategoryDto
import com.karinca.app.data.remote.dto.ConsentDto
import com.karinca.app.data.remote.dto.GoalDto
import com.karinca.app.data.remote.dto.PiggyBankEntryDto
import com.karinca.app.data.remote.dto.ProfileDto
import com.karinca.app.data.remote.dto.RecurringPaymentDto
import com.karinca.app.data.remote.dto.TransactionDto
import com.karinca.app.data.remote.dto.WeeklyReportDto
import com.karinca.app.data.remote.parseInstant
import com.karinca.app.data.remote.toDto
import com.karinca.app.data.remote.toEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant

/**
 * One job, not four: push always runs before pull, and pull processes tables in a
 * fixed order (categories before transactions) so local FK integrity holds when a
 * transaction referencing a brand-new category arrives in the same sync cycle.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val postgrest: Postgrest,
    private val outboxDao: OutboxDao,
    private val profileDao: ProfileDao,
    private val consentDao: ConsentDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val weeklyReportDao: WeeklyReportDao,
    private val goalDao: GoalDao,
    private val piggyBankEntryDao: PiggyBankEntryDao,
    private val recurringPaymentDao: RecurringPaymentDao,
    private val cursorStore: SyncCursorStore,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            cursorStore.resetIfSchemaChanged(KARINCA_DB_VERSION)
            pushOutbox()
            pullProfiles()
            pullConsents()
            pullCategories()
            pullTransactions()
            pullWeeklyReports()
            pullGoals()
            pullPiggyBankEntries()
            pullRecurringPayments()
            Log.d(TAG, "Sync succeeded")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            Result.retry()
        }
    }

    private companion object {
        const val TAG = "SyncWorker"
    }

    private suspend fun pushOutbox() {
        for (entry in outboxDao.getAll()) {
            try {
                when (entry.tableName) {
                    SyncTable.PROFILES.remoteName -> {
                        val row = profileDao.get(entry.entityId) ?: continue
                        postgrest.from(SyncTable.PROFILES.remoteName).upsert(row.toDto())
                    }
                    SyncTable.CONSENTS.remoteName -> {
                        val row = consentDao.get(entry.entityId) ?: continue
                        postgrest.from(SyncTable.CONSENTS.remoteName).upsert(row.toDto())
                    }
                    SyncTable.CATEGORIES.remoteName -> {
                        val row = categoryDao.get(entry.entityId) ?: continue
                        postgrest.from(SyncTable.CATEGORIES.remoteName).upsert(row.toDto())
                    }
                    SyncTable.TRANSACTIONS.remoteName -> {
                        val row = transactionDao.get(entry.entityId) ?: continue
                        postgrest.from(SyncTable.TRANSACTIONS.remoteName).upsert(row.toDto())
                    }
                    SyncTable.GOALS.remoteName -> {
                        val row = goalDao.get(entry.entityId) ?: continue
                        postgrest.from(SyncTable.GOALS.remoteName).upsert(row.toDto())
                    }
                    SyncTable.PIGGY_BANK_ENTRIES.remoteName -> {
                        val row = piggyBankEntryDao.get(entry.entityId) ?: continue
                        postgrest.from(SyncTable.PIGGY_BANK_ENTRIES.remoteName).upsert(row.toDto())
                    }
                    SyncTable.RECURRING_PAYMENTS.remoteName -> {
                        val row = recurringPaymentDao.get(entry.entityId) ?: continue
                        postgrest.from(SyncTable.RECURRING_PAYMENTS.remoteName).upsert(row.toDto())
                    }
                }
                outboxDao.remove(entry.tableName, entry.entityId)
            } catch (e: Exception) {
                Log.w(TAG, "Push failed for ${entry.tableName}/${entry.entityId}", e)
                outboxDao.recordFailure(entry.tableName, entry.entityId, e.message)
            }
        }
    }

    private suspend fun pullProfiles() {
        val cursor = cursorStore.get(SyncTable.PROFILES)
        var maxSeen = cursor
        val rows = postgrest.from(SyncTable.PROFILES.remoteName)
            .select {
                filter { gt("server_updated_at", cursor.toString()) }
                order("server_updated_at", Order.ASCENDING)
            }
            .decodeList<ProfileDto>()

        for (dto in rows) {
            mergeProfile(dto)
            dto.serverUpdatedAt?.let { maxSeen = maxOf(maxSeen, parseInstant(it)) }
        }
        if (maxSeen > cursor) cursorStore.advance(SyncTable.PROFILES, maxSeen)
    }

    private suspend fun pullConsents() {
        val cursor = cursorStore.get(SyncTable.CONSENTS)
        var maxSeen = cursor
        val rows = postgrest.from(SyncTable.CONSENTS.remoteName)
            .select {
                filter { gt("server_updated_at", cursor.toString()) }
                order("server_updated_at", Order.ASCENDING)
            }
            .decodeList<ConsentDto>()

        for (dto in rows) {
            mergeConsent(dto)
            dto.serverUpdatedAt?.let { maxSeen = maxOf(maxSeen, parseInstant(it)) }
        }
        if (maxSeen > cursor) cursorStore.advance(SyncTable.CONSENTS, maxSeen)
    }

    private suspend fun pullCategories() {
        val cursor = cursorStore.get(SyncTable.CATEGORIES)
        var maxSeen = cursor
        val rows = postgrest.from(SyncTable.CATEGORIES.remoteName)
            .select {
                filter { gt("server_updated_at", cursor.toString()) }
                order("server_updated_at", Order.ASCENDING)
            }
            .decodeList<CategoryDto>()

        Log.d(TAG, "pullCategories: cursor=$cursor fetched=${rows.size}")
        for (dto in rows) {
            mergeCategory(dto)
            dto.serverUpdatedAt?.let { maxSeen = maxOf(maxSeen, parseInstant(it)) }
        }
        if (maxSeen > cursor) cursorStore.advance(SyncTable.CATEGORIES, maxSeen)
    }

    private suspend fun pullTransactions() {
        val cursor = cursorStore.get(SyncTable.TRANSACTIONS)
        var maxSeen = cursor
        val rows = postgrest.from(SyncTable.TRANSACTIONS.remoteName)
            .select {
                filter { gt("server_updated_at", cursor.toString()) }
                order("server_updated_at", Order.ASCENDING)
            }
            .decodeList<TransactionDto>()

        for (dto in rows) {
            mergeTransaction(dto)
            dto.serverUpdatedAt?.let { maxSeen = maxOf(maxSeen, parseInstant(it)) }
        }
        if (maxSeen > cursor) cursorStore.advance(SyncTable.TRANSACTIONS, maxSeen)
    }

    private suspend fun pullWeeklyReports() {
        val cursor = cursorStore.get(SyncTable.WEEKLY_REPORTS)
        var maxSeen = cursor
        val rows = postgrest.from(SyncTable.WEEKLY_REPORTS.remoteName)
            .select {
                filter { gt("server_updated_at", cursor.toString()) }
                order("server_updated_at", Order.ASCENDING)
            }
            .decodeList<WeeklyReportDto>()

        Log.d(TAG, "pullWeeklyReports: cursor=$cursor fetched=${rows.size}")
        for (dto in rows) {
            // Read-only mirror: no local-write-wins comparison needed, no outbox entry to clear.
            weeklyReportDao.upsertAll(listOf(dto.toEntity()))
            dto.serverUpdatedAt?.let { maxSeen = maxOf(maxSeen, parseInstant(it)) }
        }
        if (maxSeen > cursor) cursorStore.advance(SyncTable.WEEKLY_REPORTS, maxSeen)
    }

    private suspend fun pullGoals() {
        val cursor = cursorStore.get(SyncTable.GOALS)
        var maxSeen = cursor
        val rows = postgrest.from(SyncTable.GOALS.remoteName)
            .select {
                filter { gt("server_updated_at", cursor.toString()) }
                order("server_updated_at", Order.ASCENDING)
            }
            .decodeList<GoalDto>()

        for (dto in rows) {
            mergeGoal(dto)
            dto.serverUpdatedAt?.let { maxSeen = maxOf(maxSeen, parseInstant(it)) }
        }
        if (maxSeen > cursor) cursorStore.advance(SyncTable.GOALS, maxSeen)
    }

    private suspend fun pullPiggyBankEntries() {
        val cursor = cursorStore.get(SyncTable.PIGGY_BANK_ENTRIES)
        var maxSeen = cursor
        val rows = postgrest.from(SyncTable.PIGGY_BANK_ENTRIES.remoteName)
            .select {
                filter { gt("server_updated_at", cursor.toString()) }
                order("server_updated_at", Order.ASCENDING)
            }
            .decodeList<PiggyBankEntryDto>()

        for (dto in rows) {
            mergePiggyBankEntry(dto)
            dto.serverUpdatedAt?.let { maxSeen = maxOf(maxSeen, parseInstant(it)) }
        }
        if (maxSeen > cursor) cursorStore.advance(SyncTable.PIGGY_BANK_ENTRIES, maxSeen)
    }

    private suspend fun pullRecurringPayments() {
        val cursor = cursorStore.get(SyncTable.RECURRING_PAYMENTS)
        var maxSeen = cursor
        val rows = postgrest.from(SyncTable.RECURRING_PAYMENTS.remoteName)
            .select {
                filter { gt("server_updated_at", cursor.toString()) }
                order("server_updated_at", Order.ASCENDING)
            }
            .decodeList<RecurringPaymentDto>()

        for (dto in rows) {
            mergeRecurringPayment(dto)
            dto.serverUpdatedAt?.let { maxSeen = maxOf(maxSeen, parseInstant(it)) }
        }
        if (maxSeen > cursor) cursorStore.advance(SyncTable.RECURRING_PAYMENTS, maxSeen)
    }

    // Pull-side conflict resolution: only overwrite the local row if the incoming
    // updated_at is at least as new as local. If local wins, its outbox entry (if any)
    // stays queued so the newer local edit still gets pushed. If remote wins, drop any
    // pending outbox entry for that row since the local edit is deliberately discarded.

    private suspend fun mergeProfile(dto: ProfileDto) {
        val local = profileDao.get(dto.id)
        val incoming = parseInstant(dto.updatedAt)
        if (local == null || incoming >= local.updatedAt) {
            profileDao.upsert(dto.toEntity())
            outboxDao.remove(SyncTable.PROFILES.remoteName, dto.id)
        }
    }

    private suspend fun mergeConsent(dto: ConsentDto) {
        val local = consentDao.get(dto.id)
        val incoming = parseInstant(dto.updatedAt)
        if (local == null || incoming >= local.updatedAt) {
            consentDao.upsert(dto.toEntity())
            outboxDao.remove(SyncTable.CONSENTS.remoteName, dto.id)
        }
    }

    private suspend fun mergeCategory(dto: CategoryDto) {
        val local = categoryDao.get(dto.id)
        val incoming = parseInstant(dto.updatedAt)
        if (local == null || incoming >= local.updatedAt) {
            categoryDao.upsert(dto.toEntity())
            outboxDao.remove(SyncTable.CATEGORIES.remoteName, dto.id)
        }
    }

    private suspend fun mergeTransaction(dto: TransactionDto) {
        val local = transactionDao.get(dto.id)
        val incoming = parseInstant(dto.updatedAt)
        if (local == null || incoming >= local.updatedAt) {
            transactionDao.upsert(dto.toEntity())
            outboxDao.remove(SyncTable.TRANSACTIONS.remoteName, dto.id)
        }
    }

    private suspend fun mergeGoal(dto: GoalDto) {
        val local = goalDao.get(dto.id)
        val incoming = parseInstant(dto.updatedAt)
        if (local == null || incoming >= local.updatedAt) {
            goalDao.upsert(dto.toEntity())
            outboxDao.remove(SyncTable.GOALS.remoteName, dto.id)
        }
    }

    // Append-only ledger — a row's content never changes once created (no update
    // policy exists), so there's no "which version wins" question, just an idempotent
    // upsert. Clearing the outbox entry here is a defensive no-op in the normal case
    // (push already cleared it) and a safety net if push succeeded but the local
    // outbox removal somehow didn't.
    private suspend fun mergePiggyBankEntry(dto: PiggyBankEntryDto) {
        piggyBankEntryDao.upsert(dto.toEntity())
        outboxDao.remove(SyncTable.PIGGY_BANK_ENTRIES.remoteName, dto.id)
    }

    private suspend fun mergeRecurringPayment(dto: RecurringPaymentDto) {
        val local = recurringPaymentDao.get(dto.id)
        val incoming = parseInstant(dto.updatedAt)
        if (local == null || incoming >= local.updatedAt) {
            recurringPaymentDao.upsert(dto.toEntity())
            outboxDao.remove(SyncTable.RECURRING_PAYMENTS.remoteName, dto.id)
        }
    }
}
