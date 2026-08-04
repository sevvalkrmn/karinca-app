package com.karinca.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

// Read-only mirror: written exclusively by the RevenueCat webhook (via service-role)
// server-side and pulled down here — never appears in the outbox, never pushed.
// No row for a user means "not Pro" (see EntitlementRepository), mirroring the same
// convention the entitlements table itself documents server-side.
@Entity(tableName = "entitlements")
data class EntitlementEntity(
    @PrimaryKey val userId: String,
    val isPro: Boolean,
    val updatedAt: Instant,
)
