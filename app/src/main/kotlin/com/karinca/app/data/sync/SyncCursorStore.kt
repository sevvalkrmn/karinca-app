package com.karinca.app.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

// One typed key per table (not a single JSON blob) — each table's cursor advances
// independently with no read-modify-write race touching unrelated tables.
class SyncCursorStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private fun keyFor(table: SyncTable) = longPreferencesKey("cursor_${table.remoteName}")
    private val schemaVersionKey = intPreferencesKey("local_schema_version")

    suspend fun get(table: SyncTable): Instant {
        val epochMilli = dataStore.data.first()[keyFor(table)] ?: 0L
        return Instant.ofEpochMilli(epochMilli)
    }

    suspend fun advance(table: SyncTable, newCursor: Instant) {
        dataStore.edit { prefs ->
            val current = prefs[keyFor(table)] ?: 0L
            if (newCursor.toEpochMilli() > current) {
                prefs[keyFor(table)] = newCursor.toEpochMilli()
            }
        }
    }

    // Room's fallbackToDestructiveMigration() wipes local data on a version bump without
    // touching this DataStore-backed cursor, so a stale cursor would make the next pull
    // think already-synced rows are still up to date and skip re-downloading them. Called
    // once at the top of every sync cycle; a no-op once the stored version catches up.
    suspend fun resetIfSchemaChanged(currentVersion: Int) {
        val storedVersion = dataStore.data.first()[schemaVersionKey]
        if (storedVersion != currentVersion) {
            dataStore.edit { prefs ->
                SyncTable.entries.forEach { prefs.remove(keyFor(it)) }
                prefs[schemaVersionKey] = currentVersion
            }
        }
    }

    // Called on sign-out. Cursors are unscoped by user (one DataStore per device, not
    // per account), so leaving a stale cursor behind after signing out would make the
    // next sign-in on this device — same account or a different one — silently skip
    // pulling any row whose server_updated_at falls before that leftover cursor. Reset
    // to zero so the next sync after any sign-in is always a full pull.
    suspend fun resetAll() {
        dataStore.edit { prefs ->
            SyncTable.entries.forEach { prefs.remove(keyFor(it)) }
        }
    }
}
