-- Karınca — Pro entitlement (RevenueCat-backed paywall for the AI weekly report).
--
-- Separate table rather than an is_pro column on profiles: profiles grants
-- `authenticated` UPDATE via the profiles_update_own RLS policy, and RLS restricts
-- rows, not columns — so a plain column there could be self-granted by any client
-- through the normal sync push pipeline. Here `authenticated` only ever gets SELECT;
-- every write comes from the RevenueCat webhook handler via service_role.
--
-- No row for a user means "not Pro" — every reader must treat a missing row the
-- same as is_pro = false, not as an error.

create table public.entitlements (
    user_id uuid primary key references auth.users (id) on delete cascade,
    is_pro boolean not null default false,
    revenuecat_customer_id text,
    updated_at timestamptz not null default now(),
    server_updated_at timestamptz not null default now()
);

create trigger entitlements_set_server_updated_at
before insert or update on public.entitlements
for each row execute function public.set_server_updated_at();

alter table public.entitlements enable row level security;

create policy entitlements_select_own on public.entitlements
    for select using (auth.uid() = user_id);
-- No insert/update policy for authenticated: entitlement truth is written
-- exclusively by the RevenueCat webhook handler via service_role.

grant select on public.entitlements to authenticated;
grant select, insert, update on public.entitlements to service_role;
