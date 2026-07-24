-- Adds an optional target date to savings goals, so the user can say by when
-- they want to reach a goal (e.g. "Yeni Bisiklet" by 2026-12-01).

alter table public.goals add column target_date date;
