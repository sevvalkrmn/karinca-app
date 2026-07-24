package com.karinca.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.karinca.app.data.local.entity.OutboxEntity

@Dao
interface OutboxDao {

    // REPLACE + the unique (tableName, entityId) index means repeated local edits
    // before the next sync cycle collapse to one pending push, with a fresh queuedAt
    // and a clean retry state — safe because the worker always re-reads live state.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(entry: OutboxEntity)

    @Query("SELECT * FROM outbox ORDER BY queuedAt ASC")
    suspend fun getAll(): List<OutboxEntity>

    @Query("DELETE FROM outbox WHERE tableName = :tableName AND entityId = :entityId")
    suspend fun remove(tableName: String, entityId: String)

    @Query(
        "UPDATE outbox SET retryCount = retryCount + 1, lastError = :error " +
            "WHERE tableName = :tableName AND entityId = :entityId"
    )
    suspend fun recordFailure(tableName: String, entityId: String, error: String?)
}
