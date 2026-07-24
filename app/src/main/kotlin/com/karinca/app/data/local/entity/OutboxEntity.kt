package com.karinca.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

// Marks "this row in this table needs pushing" — never stores a payload snapshot.
// The sync worker always re-reads the entity's live state at push time, so repeated
// local edits before a sync cycle coalesce into a single pending push (see the unique
// index below, used with OnConflictStrategy.REPLACE on insert).
@Entity(
    tableName = "outbox",
    indices = [Index(value = ["tableName", "entityId"], unique = true)],
)
data class OutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableName: String,
    val entityId: String,
    val queuedAt: Instant,
    val retryCount: Int = 0,
    val lastError: String? = null,
)
