package com.karinca.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.karinca.app.data.local.dao.CategoryDao
import com.karinca.app.data.local.dao.ConsentDao
import com.karinca.app.data.local.dao.GoalDao
import com.karinca.app.data.local.dao.OutboxDao
import com.karinca.app.data.local.dao.PiggyBankEntryDao
import com.karinca.app.data.local.dao.ProfileDao
import com.karinca.app.data.local.dao.RecurringPaymentDao
import com.karinca.app.data.local.dao.TransactionDao
import com.karinca.app.data.local.dao.WeeklyReportDao
import com.karinca.app.data.local.entity.CategoryEntity
import com.karinca.app.data.local.entity.ConsentEntity
import com.karinca.app.data.local.entity.GoalEntity
import com.karinca.app.data.local.entity.OutboxEntity
import com.karinca.app.data.local.entity.PiggyBankEntryEntity
import com.karinca.app.data.local.entity.ProfileEntity
import com.karinca.app.data.local.entity.RecurringPaymentEntity
import com.karinca.app.data.local.entity.TransactionEntity
import com.karinca.app.data.local.entity.WeeklyReportEntity

// Bump alongside the @Database version whenever a schema change relies on
// fallbackToDestructiveMigration() — SyncWorker compares this to the last-seen value it
// stored in DataStore and resets all sync cursors when they differ. Without this, a
// destructive wipe empties Room but leaves the DataStore cursors pointing past
// already-synced rows, so those rows are silently skipped on the next pull instead of
// being re-downloaded from Supabase. (Top-level, not a KarincaDatabase.VERSION companion
// constant, so it can be used in this same class's @Database annotation below.)
internal const val KARINCA_DB_VERSION = 9

@Database(
    entities = [
        ProfileEntity::class,
        ConsentEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        WeeklyReportEntity::class,
        GoalEntity::class,
        PiggyBankEntryEntity::class,
        RecurringPaymentEntity::class,
        OutboxEntity::class,
    ],
    version = KARINCA_DB_VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class KarincaDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun consentDao(): ConsentDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun weeklyReportDao(): WeeklyReportDao
    abstract fun goalDao(): GoalDao
    abstract fun piggyBankEntryDao(): PiggyBankEntryDao
    abstract fun recurringPaymentDao(): RecurringPaymentDao
    abstract fun outboxDao(): OutboxDao
}
