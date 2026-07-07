# Karınca

## What This Is

Karınca ("ant" in Turkish) is an offline-first Android budgeting app for the Turkish market (Turkish language, TRY currency) aimed at students and young adults. It is not a passive expense ledger — its defining feature is an **active AI financial coach** that delivers a **background weekly report**: a deterministic analysis of the user's income and spending, narrated in a warm, witty "thrifty ant" personality at a user-chosen bluntness level. The name plays on the idiom *karınca kararınca* — small, consistent saving adds up.

## Core Value

The user actually saves more money because the weekly coach gives **correct, concrete, trustworthy** advice — comparisons and suggestions grounded in numbers that are always computed deterministically, never invented by the LLM. If everything else fails, this must work.

## Requirements

### Validated

<!-- Shipped and confirmed valuable. -->

(None yet — ship to validate)

### Active

<!-- Current MVP scope. These are hypotheses until shipped and validated. -->

- [ ] Fast, offline manual income/expense entry
- [ ] Categories as core infrastructure (the report's comparisons are category-based)
- [ ] User profile: monthly income (regular + optional variable), a single savings goal OR monthly spending limit, and coach tone (gentle / balanced / brutal) — editable, injected into the prompt each period
- [ ] Comparative history views: this week vs last week, this month vs last month, by category
- [ ] Background AI **weekly** report (server-scheduled): deterministic `FinancialEngine` → `ReportFacts` → narrator in Karınca voice at chosen tone
- [ ] Cold-start handling: graceful "getting to know you" state when there isn't enough history to compare
- [ ] Freemium: free tier = fast tracker + basic comparative views; Pro (RevenueCat) = the AI weekly report

### Out of Scope

<!-- Explicit boundaries for this MVP. Deferred to later versions; schema must stay forward-compatible. -->

- Shared expenses, group splits, debt ledger — deferred to v2 (also removes the hard multi-user server-authoritative sync + RLS problem; MVP sync is single-user, multi-device, LWW only)
- Installments (taksit) and recurring transactions — deferred to v2
- Multi-currency / FX / gold equivalent display — deferred to v2
- Home-screen widget — deferred to v2
- SMS auto-capture, bank integrations, receipt OCR, in-report chatbot, advanced net-worth tracking — deferred to later versions
- **Design constraint:** the data model must be forward-compatible with the deferred v2 features (installments, recurring, shared expenses, multi-currency) so adding them later does not require a painful migration. Don't build them now — just don't box them out of the schema.

## Context

- **Market:** Turkey first. Turkish language, TRY primary currency. Primary users: students and young adults with irregular income.
- **Cultural framing:** the ant as thrifty, forward-planning saver; coach personality is a close friend who helps you save and teases gently, never a cold bank.
- **Personality is user-tunable:** coach tone (gentle / balanced / brutal) controls how blunt the advice is. Personality comes from the LLM; correctness comes from the math.
- **Deferred v2 features are known now** so the schema is designed forward-compatible from Phase 1.

## Constraints

- **Architecture (do not violate):** The LLM must NEVER compute or invent numbers. All financial calculations (category totals, week/month comparisons, counterfactuals, anomaly detection, goal progress) are done deterministically in Python. The LLM's only job is to narrate pre-computed numbers in the Karınca personality at the chosen tone. Enforced structurally via the `narrate(facts, profile, tone)` boundary.
- **Client:** Kotlin + Jetpack Compose, MVVM, Hilt (DI). Offline-first: local Room (SQLite) is the source of truth; DataStore for prefs.
- **Cloud data & auth:** Supabase (Postgres + Auth + RLS + Storage). Client syncs local Room ↔ Supabase.
- **Sync (MVP):** single-user, multi-device only. UUID primary keys (client-generated), `updated_at` cursors, tombstones (soft deletes), outbox pattern, last-write-wins conflict resolution. (Server-authoritative resolution is a v2 concern that arrives with shared expenses.)
- **AI service:** a separate Python/FastAPI microservice (deterministic `FinancialEngine` + LLM narrator). Swappable LLM provider behind the `narrate()` boundary. Single structured prompt call — NOT LangGraph. Server-scheduled (cron) trigger, not client-invoked.
- **Auth:** Google sign-in + phone OTP (both common for the target user).
- **Payments:** Google Play Billing via RevenueCat (freemium; Pro gates the AI weekly report).
- **Compliance:** KVKK (Turkish data-protection law) is a Phase 1 design input — explicit consent for storing financial PII (Supabase EU region) and for sending aggregated summaries to the LLM provider.
- **Typography:** Plus Jakarta Sans.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| LLM never computes; `narrate(facts, profile, tone)` boundary | Correctness is the paid product; structural separation makes invented numbers impossible | — Pending |
| `FinancialEngine` runs in the Python service (reads Supabase via service role) | One authoritative, unit-testable engine; keeps compute server-side and trustworthy | — Pending |
| Weekly report trigger is a server-side scheduled job (cron → FastAPI) | Reliability; runs even if the app isn't opened | — Pending |
| Room = offline source of truth; Supabase for auth/sync | Fast offline entry is the free-tier hook; relational data fits Postgres | — Pending |
| MVP sync is single-user multi-device, LWW | Deferring shared expenses removes the hard multi-user consistency problem | — Pending |
| UUID PKs + `updated_at` + tombstones + outbox, baked into Phase 1 schema | Offline creation needs collision-free keys; retrofitting sync primitives is painful | — Pending |
| Schema forward-compatible with deferred v2 (installments, recurring, splits, multi-currency) | Avoid a painful migration when v2 features land | — Pending |
| Single structured prompt call, swappable provider — not LangGraph | The job is "narrate a typed facts object in a tone"; agent-graph overhead unwarranted | — Pending |
| Auth: Google sign-in + phone OTP | Both common for Turkish students/young adults | — Pending |
| Monetization: AI weekly report is the Pro feature; free = tracker + basic comparisons | The coach is the only truly differentiated part; it anchors Pro | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd:complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-07-07 after initialization*
