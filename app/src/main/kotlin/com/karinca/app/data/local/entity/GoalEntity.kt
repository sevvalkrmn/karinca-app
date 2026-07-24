package com.karinca.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Entity(
    tableName = "goals",
    indices = [Index("userId"), Index("updatedAt")],
)
data class GoalEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val icon: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: LocalDate,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
    val completedAt: Instant?,
    // The last calendar date GoalRepository.evaluateProgress() confirmed real budget still
    // covered this goal's cumulative promise. Null means it's never been evaluated yet
    // (treated as the creation date — i.e. zero days confirmed).
    val lastEvaluatedDate: LocalDate?,
) {
    // How much this goal is *paced* to have accumulated by today if saving steadily from
    // the day it was created through its target date — the day of creation counts as the
    // first saving day, so this is never zero right after a goal is created. Pure date
    // math, no real transaction behind it. Used for the creation-time budget gate and
    // Home's spendableBalance nudge — NOT for the goal's own displayed progress (see
    // displayedCurrentAmount).
    fun pacedAmount(today: LocalDate = LocalDate.now()): Double {
        val startDate = createdAt.atZone(ZoneId.systemDefault()).toLocalDate()
        val totalDays = ChronoUnit.DAYS.between(startDate, targetDate).coerceAtLeast(1)
        val elapsedDays = (ChronoUnit.DAYS.between(startDate, today) + 1).coerceIn(1, totalDays)
        return (targetAmount / totalDays) * elapsedDays
    }

    // What the goal card shows as "saved so far": the ratcheted, budget-verified amount
    // GoalRepository.evaluateProgress() has confirmed — never a projection. A freshly
    // created goal reads 0 until a full day has passed and the real balance has been
    // checked to still cover it.
    fun displayedCurrentAmount(): Double = currentAmount
}
