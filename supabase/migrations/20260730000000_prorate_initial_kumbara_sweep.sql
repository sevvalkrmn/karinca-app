-- Karınca — let a user tell onboarding how much of this month's income they'd
-- already spent before ever opening the app, and shrink the first-month Kumbara
-- sweep proportionally, instead of always sweeping the full income-limit
-- difference as if they were starting the month completely fresh on signup day.
--
-- The income transaction itself is unaffected (the salary really did arrive in
-- full) — only how much of it gets swept into Kumbara changes. Month 2+ is
-- untouched: ai-service/app/monthly_income.py's own salary_limit_diff +
-- month-end monthly_leftover settlement already accounts for real usage there.

alter table public.profiles add column if not exists initial_already_spent_amount numeric(12, 2);

create or replace function public.settle_initial_income()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_income numeric(12, 2) := new.regular_monthly_income;
  v_month date := date_trunc('month', now())::date;
  v_already_spent numeric(12, 2) := coalesce(new.initial_already_spent_amount, 0);
  v_remaining_fraction numeric(12, 6);
  v_delta numeric(12, 2);
  v_income_category_id uuid := 'f7a0e280-296e-4b37-876a-89611afea4be'; -- seeded "Maaş/Burs", same id as DEFAULT_INCOME_CATEGORY_ID in monthly_income.py
begin
  if v_income is null or v_income <= 0 then
    return new;
  end if;

  insert into public.transactions (id, user_id, category_id, type, amount, occurred_at, source, updated_at)
  values (gen_random_uuid(), new.id, v_income_category_id, 'income', v_income, v_month, 'auto_monthly_income', now());

  if new.reference_type = 'limit' and new.reference_amount is not null then
    v_remaining_fraction := greatest(1 - v_already_spent / v_income, 0);
    v_delta := (v_income - new.reference_amount) * v_remaining_fraction;
    if v_delta > 0 then
      insert into public.piggy_bank_entries (id, user_id, amount, reason, updated_at)
      values (gen_random_uuid(), new.id, v_delta, 'salary_limit_diff', now());
    end if;
  end if;

  update public.profiles set last_auto_income_month = v_month where id = new.id;

  return new;
end;
$$;
