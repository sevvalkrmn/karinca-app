package com.karinca.app.di

import android.content.Context
import androidx.room.Room
import com.karinca.app.data.local.KarincaDatabase
import com.karinca.app.data.local.dao.CategoryDao
import com.karinca.app.data.local.dao.ConsentDao
import com.karinca.app.data.local.dao.EntitlementDao
import com.karinca.app.data.local.dao.GoalDao
import com.karinca.app.data.local.dao.OutboxDao
import com.karinca.app.data.local.dao.PiggyBankEntryDao
import com.karinca.app.data.local.dao.ProfileDao
import com.karinca.app.data.local.dao.RecurringPaymentDao
import com.karinca.app.data.local.dao.TransactionDao
import com.karinca.app.data.local.dao.WeeklyReportDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    // Local Room is a re-derivable sync cache (source of truth is Supabase once synced),
    // and the app has no shipped users yet — destructive fallback on schema bumps avoids
    // hand-writing migrations during active development.
    fun provideDatabase(@ApplicationContext context: Context): KarincaDatabase =
        Room.databaseBuilder(context, KarincaDatabase::class.java, "karinca.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideProfileDao(db: KarincaDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideConsentDao(db: KarincaDatabase): ConsentDao = db.consentDao()

    @Provides
    fun provideCategoryDao(db: KarincaDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideTransactionDao(db: KarincaDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideWeeklyReportDao(db: KarincaDatabase): WeeklyReportDao = db.weeklyReportDao()

    @Provides
    fun provideGoalDao(db: KarincaDatabase): GoalDao = db.goalDao()

    @Provides
    fun providePiggyBankEntryDao(db: KarincaDatabase): PiggyBankEntryDao = db.piggyBankEntryDao()

    @Provides
    fun provideRecurringPaymentDao(db: KarincaDatabase): RecurringPaymentDao = db.recurringPaymentDao()

    @Provides
    fun provideEntitlementDao(db: KarincaDatabase): EntitlementDao = db.entitlementDao()

    @Provides
    fun provideOutboxDao(db: KarincaDatabase): OutboxDao = db.outboxDao()
}
