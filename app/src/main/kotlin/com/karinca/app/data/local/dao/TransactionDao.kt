package com.karinca.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.karinca.app.data.local.CategoryKind
import com.karinca.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class CategoryTotal(
    val categoryId: String,
    val total: Double,
    val count: Int,
)

data class CategoryUsageCount(
    val categoryId: String,
    val count: Int,
)

@Dao
interface TransactionDao {

    @Query(
        """
        SELECT * FROM transactions
        WHERE userId = :userId AND deletedAt IS NULL AND occurredAt BETWEEN :from AND :to
        ORDER BY occurredAt DESC
        """
    )
    fun observeBetween(userId: String, from: LocalDate, to: LocalDate): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun get(id: String): TransactionEntity?

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE userId = :userId AND type = :type AND deletedAt IS NULL AND occurredAt BETWEEN :from AND :to
        """
    )
    fun observeTotal(userId: String, type: CategoryKind, from: LocalDate, to: LocalDate): Flow<Double>

    @Query(
        """
        SELECT categoryId, SUM(amount) AS total, COUNT(*) AS count FROM transactions
        WHERE userId = :userId AND type = :type AND deletedAt IS NULL AND occurredAt BETWEEN :from AND :to
        GROUP BY categoryId
        """
    )
    fun observeCategoryTotals(userId: String, type: CategoryKind, from: LocalDate, to: LocalDate): Flow<List<CategoryTotal>>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE userId = :userId AND type = :type AND deletedAt IS NULL
        """
    )
    fun observeAllTimeTotal(userId: String, type: CategoryKind): Flow<Double>

    // All-time usage counts, no date range — used to rank the quick-add category picker by
    // how often the user actually reaches for each one, not alphabetically.
    @Query(
        """
        SELECT categoryId, COUNT(*) AS count FROM transactions
        WHERE userId = :userId AND deletedAt IS NULL
        GROUP BY categoryId
        """
    )
    fun observeCategoryUsageCounts(userId: String): Flow<List<CategoryUsageCount>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE userId = :userId AND deletedAt IS NULL
        ORDER BY occurredAt DESC, createdAt DESC
        LIMIT :limit
        """
    )
    fun observeRecentAll(userId: String, limit: Int): Flow<List<TransactionEntity>>

    // Every transaction, newest first — the "Tüm İşlemler" screen behind Home's
    // "Tümünü Gör". Same ordering as observeRecentAll, just without the cap.
    @Query(
        """
        SELECT * FROM transactions
        WHERE userId = :userId AND deletedAt IS NULL
        ORDER BY occurredAt DESC, createdAt DESC
        """
    )
    fun observeAll(userId: String): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE userId = :userId AND categoryId = :categoryId AND deletedAt IS NULL
        ORDER BY occurredAt DESC, createdAt DESC
        LIMIT :limit
        """
    )
    fun observeRecentByCategory(userId: String, categoryId: String, limit: Int): Flow<List<TransactionEntity>>

    @Upsert
    suspend fun upsert(transaction: TransactionEntity)

    @Upsert
    suspend fun upsertAll(transactions: List<TransactionEntity>)
}
