-- Distinguishes the server-generated monthly salary transaction from every normal
-- manually-logged one. Null for all existing/manual rows; only ever set to
-- 'auto_monthly_income' by the AI service's monthly job (service-role), which also
-- uses it to check "have I already created this month's salary?" before inserting.

alter table public.transactions add column source text;
