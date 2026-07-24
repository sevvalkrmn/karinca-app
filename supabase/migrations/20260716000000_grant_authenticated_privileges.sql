-- Karınca — grant baseline table privileges to the authenticated role.
--
-- RLS policies control WHICH rows a role can see/touch, but Postgres requires the role to
-- hold the underlying GRANT on the table before RLS is even evaluated — tables created via
-- raw SQL (as opposed to the Supabase dashboard's table editor) don't get this
-- automatically. Missing this caused "permission denied for table X" on every client
-- request, regardless of how correct the RLS policies were.
--
-- No DELETE grants anywhere: every table's RLS intentionally has no delete policy
-- (deletes are soft, via UPDATE .. deleted_at). weekly_reports gets SELECT only — it's
-- written exclusively by the Python service via the service-role key, which bypasses
-- grants and RLS entirely.

grant usage on schema public to authenticated;

grant select, insert, update on public.profiles to authenticated;
grant select, insert, update on public.consents to authenticated;
grant select, insert, update on public.categories to authenticated;
grant select, insert, update on public.transactions to authenticated;
grant select on public.weekly_reports to authenticated;
