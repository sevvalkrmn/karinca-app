-- Karınca — Kumbara (piggy bank) ledger.
--
-- Append-only by design: the displayed balance is SUM(amount), never a stored
-- mutable counter. Two independent writers — this client (overspend withdrawals)
-- and the server-side monthly job (salary/limit deposits) — only ever INSERT new
-- rows, so they can never clobber each other the way a shared last-write-wins
-- counter could if their syncs interleaved badly.

create table public.piggy_bank_entries (
    id uuid primary key,
    user_id uuid not null references auth.users (id) on delete cascade,
    amount numeric(12, 2) not null, -- positive = deposit, negative = withdrawal
    reason text not null check (reason in ('salary_limit_diff', 'monthly_leftover', 'overspend_withdrawal')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null,
    server_updated_at timestamptz not null default now()
);

create trigger piggy_bank_entries_set_server_updated_at
before insert or update on public.piggy_bank_entries
for each row execute function public.set_server_updated_at();

create index piggy_bank_entries_user_id_idx on public.piggy_bank_entries (user_id);
create index piggy_bank_entries_updated_at_idx on public.piggy_bank_entries (updated_at);

alter table public.piggy_bank_entries enable row level security;

create policy piggy_bank_entries_select_own on public.piggy_bank_entries
    for select using (auth.uid() = user_id);
create policy piggy_bank_entries_insert_own on public.piggy_bank_entries
    for insert with check (auth.uid() = user_id);
-- No update/delete policy anywhere, for anyone — immutable ledger.

grant select, insert on public.piggy_bank_entries to authenticated;
grant select, insert on public.piggy_bank_entries to service_role;
