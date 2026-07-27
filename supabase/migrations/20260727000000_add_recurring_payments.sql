-- Karınca — Aboneliklerim (recurring monthly payments).
--
-- Set up once (from a checkbox in Harcama Ekle on a normal expense), then the AI
-- service's monthly job auto-creates that same expense every month after. Cancel
-- flips `active` to false rather than deleting, so history stays intact.
--
-- last_processed_month is intentionally NOT in this table's client-facing shape in
-- spirit (mirrors profiles.last_auto_income_month) — it's read/written only by the
-- service via service-role; the client only ever sets/reads the other columns.

create table public.recurring_payments (
    id uuid primary key,
    user_id uuid not null references auth.users (id) on delete cascade,
    name text not null,
    amount numeric(12, 2) not null,
    category_id uuid references public.categories (id),
    day_of_month int not null check (day_of_month between 1 and 28),
    active boolean not null default true,
    last_processed_month date,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null,
    deleted_at timestamptz,
    server_updated_at timestamptz not null default now()
);

create trigger recurring_payments_set_server_updated_at
before insert or update on public.recurring_payments
for each row execute function public.set_server_updated_at();

create index recurring_payments_user_id_idx on public.recurring_payments (user_id);
create index recurring_payments_updated_at_idx on public.recurring_payments (updated_at);

alter table public.recurring_payments enable row level security;

create policy recurring_payments_select_own on public.recurring_payments
    for select using (auth.uid() = user_id);
create policy recurring_payments_insert_own on public.recurring_payments
    for insert with check (auth.uid() = user_id);
create policy recurring_payments_update_own on public.recurring_payments
    for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
-- No delete policy: cancel is a soft update (active = false / deleted_at), never a
-- hard delete.

grant select, insert, update on public.recurring_payments to authenticated;
grant select, update on public.recurring_payments to service_role;
