-- Makes the AI coach service's weekly cron run idempotent: without this, a rerun
-- (retry after a partial failure, or an accidental double-trigger) could insert a
-- second report row for the same user + week instead of updating the existing one.
-- The service upserts on this exact conflict target.

alter table public.weekly_reports
    add constraint weekly_reports_user_week_unique unique (user_id, week_start);
