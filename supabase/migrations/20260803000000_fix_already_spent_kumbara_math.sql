-- Karınca — the previous version of settle_initial_income() shrank the first-month
-- Kumbara sweep proportionally from the very first lira of "already spent" reported
-- at onboarding, even while still well under the limit. That's wrong: nothing about
-- the savings target should change until spending has actually eaten past the
-- limit itself. Corrected model:
--   - already_spent <= limit: Kumbara sweep is untouched (income - limit, exactly
--     as if the "already spent" question had never been asked) — the user hasn't
--     touched their savings portion yet.
--   - limit < already_spent <= income: Kumbara only gets what's left of income
--     after the overspend (income - already_spent) — some of what should have
--     been saved has already been eaten into.
--   - already_spent >= income: nothing left to sweep, Kumbara gets 0.

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
  v_delta numeric(12, 2);
  v_income_category_id uuid := 'f7a0e280-296e-4b37-876a-89611afea4be';
  v_other_expense_category_id uuid := '9e401573-b7e3-452d-9924-49b8b5b87bfe';
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
    v_delta := v_income - least(greatest(v_already_spent, new.reference_amount), v_income);
    if v_delta > 0 then
      insert into public.piggy_bank_entries (id, user_id, amount, reason, updated_at)
      values (gen_random_uuid(), new.id, v_delta, 'salary_limit_diff', now());
    end if;
  end if;

  update public.profiles set last_auto_income_month = v_month where id = new.id;

  return new;
end;
$$;
