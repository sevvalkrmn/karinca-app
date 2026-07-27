package com.karinca.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

// Set up once (from the "tekrar eden ödeme" checkbox in Harcama Ekle), then the AI
// service's monthly job auto-creates a matching expense transaction every month after.
// last_processed_month (server-only bookkeeping, mirrors profiles.lastAutoIncomeMonth)
// deliberately has no field here — the client never reads or writes it.
@Entity(
    tableName = "recurring_payments",
    indices = [Index("userId"), Index("updatedAt")],
)
data class RecurringPaymentEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val amount: Double,
    val categoryId: String?,
    val dayOfMonth: Int, // 1-28, so every month always has this day
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
)
