package com.karinca.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

// One row per user, written right after sign-in — decoupled from ProfileEntity
// so recording KVKK consent never depends on the user having finished financial
// onboarding (income/goal/tone).
@Entity(tableName = "consents")
data class ConsentEntity(
    @PrimaryKey val id: String, // == auth user id
    val kvkkConsentAt: Instant,
    val kvkkConsentVersion: String, // policy text version consented to, forces re-consent on bump
    val createdAt: Instant,
    val updatedAt: Instant,
)
