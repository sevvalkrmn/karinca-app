package com.karinca.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.karinca.app.data.local.entity.PiggyBankEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PiggyBankEntryDao {

    @Query("SELECT COALESCE(SUM(amount), 0) FROM piggy_bank_entries WHERE userId = :userId")
    fun observeBalance(userId: String): Flow<Double>

    @Query("SELECT * FROM piggy_bank_entries WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeAll(userId: String): Flow<List<PiggyBankEntryEntity>>

    @Query("SELECT * FROM piggy_bank_entries WHERE id = :id")
    suspend fun get(id: String): PiggyBankEntryEntity?

    @Upsert
    suspend fun upsert(entry: PiggyBankEntryEntity)

    @Upsert
    suspend fun upsertAll(entries: List<PiggyBankEntryEntity>)
}
