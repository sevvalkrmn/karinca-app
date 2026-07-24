package com.karinca.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.karinca.app.data.local.entity.WeeklyReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyReportDao {

    @Query("SELECT * FROM weekly_reports WHERE userId = :userId ORDER BY weekStart DESC")
    fun observeAll(userId: String): Flow<List<WeeklyReportEntity>>

    @Upsert
    suspend fun upsertAll(reports: List<WeeklyReportEntity>)
}
