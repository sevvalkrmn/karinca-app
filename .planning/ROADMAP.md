# Roadmap: Karınca

## Overview

Karınca ships in four phases, re-sequenced so the AI weekly report — the app's actual core value — arrives as early as possible. Phase 1 builds the foundation: auth, KVKK consent, the offline-first Room/Supabase data model with sync primitives, categories, user profile, and the versioned `ReportFacts` contract that Phase 3 will populate. Phase 2 delivers a fully usable free product: fast offline transaction entry, comparative history views, and a logging streak. Phase 3 is the differentiator: a separate Python/FastAPI service where a deterministic `FinancialEngine` computes every number and an LLM narrator (behind a strict `narrate(facts, profile, tone)` boundary) turns those numbers into a Karınca-voiced weekly report — the LLM never invents or computes a figure. Phase 4 monetizes that report as a RevenueCat Pro subscription in TRY, while the free tier stays fully usable.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [ ] **Phase 1: Foundation & Data Model** - Auth, KVKK consent, offline-first synced data model, categories, user profile, and the `ReportFacts` contract
- [ ] **Phase 2: Free Core** - Fast offline income/expense entry, comparative history views, and logging streak
- [ ] **Phase 3: AI Weekly Report** - Deterministic FinancialEngine + narrator service delivering the Karınca-voiced weekly report
- [ ] **Phase 4: Pro / Paywall** - RevenueCat freemium gating of the AI weekly report with TRY pricing

## Phase Details

### Phase 1: Foundation & Data Model
**Goal**: Users have secure, KVKK-compliant accounts, a personal profile, categories, and an offline-first data model that syncs across their own devices — the foundation everything else builds on.
**Depends on**: Nothing (first phase)
**Requirements**: AUTH-01, AUTH-02, AUTH-03, AUTH-04, DATA-01, DATA-02, DATA-03, DATA-04, DATA-05, DATA-06, DATA-07, CAT-01, CAT-02, CAT-03, PROF-01, PROF-02, PROF-03, PROF-04, PROF-05
**Success Criteria** (what must be TRUE):
  1. User can sign in with Google or phone OTP and stays signed in across app restarts; the app blocks usage until KVKK consent for financial data storage and LLM processing is granted and recorded.
  2. User can create and edit a profile: monthly income as regular + optional variable amount, a single reference (savings goal OR monthly spending limit), and a coach tone (gentle/balanced/brutal) — changes persist.
  3. User can categorize their data using default Turkish categories or custom ones they add, each with a color and icon.
  4. Data created offline on one device appears on the user's other device of the same account after sync; deleting a record removes it everywhere without it ever reappearing (tombstoned, last-write-wins by `updated_at`).
  5. The versioned `ReportFacts` DTO contract exists and is shared by the schema design, ready for the FinancialEngine to populate numbers into and the client to render in Phase 3.
**Plans**: TBD

Plans:
- [ ] 01-01: TBD

### Phase 2: Free Core
**Goal**: Users can quickly track income and expenses entirely offline, view comparative spending history, and build a logging streak — a fully usable free product on its own.
**Depends on**: Phase 1
**Requirements**: TXN-01, TXN-02, TXN-03, TXN-04, TXN-05, HIST-01, HIST-02, HIST-03, HIST-04, ENG-01
**Success Criteria** (what must be TRUE):
  1. User can record an expense (amount, category, date) in minimal taps from the main screen while fully offline.
  2. User can record income (regular and one-off), and edit or delete any transaction.
  3. User can view this week's spending vs last week's, this month's vs last month's, and a breakdown by category — all available in the free tier with no paywall.
  4. User sees a visible streak that grows with consistent logging.
**Plans**: TBD

Plans:
- [ ] 02-01: TBD
**UI hint**: yes

### Phase 3: AI Weekly Report
**Goal**: Users receive a weekly, server-generated AI report in the Karınca voice, with every number computed deterministically and the LLM used only for narration.
**Depends on**: Phase 1, Phase 2
**Requirements**: RPT-01, RPT-02, RPT-03, RPT-04, RPT-05, RPT-06, RPT-07, RPT-08
**Success Criteria** (what must be TRUE):
  1. A server-scheduled (cron) job generates a report for every user each week without the app being opened.
  2. Every number in a report traces back to the deterministic Python `FinancialEngine`'s `ReportFacts` output — the LLM narrator receives only `ReportFacts` + profile + tone via the `narrate()` boundary and never performs arithmetic or introduces a figure absent from `ReportFacts`; swapping the LLM provider changes only wording, never a single number.
  3. The report's bluntness matches the user's chosen coach tone (gentle/balanced/brutal), and includes both a backward-looking comparison and at least one forward-looking savings suggestion, both grounded in computed numbers.
  4. When a user has insufficient history to compare, the report gracefully shows a "getting to know you" state instead of an empty or misleading comparison.
  5. Every generated report persists both its `ReportFacts` and its narrated text, so any claim in the text is auditable back to a number.
**Plans**: TBD

Plans:
- [ ] 03-01: TBD

### Phase 4: Pro / Paywall
**Goal**: The AI weekly report is monetized as a Pro subscription in TRY via RevenueCat/Google Play Billing, while the free tracking and comparison experience remains fully usable.
**Depends on**: Phase 3
**Requirements**: PRO-01, PRO-02, PRO-03, PRO-04
**Success Criteria** (what must be TRUE):
  1. A non-paying user retains full access to fast tracking and comparative history views — nothing in the free tier is crippled to push conversion.
  2. The AI weekly report is inaccessible without an active Pro subscription.
  3. User can purchase Pro through Google Play Billing via RevenueCat at TRY pricing, and gains access to the weekly report immediately after purchase.
**Plans**: TBD

Plans:
- [ ] 04-01: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation & Data Model | 0/TBD | Not started | - |
| 2. Free Core | 0/TBD | Not started | - |
| 3. AI Weekly Report | 0/TBD | Not started | - |
| 4. Pro / Paywall | 0/TBD | Not started | - |
