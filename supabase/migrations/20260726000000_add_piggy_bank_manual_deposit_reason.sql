-- Adds a fourth Kumbara ledger reason: a manual deposit when the user chooses
-- "Kumbaraya At" instead of "Mevcut Bütçeye Ekle" while logging a new income —
-- no transaction is created for that income, this deposit is its only record.

alter table public.piggy_bank_entries drop constraint piggy_bank_entries_reason_check;
alter table public.piggy_bank_entries add constraint piggy_bank_entries_reason_check
    check (reason in ('salary_limit_diff', 'monthly_leftover', 'overspend_withdrawal', 'overspend_reversal', 'manual_deposit'));
