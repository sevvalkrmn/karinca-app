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
