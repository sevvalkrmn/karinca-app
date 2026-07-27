package com.karinca.app.data.sync

// Shared between outbox writes, the cursor store, and the sync worker so the
// remote table name is defined in exactly one place — never a raw string literal
// scattered across call sites.
enum class SyncTable(val remoteName: String) {
    PROFILES("profiles"),
    CONSENTS("consents"),
    CATEGORIES("categories"),
    TRANSACTIONS("transactions"),
    WEEKLY_REPORTS("weekly_reports"),
    GOALS("goals"),
    PIGGY_BANK_ENTRIES("piggy_bank_entries"),
    RECURRING_PAYMENTS("recurring_payments"),
}
