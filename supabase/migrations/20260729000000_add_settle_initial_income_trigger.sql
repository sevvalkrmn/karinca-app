-- Karınca — settle the very first month's salary/Kumbara sweep immediately when a
-- profile is first saved (onboarding, or Kişisel Bilgiler for anyone who set their
-- profile up later), instead of waiting for the next monthly cron run.
--
-- This mirrors monthly_income.py's per-profile logic in SQL, tied to Postgres's own
-- INSERT semantics: an upsert only fires AFTER INSERT the first time a row is ever
-- created, never on later edits — so this can never double-fire, and there's no new
-- ai-service endpoint or client-invoked call needed (the AI service stays
-- cron-only, per CLAUDE.md). Setting last_auto_income_month here is what hands off
-- cleanly to the monthly cron: its catch-up loop starts from the month *after* the
-- one this trigger just settled, so it never revisits (and never duplicates) it.

create or replace function public.settle_initial_income()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_income numeric(12, 2) := new.regular_monthly_income;
  v_month date := date_trunc('month', now())::date;
  v_delta numeric(12, 2);
  v_income_category_id uuid := 'f7a0e280-296e-4b37-876a-89611afea4be'; -- seeded "Maaş/Burs", same id as DEFAULT_INCOME_CATEGORY_ID in monthly_income.py
begin
  if v_income is null or v_income <= 0 then
    return new;
  end if;

  insert into public.transactions (id, user_id, category_id, type, amount, occurred_at, source, updated_at)
  values (gen_random_uuid(), new.id, v_income_category_id, 'income', v_income, v_month, 'auto_monthly_income', now());

  if new.reference_type = 'limit' and new.reference_amount is not null then
    v_delta := v_income - new.reference_amount;
    if v_delta > 0 then
      insert into public.piggy_bank_entries (id, user_id, amount, reason, updated_at)
      values (gen_random_uuid(), new.id, v_delta, 'salary_limit_diff', now());
    end if;
  end if;

  update public.profiles set last_auto_income_month = v_month where id = new.id;

  return new;
end;
$$;

create trigger profiles_settle_initial_income
after insert on public.profiles
for each row execute function public.settle_initial_income();
