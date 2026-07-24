-- Tracks the last calendar date GoalRepository.evaluateProgress() confirmed a goal's
-- cumulative promised amount was still really covered by the user's balance. Null means
-- never evaluated yet (treated as the goal's creation date).

alter table public.goals add column last_evaluated_date date;
