package com.karinca.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.karinca.app.data.local.CoachTone
import com.karinca.app.data.local.ReferenceType
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String, // == auth user id
    val referenceType: ReferenceType,
    val referenceAmount: Double,
    val targetDate: LocalDate?, // only meaningful when referenceType == GOAL
    val referenceSetAt: Instant,
    val regularMonthlyIncome: Double,
    val variableMonthlyIncome: Double?, // optional — irregular/gig income on top of the regular amount
    val coachTone: CoachTone,
    val createdAt: Instant,
    val updatedAt: Instant, // client-authoritative last-write-wins field
)
