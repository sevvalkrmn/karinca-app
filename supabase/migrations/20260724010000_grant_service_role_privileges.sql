-- The service-role key bypasses RLS, but RLS bypass is a separate mechanism from
-- the baseline table GRANT — Postgres still checks GRANT before RLS is even
-- evaluated, exactly like the earlier authenticated-role grants migration explains.
-- Nothing ever granted service_role anything, so the AI coach service got
-- "permission denied for table X" on its very first real query.

grant usage on schema public to service_role;

grant select on public.profiles to service_role;
grant select on public.consents to service_role;
grant select on public.categories to service_role;
grant select on public.transactions to service_role;
grant select on public.goals to service_role;
grant select, insert, update on public.weekly_reports to service_role;
