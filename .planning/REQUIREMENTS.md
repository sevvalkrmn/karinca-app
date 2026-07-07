# Requirements: Karınca

**Defined:** 2026-07-07
**Core Value:** The user saves more money because the weekly coach gives correct, concrete, trustworthy advice grounded in deterministically-computed numbers.

## v1 Requirements

Requirements for the initial release (narrowed MVP: track income/expense, compare over time, deliver a background weekly AI report). Each maps to a roadmap phase.

### Authentication & Consent

- [ ] **AUTH-01**: User can sign in with Google
- [ ] **AUTH-02**: User can sign in with a phone number via OTP
- [ ] **AUTH-03**: User session persists across app restarts
- [ ] **AUTH-04**: User must grant KVKK consent (financial data storage + LLM processing of summaries) before using the app, and the consent is recorded

### Data & Sync

- [ ] **DATA-01**: Every syncable entity uses a client-generated UUID primary key
- [ ] **DATA-02**: The app is fully usable offline; the local Room database is the source of truth
- [ ] **DATA-03**: Local data syncs to Supabase across the same user's multiple devices
- [ ] **DATA-04**: Sync resolves conflicts by last-write-wins using `updated_at`
- [ ] **DATA-05**: Deletes are soft (tombstones) and propagate across devices without resurrecting
- [ ] **DATA-06**: The schema accommodates deferred v2 features (installments, recurring, shared expenses, multi-currency) so they can be added without a destructive migration
- [ ] **DATA-07**: The `ReportFacts` DTO contract (the typed set of numbers a report may reference) is defined and versioned, shared by the FinancialEngine and the client

### Categories

- [ ] **CAT-01**: User can assign every transaction to a category
- [ ] **CAT-02**: App ships with default Turkish categories, and the user can add custom categories
- [ ] **CAT-03**: Each category has a color and an icon

### User Profile

- [ ] **PROF-01**: User can set monthly income as a regular amount plus an optional variable amount
- [ ] **PROF-02**: User can set a single reference the coach speaks against — either a savings goal OR a monthly spending limit
- [ ] **PROF-03**: User can select a coach tone: gentle, balanced, or brutal
- [ ] **PROF-04**: User can edit the profile at any time
- [ ] **PROF-05**: The profile is stored as a record that is injected into the AI prompt each report period

### Transaction Entry

- [ ] **TXN-01**: User can record an expense (amount, category, date) quickly while offline
- [ ] **TXN-02**: User can record income (regular and one-off)
- [ ] **TXN-03**: User can edit a transaction
- [ ] **TXN-04**: User can delete a transaction
- [ ] **TXN-05**: Expense entry is fast, reachable in minimal taps from the main screen

### Comparative History

- [ ] **HIST-01**: User can view this week's spending compared to last week's
- [ ] **HIST-02**: User can view this month's spending compared to last month's
- [ ] **HIST-03**: User can view spending broken down by category
- [ ] **HIST-04**: Comparative history views are available in the free tier

### Engagement

- [ ] **ENG-01**: User accumulates a visible streak for consistent logging

### AI Weekly Report

- [ ] **RPT-01**: A server-scheduled job generates a weekly report per user
- [ ] **RPT-02**: The deterministic Python FinancialEngine computes every number in the report (`ReportFacts`) from the user's transactions
- [ ] **RPT-03**: The narrator produces a Karınca-voiced report from `ReportFacts` + profile + tone; the LLM never computes or invents numbers (enforced by the `narrate(facts, profile, tone)` boundary)
- [ ] **RPT-04**: The report's bluntness reflects the user's chosen coach tone (gentle / balanced / brutal)
- [ ] **RPT-05**: The report includes a backward-looking comparison and at least one forward-looking savings suggestion, both grounded in computed numbers
- [ ] **RPT-06**: When there is insufficient history to compare, the report degrades gracefully to a "getting to know you" state instead of producing empty or misleading comparisons
- [ ] **RPT-07**: Each generated report persists both its `ReportFacts` and its narrated text, so every claim is auditable back to a number
- [ ] **RPT-08**: The LLM provider is swappable behind the `narrate()` boundary without changing the FinancialEngine

### Monetization

- [ ] **PRO-01**: Free tier includes fast tracking and basic comparative views, fully usable on its own
- [ ] **PRO-02**: The AI weekly report is gated behind a Pro subscription
- [ ] **PRO-03**: User can purchase Pro via Google Play Billing through RevenueCat, with TRY pricing
- [ ] **PRO-04**: A non-paying user retains full access to the free tier

## v2 Requirements

Deferred to a future release. Tracked but not in the current roadmap. The schema is designed so these can be added without a destructive migration (DATA-06).

### Shared Expenses
- **SHARE-01**: Groups, members (including non-app people), and expense splits (equal / exact / percentage / shares)
- **SHARE-02**: Simple 1:1 person-to-person debt ledger with settle-up
- **SHARE-03**: Multi-user server-authoritative sync + RLS for shared data

### Installments & Recurring
- **INST-01**: Installment (taksit) plans that materialize monthly transactions
- **RECUR-01**: Recurring transaction templates that spawn scheduled transactions

### Multi-Currency
- **FX-01**: Multi-currency transactions with rate-at-time snapshots
- **FX-02**: FX / gold current-equivalent display from a rate feed

### Convenience
- **WIDGET-01**: Home-screen widget for quick entry / glanceable status
- **RPT-09**: Optional weekly cadence toggle and deeper Pro comparisons

## Out of Scope

Explicitly excluded from the product roadmap for the foreseeable future.

| Feature | Reason |
|---------|--------|
| SMS auto-capture of bank messages | Fragile parsing, permissions/privacy burden; manual entry is the MVP hook |
| Bank / open-banking integrations | High integration + compliance cost; not core to the coaching value |
| Receipt OCR | Complexity vs. value; manual entry suffices for MVP |
| In-report chatbot | The value is the periodic narrated report, not live Q&A; adds cost/latency |
| Advanced net-worth tracking | Beyond the "track, compare, coach" core |
| LangGraph / agentic orchestration | The AI job is narrating a typed facts object in a tone — a single structured prompt call suffices |

## Traceability

Finalized by roadmapper (ROADMAP.md, 2026-07-07). Phase 1 = Foundation & Data Model, Phase 2 = Free Core, Phase 3 = AI Weekly Report, Phase 4 = Pro / Paywall. Each requirement maps to exactly one phase.

| Requirement | Phase | Status |
|-------------|-------|--------|
| AUTH-01 | Phase 1 | Pending |
| AUTH-02 | Phase 1 | Pending |
| AUTH-03 | Phase 1 | Pending |
| AUTH-04 | Phase 1 | Pending |
| DATA-01 | Phase 1 | Pending |
| DATA-02 | Phase 1 | Pending |
| DATA-03 | Phase 1 | Pending |
| DATA-04 | Phase 1 | Pending |
| DATA-05 | Phase 1 | Pending |
| DATA-06 | Phase 1 | Pending |
| DATA-07 | Phase 1 | Pending |
| CAT-01 | Phase 1 | Pending |
| CAT-02 | Phase 1 | Pending |
| CAT-03 | Phase 1 | Pending |
| PROF-01 | Phase 1 | Pending |
| PROF-02 | Phase 1 | Pending |
| PROF-03 | Phase 1 | Pending |
| PROF-04 | Phase 1 | Pending |
| PROF-05 | Phase 1 | Pending |
| TXN-01 | Phase 2 | Pending |
| TXN-02 | Phase 2 | Pending |
| TXN-03 | Phase 2 | Pending |
| TXN-04 | Phase 2 | Pending |
| TXN-05 | Phase 2 | Pending |
| HIST-01 | Phase 2 | Pending |
| HIST-02 | Phase 2 | Pending |
| HIST-03 | Phase 2 | Pending |
| HIST-04 | Phase 2 | Pending |
| ENG-01 | Phase 2 | Pending |
| RPT-01 | Phase 3 | Pending |
| RPT-02 | Phase 3 | Pending |
| RPT-03 | Phase 3 | Pending |
| RPT-04 | Phase 3 | Pending |
| RPT-05 | Phase 3 | Pending |
| RPT-06 | Phase 3 | Pending |
| RPT-07 | Phase 3 | Pending |
| RPT-08 | Phase 3 | Pending |
| PRO-01 | Phase 4 | Pending |
| PRO-02 | Phase 4 | Pending |
| PRO-03 | Phase 4 | Pending |
| PRO-04 | Phase 4 | Pending |

**Coverage:**
- v1 requirements: 41 total
- Mapped to phases: 41
- Unmapped: 0 ✓

---
*Requirements defined: 2026-07-07*
*Last updated: 2026-07-07 after initial definition*
