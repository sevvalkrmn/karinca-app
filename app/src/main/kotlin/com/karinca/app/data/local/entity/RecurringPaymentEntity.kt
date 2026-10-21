package com.karinca.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.karinca.app.data.local.CategoryKind
import com.karinca.app.data.local.RecurringDestination
import java.time.Instant

// Set up once (from the "tekrar eden" checkbox in Harcama/Gelir Ekle), then the AI
// service's monthly job auto-creates it every month after: an expense or BUDGET income
// as a transaction of that type, a KUMBARA income as a Kumbara deposit.
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
    val type: CategoryKind,
    val destination: RecurringDestination,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
)
