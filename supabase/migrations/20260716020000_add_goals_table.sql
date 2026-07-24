-- Karınca — Kumbaram (savings goals) table.
--
-- Independent of profiles.reference_type/reference_amount (the single financial
-- reference used for the weekly AI coach). Goals are a separate, user-managed list
-- of named savings targets (e.g. "Yeni Bisiklet"), each tracked with its own
-- target/current amount — there can be any number of them per user.

create table public.goals (
    id uuid primary key,
    user_id uuid not null references auth.users (id) on delete cascade,
    name text not null,
    icon text not null,
    target_amount numeric(12, 2) not null,
    current_amount numeric(12, 2) not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null,
    deleted_at timestamptz,
    completed_at timestamptz,
    server_updated_at timestamptz not null default now()
);

create trigger goals_set_server_updated_at
before insert or update on public.goals
for each row execute function public.set_server_updated_at();

create index goals_user_id_idx on public.goals (user_id);
create index goals_updated_at_idx on public.goals (updated_at);

alter table public.goals enable row level security;

create policy goals_select_own on public.goals
    for select using (auth.uid() = user_id);
create policy goals_insert_own on public.goals
    for insert with check (auth.uid() = user_id);
create policy goals_update_own on public.goals
    for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
-- No delete policy: goals are tombstoned via update (deleted_at), never hard-deleted.

grant select, insert, update on public.goals to authenticated;
