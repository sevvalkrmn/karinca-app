package com.karinca.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.karinca.app.data.local.entity.RecurringPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringPaymentDao {

    @Query(
        """
        SELECT * FROM recurring_payments
        WHERE userId = :userId AND active = 1 AND deletedAt IS NULL
        ORDER BY createdAt DESC
        """
    )
    fun observeActive(userId: String): Flow<List<RecurringPaymentEntity>>

    @Query("SELECT * FROM recurring_payments WHERE id = :id")
    suspend fun get(id: String): RecurringPaymentEntity?

    @Upsert
    suspend fun upsert(payment: RecurringPaymentEntity)

    @Upsert
    suspend fun upsertAll(payments: List<RecurringPaymentEntity>)
}
