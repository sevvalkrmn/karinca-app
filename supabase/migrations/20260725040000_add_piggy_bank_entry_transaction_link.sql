-- Links an overspend_withdrawal entry back to the transaction that caused it, so
-- deleting that transaction can reverse the withdrawal (via a compensating entry —
-- the ledger stays append-only, nothing is ever updated or deleted in place).
-- Null for server-deposited entries (salary_limit_diff, monthly_leftover), which
-- have no single transaction behind them.

alter table public.piggy_bank_entries add column transaction_id uuid references public.transactions (id);

alter table public.piggy_bank_entries drop constraint piggy_bank_entries_reason_check;
alter table public.piggy_bank_entries add constraint piggy_bank_entries_reason_check
    check (reason in ('salary_limit_diff', 'monthly_leftover', 'overspend_withdrawal', 'overspend_reversal'));
