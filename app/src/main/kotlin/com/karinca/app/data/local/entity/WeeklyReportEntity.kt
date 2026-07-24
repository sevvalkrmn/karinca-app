package com.karinca.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.karinca.app.data.local.CoachTone
import java.time.Instant
import java.time.LocalDate

// Read-only mirror: rows are only ever written by the pull side of sync.
// Never appears in the outbox, never pushed.
@Entity(
    tableName = "weekly_reports",
    indices = [Index("userId"), Index("weekStart"), Index("updatedAt")],
)
data class WeeklyReportEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val contractVersion: String,
    val factsJson: String, // raw ReportFacts JSON payload, parsed on read
    val narration: String?,
    val coachToneUsed: CoachTone,
    val profileSnapshotJson: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
