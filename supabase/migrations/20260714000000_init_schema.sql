-- Karınca — initial schema (Phase 1: Foundation & Data Model)
--
-- Multi-provider auth note: every table below keys off auth.users(id), not off
-- a specific provider. Supabase Auth links Google, phone OTP, and (later)
-- email/password identities to a single auth.users row via auth.identities
-- natively — no extra schema is needed to support multiple providers per user.
-- Provider enablement and identity-linking behavior are configured in the
-- Supabase Auth dashboard, not here.
--
-- Sync model: every syncable table carries client-authoritative `updated_at`
-- (used for last-write-wins on the client) and server-authoritative
-- `server_updated_at` (stamped by trigger, used as the pull cursor — see
-- SyncCursorStore / SyncWorker.kt). Deletes are soft (`deleted_at`) where the
-- client can delete; there is no hard DELETE policy on any syncable table.

create or replace function public.set_server_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.server_updated_at := now();
  return new;
end;
$$;

-- ============================================================
-- profiles — one row per user, financial onboarding (income/goal/tone)
-- ============================================================
create table public.profiles (
    id uuid primary key references auth.users (id) on delete cascade,
    reference_type text not null check (reference_type in ('goal', 'limit')),
    reference_amount numeric(12, 2) not null,
    target_date date,
    reference_set_at timestamptz not null,
    regular_monthly_income numeric(12, 2) not null,
    variable_monthly_income numeric(12, 2),
    coach_tone text not null check (coach_tone in ('gentle', 'balanced', 'brutal')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null,
    server_updated_at timestamptz not null default now()
);

create trigger profiles_set_server_updated_at
before insert or update on public.profiles
for each row execute function public.set_server_updated_at();

alter table public.profiles enable row level security;

create policy profiles_select_own on public.profiles
    for select using (auth.uid() = id);
create policy profiles_insert_own on public.profiles
    for insert with check (auth.uid() = id);
create policy profiles_update_own on public.profiles
    for update using (auth.uid() = id) with check (auth.uid() = id);
-- No delete policy: a profile is only ever removed via account deletion,
-- which is not a client-facing flow yet.

-- ============================================================
-- consents — one row per user, recorded independently of profile setup
-- ============================================================
create table public.consents (
    id uuid primary key references auth.users (id) on delete cascade,
    kvkk_consent_at timestamptz not null,
    kvkk_consent_version text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null,
    server_updated_at timestamptz not null default now()
);

create trigger consents_set_server_updated_at
before insert or update on public.consents
for each row execute function public.set_server_updated_at();

alter table public.consents enable row level security;

create policy consents_select_own on public.consents
    for select using (auth.uid() = id);
create policy consents_insert_own on public.consents
    for insert with check (auth.uid() = id);
create policy consents_update_own on public.consents
    for update using (auth.uid() = id) with check (auth.uid() = id);

-- ============================================================
-- categories — system defaults (user_id null) + per-user custom categories
-- ============================================================
create table public.categories (
    id uuid primary key,
    user_id uuid references auth.users (id) on delete cascade, -- null = system default
    kind text not null check (kind in ('income', 'expense')),
    name text not null,
    color text not null,
    icon text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null,
    deleted_at timestamptz,
    server_updated_at timestamptz not null default now()
);

create trigger categories_set_server_updated_at
before insert or update on public.categories
for each row execute function public.set_server_updated_at();

create index categories_user_id_idx on public.categories (user_id);
create index categories_updated_at_idx on public.categories (updated_at);

alter table public.categories enable row level security;

-- Every signed-in user can read system defaults (user_id is null) plus their own.
create policy categories_select_own_or_default on public.categories
    for select using (user_id is null or auth.uid() = user_id);
create policy categories_insert_own on public.categories
    for insert with check (auth.uid() = user_id);
create policy categories_update_own on public.categories
    for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
-- No delete policy: categories are tombstoned via update (deleted_at), never
-- hard-deleted — past transactions must still resolve their category.
-- System default rows (user_id null) are seeded/managed via the service-role
-- key and are not writable by any user policy above.

-- ============================================================
-- transactions
-- ============================================================
create table public.transactions (
    id uuid primary key,
    user_id uuid not null references auth.users (id) on delete cascade,
    category_id uuid not null references public.categories (id),
    type text not null check (type in ('income', 'expense')),
    amount numeric(12, 2) not null,
    occurred_at date not null,
    note text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null,
    deleted_at timestamptz,
    server_updated_at timestamptz not null default now()
);

create trigger transactions_set_server_updated_at
before insert or update on public.transactions
for each row execute function public.set_server_updated_at();

create index transactions_user_id_idx on public.transactions (user_id);
create index transactions_category_id_idx on public.transactions (category_id);
create index transactions_updated_at_idx on public.transactions (updated_at);
create index transactions_occurred_at_idx on public.transactions (occurred_at);

alter table public.transactions enable row level security;

create policy transactions_select_own on public.transactions
    for select using (auth.uid() = user_id);
create policy transactions_insert_own on public.transactions
    for insert with check (auth.uid() = user_id);
create policy transactions_update_own on public.transactions
    for update using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- ============================================================
-- weekly_reports — read-only mirror on the client.
-- Written only by the Python/FastAPI service via the service-role key
-- (which bypasses RLS entirely); the client never inserts/updates this table,
-- so there is no insert/update policy for authenticated users.
-- ============================================================
create table public.weekly_reports (
    id uuid primary key,
    user_id uuid not null references auth.users (id) on delete cascade,
    week_start date not null,
    week_end date not null,
    contract_version text not null,
    facts_json jsonb not null,
    narration text,
    coach_tone_used text not null check (coach_tone_used in ('gentle', 'balanced', 'brutal')),
    profile_snapshot_json jsonb not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null,
    server_updated_at timestamptz not null default now()
);

create trigger weekly_reports_set_server_updated_at
before insert or update on public.weekly_reports
for each row execute function public.set_server_updated_at();

create index weekly_reports_user_id_idx on public.weekly_reports (user_id);
create index weekly_reports_week_start_idx on public.weekly_reports (week_start);
create index weekly_reports_updated_at_idx on public.weekly_reports (updated_at);

alter table public.weekly_reports enable row level security;

create policy weekly_reports_select_own on public.weekly_reports
    for select using (auth.uid() = user_id);
