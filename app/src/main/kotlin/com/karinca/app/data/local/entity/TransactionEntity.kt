package com.karinca.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.karinca.app.data.local.CategoryKind
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.NO_ACTION, // categories are tombstoned, never hard-deleted
        ),
    ],
    indices = [Index("categoryId"), Index("updatedAt"), Index("occurredAt"), Index("userId")],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val categoryId: String,
    val type: CategoryKind,
    val amount: Double,
    val occurredAt: LocalDate,
    val note: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
)
