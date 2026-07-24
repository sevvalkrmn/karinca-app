package com.karinca.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.karinca.app.data.local.dao.WeeklyReportDao
import com.karinca.app.data.local.entity.WeeklyReportEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// weekly_reports is a read-only mirror (written only by the server-side Python/FastAPI
// coach service) — this repository only ever reads it, plus tracks which report the user
// has already seen the "new report" envelope reveal for, so that reveal only ever plays
// once per report rather than on every app open.
@Singleton
class WeeklyReportRepository @Inject constructor(
    private val weeklyReportDao: WeeklyReportDao,
    private val dataStore: DataStore<Preferences>,
) {
    private val lastSeenReportIdKey = stringPreferencesKey("coach_last_seen_report_id")

    // observeAll is already ordered by weekStart DESC, so the first emission is the latest
    // report — no separate "latest" DAO query needed.
    fun observeLatest(userId: String): Flow<WeeklyReportEntity?> =
        weeklyReportDao.observeAll(userId).map { it.firstOrNull() }

    suspend fun lastSeenReportId(): String? = dataStore.data.first()[lastSeenReportIdKey]

    suspend fun markSeen(reportId: String) {
        dataStore.edit { prefs -> prefs[lastSeenReportIdKey] = reportId }
    }
}
