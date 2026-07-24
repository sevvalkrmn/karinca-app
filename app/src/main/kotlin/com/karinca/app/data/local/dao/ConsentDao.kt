package com.karinca.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.karinca.app.data.local.entity.ConsentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsentDao {

    @Query("SELECT * FROM consents WHERE id = :userId")
    fun observe(userId: String): Flow<ConsentEntity?>

    @Query("SELECT * FROM consents WHERE id = :userId")
    suspend fun get(userId: String): ConsentEntity?

    @Upsert
    suspend fun upsert(consent: ConsentEntity)
}
