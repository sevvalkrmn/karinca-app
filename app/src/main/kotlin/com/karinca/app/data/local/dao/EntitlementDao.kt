package com.karinca.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.karinca.app.data.local.entity.EntitlementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntitlementDao {

    @Query("SELECT * FROM entitlements WHERE userId = :userId")
    fun observe(userId: String): Flow<EntitlementEntity?>

    @Upsert
    suspend fun upsertAll(entitlements: List<EntitlementEntity>)
}
