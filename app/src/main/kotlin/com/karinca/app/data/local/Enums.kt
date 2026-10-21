package com.karinca.app.data.local

// String values match the Postgres check-constraint values exactly, so entity <-> DTO
// mapping never needs a separate translation table.

enum class ReferenceType(val value: String) {
    GOAL("goal"),
    LIMIT("limit");

    companion object {
        fun fromValue(value: String): ReferenceType = entries.first { it.value == value }
    }
}

enum class CoachTone(val value: String) {
    GENTLE("gentle"),
    BALANCED("balanced"),
    BRUTAL("brutal");

    companion object {
        fun fromValue(value: String): CoachTone = entries.first { it.value == value }
    }
}

enum class CategoryKind(val value: String) {
    INCOME("income"),
    EXPENSE("expense");

    companion object {
        fun fromValue(value: String): CategoryKind = entries.first { it.value == value }
    }
}

enum class PiggyBankEntryReason(val value: String) {
    // Immediate deposit the moment the auto-generated monthly salary transaction
    // lands, when regularMonthlyIncome exceeds the profile's LIMIT reference.
    SALARY_LIMIT_DIFF("salary_limit_diff"),
    // Deposit swept in at month's end when actual spending came in under the limit.
    MONTHLY_LEFTOVER("monthly_leftover"),
    // Withdrawal recorded when the user confirms spending past this month's limit.
    OVERSPEND_WITHDRAWAL("overspend_withdrawal"),
    // Compensating deposit when the transaction behind an OVERSPEND_WITHDRAWAL is
    // later deleted — the withdrawal itself is never edited/removed (append-only).
    OVERSPEND_REVERSAL("overspend_reversal"),
    // The user chose "Kumbaraya At" instead of "Mevcut Bütçeye Ekle" when logging a
    // new income — no transaction is created for it, this deposit is the only record.
    MANUAL_DEPOSIT("manual_deposit");

    companion object {
        fun fromValue(value: String): PiggyBankEntryReason = entries.first { it.value == value }
    }
}

// Where a recurring income lands each month — remembers the "Mevcut Bütçeye Ekle" /
// "Kumbaraya At" choice made when it was first logged. Always BUDGET for expenses.
enum class RecurringDestination(val value: String) {
    BUDGET("budget"),
    KUMBARA("kumbara");

    companion object {
        fun fromValue(value: String): RecurringDestination = entries.first { it.value == value }
    }
}
