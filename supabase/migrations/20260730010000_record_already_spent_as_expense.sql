-- Karınca — the previous migration shrank the first-month Kumbara sweep by what the
-- user says they'd already spent before joining, but never actually recorded that
-- spending anywhere — so Home's "HARCANABİLİR" figure (all-time income minus all-time
-- expense transactions) never reflected it, still showing the full income as spendable.
-- Recording it as a real expense transaction (categorized "Diğer", since we don't know
-- its real breakdown) fixes that, and correctly counts toward this month's budget-used
-- percentage too. Dated the 1st of the month (like the income transaction), not "now" —
-- it wasn't spent today, so it must not inflate Home's "Günlük Harcama" (today-only) stat.

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
  v_income_category_id uuid := 'f7a0e280-296e-4b37-876a-89611afea4be'; -- seeded "Maaş/Burs"
  v_other_expense_category_id uuid := '9e401573-b7e3-452d-9924-49b8b5b87bfe'; -- seeded "Diğer" (expense)
begin
  if v_income is null or v_income <= 0 then
    return new;
  end if;

  insert into public.transactions (id, user_id, category_id, type, amount, occurred_at, source, updated_at)
  values (gen_random_uuid(), new.id, v_income_category_id, 'income', v_income, v_month, 'auto_monthly_income', now());

  if v_already_spent > 0 then
    insert into public.transactions (id, user_id, category_id, type, amount, occurred_at, source, updated_at)
    values (gen_random_uuid(), new.id, v_other_expense_category_id, 'expense', least(v_already_spent, v_income), v_month, 'onboarding_already_spent', now());
  end if;

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
