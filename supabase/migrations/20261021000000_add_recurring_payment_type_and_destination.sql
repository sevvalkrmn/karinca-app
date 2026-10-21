-- Karınca — recurring incomes alongside recurring payments.
--
-- The "tekrar eden" checkbox now exists in Gelir Ekle too, so a recurring row can be
-- an income as well as an expense. `type` mirrors transactions.type; `destination`
-- only matters for incomes and remembers the "Mevcut Bütçeye Ekle" / "Kumbaraya At"
-- choice made on the first entry, so the monthly job repeats the same thing: a
-- 'budget' income becomes an income transaction, a 'kumbara' one becomes a
-- manual_deposit Kumbara entry. Existing rows keep behaving exactly as before via
-- the defaults.

alter table public.recurring_payments
    add column type text not null default 'expense' check (type in ('expense', 'income')),
    add column destination text not null default 'budget' check (destination in ('budget', 'kumbara'));
