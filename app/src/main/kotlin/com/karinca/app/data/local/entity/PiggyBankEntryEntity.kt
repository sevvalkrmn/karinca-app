package com.karinca.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.karinca.app.data.local.PiggyBankEntryReason
import java.time.Instant

// Append-only ledger row — never updated or deleted once written. The Kumbara's
// displayed balance is always SUM(amount) over these, never a stored counter, so
// this client (overspend withdrawals) and the server's monthly job (salary/limit
// deposits) can each freely insert their own rows without ever conflicting.
@Entity(
    tableName = "piggy_bank_entries",
    indices = [Index("userId"), Index("updatedAt")],
)
data class PiggyBankEntryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val amount: Double, // positive = deposit, negative = withdrawal
    val reason: PiggyBankEntryReason,
    val createdAt: Instant,
    val updatedAt: Instant,
)
