"""The versioned ReportFacts contract.

Every number the narrator is allowed to reference lives here — computed once by
FinancialEngine, stored verbatim as `facts_json`, and handed to narrate() alongside
ProfileSnapshot. Bump `contract_version` if a field is ever renamed, removed, or
reinterpreted, so old stored reports stay decodable under their own version.
"""

from pydantic import BaseModel


class CategoryBreakdownItem(BaseModel):
    category_name: str
    total: float
    transaction_count: int
    pct_of_expense: float


class WeekComparison(BaseModel):
    expense_delta: float
    expense_delta_pct: float
    income_delta: float


class AnomalyFlag(BaseModel):
    category_name: str
    this_week_total: float
    trailing_avg: float
    pct_above_avg: float


class BudgetStatus(BaseModel):
    reference_type: str  # "goal" | "limit"
    reference_amount: float
    month_expense_so_far: float
    pct_used: float


class GoalSnapshot(BaseModel):
    name: str
    target_amount: float
    current_amount: float
    target_date: str
    pct_complete: float


class SavingsSuggestion(BaseModel):
    category_name: str
    current_weekly_spend: float
    suggested_cut_pct: float
    suggested_savings_try: float


class ReportFacts(BaseModel):
    contract_version: str = "v1"
    week_start: str
    week_end: str
    income_total: float
    expense_total: float
    net: float
    category_breakdown: list[CategoryBreakdownItem]
    # None whenever there isn't a full previous week of data yet — the "getting to
    # know you" case (RPT-06). Never a fabricated 0%.
    vs_last_week: WeekComparison | None = None
    anomaly: AnomalyFlag | None = None
    budget_status: BudgetStatus | None = None
    goals_snapshot: list[GoalSnapshot] = []
    savings_suggestion: SavingsSuggestion | None = None


class ProfileSnapshot(BaseModel):
    """Frozen at generation time — deliberately just enough for narrate() to set tone
    and reference the user's own target, never anything identifying (no name, email,
    phone, user id)."""

    coach_tone: str  # "gentle" | "balanced" | "brutal"
    reference_type: str  # "goal" | "limit"
    reference_amount: float
