-- The "Yeni Hedef" form now requires a target date (no longer optional), so enforce
-- that at the schema level too. Backfill any existing goal rows first (none are
-- expected yet, since the goals table only just shipped) so the NOT NULL constraint
-- can be added safely.

update public.goals set target_date = current_date + 90 where target_date is null;
alter table public.goals alter column target_date set not null;
