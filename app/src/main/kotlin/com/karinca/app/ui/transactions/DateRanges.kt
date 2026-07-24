package com.karinca.app.ui.transactions

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class DateRange(val from: LocalDate, val to: LocalDate)

enum class ComparisonPeriod { WEEK, MONTH }

// ISO week (Monday..Sunday) — matches the app's TR locale convention.
object DateRanges {

    fun current(period: ComparisonPeriod, today: LocalDate = LocalDate.now()): DateRange = when (period) {
        ComparisonPeriod.WEEK -> thisWeek(today)
        ComparisonPeriod.MONTH -> thisMonth(today)
    }

    fun previous(period: ComparisonPeriod, today: LocalDate = LocalDate.now()): DateRange = when (period) {
        ComparisonPeriod.WEEK -> lastWeek(today)
        ComparisonPeriod.MONTH -> lastMonth(today)
    }

    private fun thisWeek(today: LocalDate): DateRange {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return DateRange(monday, monday.plusDays(6))
    }

    private fun lastWeek(today: LocalDate): DateRange {
        val thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val lastMonday = thisMonday.minusWeeks(1)
        return DateRange(lastMonday, lastMonday.plusDays(6))
    }

    private fun thisMonth(today: LocalDate): DateRange {
        val first = today.withDayOfMonth(1)
        return DateRange(first, first.with(TemporalAdjusters.lastDayOfMonth()))
    }

    private fun lastMonth(today: LocalDate): DateRange {
        val firstOfThisMonth = today.withDayOfMonth(1)
        val firstOfLastMonth = firstOfThisMonth.minusMonths(1)
        return DateRange(firstOfLastMonth, firstOfLastMonth.with(TemporalAdjusters.lastDayOfMonth()))
    }
}
