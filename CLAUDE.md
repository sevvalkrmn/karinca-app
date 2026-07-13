## Project

**Karınca**

Karınca ("ant" in Turkish) is an offline-first Android budgeting app for the Turkish market (Turkish language, TRY currency) aimed at students and young adults. It is not a passive expense ledger — its defining feature is an **active AI financial coach** that delivers a **background weekly report**: a deterministic analysis of the user's income and spending, narrated in a warm, witty "thrifty ant" personality at a user-chosen bluntness level. The name plays on the idiom *karınca kararınca* — small, consistent saving adds up.

**Core Value:** The user actually saves more money because the weekly coach gives **correct, concrete, trustworthy** advice — comparisons and suggestions grounded in numbers that are always computed deterministically, never invented by the LLM. If everything else fails, this must work.

### Constraints

- **Architecture (do not violate):** The LLM must NEVER compute or invent numbers. All financial calculations (category totals, week/month comparisons, counterfactuals, anomaly detection, goal progress) are done deterministically in Python. The LLM's only job is to narrate pre-computed numbers in the Karınca personality at the chosen tone. Enforced structurally via the `narrate(facts, profile, tone)` boundary.
- **Client:** Kotlin + Jetpack Compose, MVVM, Hilt (DI). Offline-first: local Room (SQLite) is the source of truth; DataStore for prefs.
- **Cloud data & auth:** Supabase (Postgres + Auth + RLS + Storage). Client syncs local Room ↔ Supabase.
- **Sync (MVP):** single-user, multi-device only. UUID primary keys (client-generated), `updated_at` cursors, tombstones (soft deletes), outbox pattern, last-write-wins conflict resolution. (Server-authoritative resolution is a v2 concern that arrives with shared expenses.)
- **AI service:** a separate Python/FastAPI microservice (deterministic `FinancialEngine` + LLM narrator). Swappable LLM provider behind the `narrate()` boundary. Single structured prompt call — NOT LangGraph. Server-scheduled (cron) trigger, not client-invoked.
- **Auth:** Google sign-in + phone OTP (both common for the target user).
- **Payments:** Google Play Billing via RevenueCat (freemium; Pro gates the AI weekly report).
- **Compliance:** KVKK (Turkish data-protection law) is a Phase 1 design input — explicit consent for storing financial PII (Supabase EU region) and for sending aggregated summaries to the LLM provider.
- **Typography:** Plus Jakarta Sans.

## Working Agreement — UI Division of Labor

**The user designs; Claude implements.** The product's look and personality belong to the user.

**The user owns (do NOT do these):**
- All screen design in Figma: layout, visual design, brand language, component decisions
- All user-facing Turkish copy — the "Karınca" voice

**Do NOT invent screen designs or visual layouts.** When a screen is needed, ask the user for the design (Figma screen / screenshot / spec), then implement it faithfully in Compose. If no design has been provided yet, **ask — do not improvise one.**

**Claude owns:**
- Translating the user's finished designs into Jetpack Compose code
- Material 3 theme / token setup (colors, typography, shapes)
- ViewModels, state holders (`StateFlow` / UI state), navigation
- Data model, Room (SQLite) entities/DAOs/migrations, Supabase (Postgres) schema + RLS
- Sync engine (UUID PKs, `updated_at` cursors, tombstones, outbox, last-write-wins)
- The Python/FastAPI AI service (`FinancialEngine`, `narrate()` boundary, cron trigger)
- Business logic and all wiring (binding UI to state + events)

**When writing Compose from the user's designs, briefly explain the structure and the key Compose concepts used** — the user wants to understand and maintain the code, not just receive it.

**Secrets:** Supabase service-role key and any LLM key are server-side env only — never in the repo or the Android client. The client uses the Supabase anon key + RLS.

## Technology Stack

Technology stack not yet documented. Will populate after codebase mapping or first phase.

## Conventions

Conventions not yet established. Will populate as patterns emerge during development.

## Architecture

Architecture not yet mapped. Follow existing patterns found in the codebase.

## Project Skills

No project skills found. Add skills to any of: `.claude/skills/`, `.agents/skills/`, `.cursor/skills/`, `.github/skills/`, or `.codex/skills/` with a `SKILL.md` index file.
