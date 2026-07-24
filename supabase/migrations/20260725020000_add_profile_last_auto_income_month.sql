-- Tracks the last calendar month (first-of-month date) the AI service's monthly job
-- has already created a salary transaction + settled the limit for. Read/written
-- only by the service via service-role — the client never touches this column, so
-- it isn't added to ProfileEntity/ProfileDto.

alter table public.profiles add column last_auto_income_month date;
