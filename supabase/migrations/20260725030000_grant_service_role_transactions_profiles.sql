-- The monthly income job needs to INSERT the auto-generated salary transaction and
-- UPDATE profiles.last_auto_income_month — the earlier service_role grants migration
-- only gave it SELECT on both, which was enough for the weekly report but not this.

grant insert on public.transactions to service_role;
grant update on public.profiles to service_role;
