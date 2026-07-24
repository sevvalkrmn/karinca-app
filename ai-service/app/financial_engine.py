"""Every number in a report traces back here. The narrator never computes or
invents a figure — it only narrates whatever ReportFacts this module produces.
"""

from collections import defaultdict
from datetime import date, timedelta

from app.models import (
    AnomalyFlag,
    BudgetStatus,
    CategoryBreakdownItem,
    GoalSnapshot,
    ReportFacts,
    SavingsSuggestion,
    WeekComparison,
)
from app.supabase_client import SupabaseGateway

TOP_CATEGORY_LIMIT = 5
TRAILING_WEEKS = 4
ANOMALY_THRESHOLD_PCT = 50.0
SUGGESTED_CUT_PCT = 10.0
OTHER_CATEGORY_NAME = "Diğer"


class FinancialEngine:
    def __init__(self, gateway: SupabaseGateway) -> None:
        self._gateway = gateway

    def compute_facts(self, user_id: str, profile: dict, week_start: date, week_end: date) -> ReportFacts:
        # Pull enough trailing history for the 4-week anomaly average in one query,
        # then slice it into weeks in memory rather than re-querying per week.
        history_start = week_start - timedelta(days=7 * TRAILING_WEEKS)
        all_txns = self._gateway.fetch_transactions(user_id, history_start, week_end)

        category_ids = {t["category_id"] for t in all_txns if t["category_id"]}
        categories_by_id = self._gateway.fetch_categories_by_ids(list(category_ids))

        this_week_txns = [t for t in all_txns if week_start <= t["occurred_at"] <= week_end]
        income_total = _sum_by_type(this_week_txns, "income")
        expense_total = _sum_by_type(this_week_txns, "expense")

        category_breakdown = self._category_breakdown(this_week_txns, categories_by_id, expense_total)

        last_week_start = week_start - timedelta(days=7)
        last_week_end = week_end - timedelta(days=7)
        last_week_txns = [t for t in all_txns if last_week_start <= t["occurred_at"] <= last_week_end]
        vs_last_week = _week_comparison(this_week_txns, last_week_txns) if last_week_txns else None

        anomaly = self._detect_anomaly(all_txns, this_week_txns, categories_by_id, week_start)
        budget_status = self._budget_status(user_id, profile, week_end)
        goals_snapshot = self._goals_snapshot(user_id)
        savings_suggestion = _savings_suggestion(category_breakdown)

        return ReportFacts(
            week_start=week_start.isoformat(),
            week_end=week_end.isoformat(),
            income_total=income_total,
            expense_total=expense_total,
            net=income_total - expense_total,
            category_breakdown=category_breakdown,
            vs_last_week=vs_last_week,
            anomaly=anomaly,
            budget_status=budget_status,
            goals_snapshot=goals_snapshot,
            savings_suggestion=savings_suggestion,
        )

    def _category_breakdown(
        self, week_txns: list[dict], categories_by_id: dict[str, dict], expense_total: float
    ) -> list[CategoryBreakdownItem]:
        totals: dict[str, float] = defaultdict(float)
        counts: dict[str, int] = defaultdict(int)
        for t in week_txns:
            if t["type"] != "expense":
                continue
            name = _category_name(t, categories_by_id)
            totals[name] += t["amount"]
            counts[name] += 1

        ranked = sorted(totals.items(), key=lambda kv: kv[1], reverse=True)
        top, rest = ranked[:TOP_CATEGORY_LIMIT], ranked[TOP_CATEGORY_LIMIT:]

        items = [
            CategoryBreakdownItem(
                category_name=name,
                total=total,
                transaction_count=counts[name],
                pct_of_expense=(total / expense_total * 100) if expense_total > 0 else 0,
            )
            for name, total in top
        ]
        if rest:
            rest_total = sum(total for _, total in rest)
            rest_count = sum(counts[name] for name, _ in rest)
            items.append(
                CategoryBreakdownItem(
                    category_name=OTHER_CATEGORY_NAME,
                    total=rest_total,
                    transaction_count=rest_count,
                    pct_of_expense=(rest_total / expense_total * 100) if expense_total > 0 else 0,
                )
            )
        return items

    def _detect_anomaly(
        self,
        all_txns: list[dict],
        this_week_txns: list[dict],
        categories_by_id: dict[str, dict],
        week_start: date,
    ) -> AnomalyFlag | None:
        this_week_totals: dict[str, float] = defaultdict(float)
        for t in this_week_txns:
            if t["type"] == "expense":
                this_week_totals[_category_name(t, categories_by_id)] += t["amount"]

        # category -> {weeks_back: total}, only for the TRAILING_WEEKS before week_start.
        trailing: dict[str, dict[int, float]] = defaultdict(lambda: defaultdict(float))
        for t in all_txns:
            if t["type"] != "expense" or t["occurred_at"] >= week_start:
                continue
            weeks_back = (week_start - t["occurred_at"]).days // 7
            if weeks_back < TRAILING_WEEKS:
                trailing[_category_name(t, categories_by_id)][weeks_back] += t["amount"]

        best: AnomalyFlag | None = None
        for name, this_total in this_week_totals.items():
            history = trailing.get(name)
            # Need at least 2 distinct prior weeks of data before a trailing average
            # means anything — otherwise a single big prior week could itself look
            # like the anomaly.
            if not history or len(history) < 2:
                continue
            avg = sum(history.values()) / TRAILING_WEEKS
            if avg <= 0:
                continue
            pct_above = (this_total - avg) / avg * 100
            if pct_above >= ANOMALY_THRESHOLD_PCT and (best is None or pct_above > best.pct_above_avg):
                best = AnomalyFlag(
                    category_name=name,
                    this_week_total=this_total,
                    trailing_avg=avg,
                    pct_above_avg=pct_above,
                )
        return best

    def _budget_status(self, user_id: str, profile: dict, week_end: date) -> BudgetStatus | None:
        reference_amount = profile.get("reference_amount")
        if reference_amount is None:
            return None
        month_expense = self._gateway.fetch_month_expense(user_id, week_end)
        return BudgetStatus(
            reference_type=profile["reference_type"],
            reference_amount=reference_amount,
            month_expense_so_far=month_expense,
            pct_used=(month_expense / reference_amount * 100) if reference_amount > 0 else 0,
        )

    def _goals_snapshot(self, user_id: str) -> list[GoalSnapshot]:
        goals = self._gateway.fetch_active_goals(user_id)
        return [
            GoalSnapshot(
                name=g["name"],
                target_amount=g["target_amount"],
                current_amount=g["current_amount"],
                target_date=g["target_date"],
                pct_complete=(g["current_amount"] / g["target_amount"] * 100) if g["target_amount"] > 0 else 0,
            )
            for g in goals
        ]


def _category_name(txn: dict, categories_by_id: dict[str, dict]) -> str:
    category = categories_by_id.get(txn["category_id"])
    return category["name"] if category else OTHER_CATEGORY_NAME


def _sum_by_type(txns: list[dict], type_: str) -> float:
    return sum(t["amount"] for t in txns if t["type"] == type_)


def _week_comparison(this_week_txns: list[dict], last_week_txns: list[dict]) -> WeekComparison:
    this_expense = _sum_by_type(this_week_txns, "expense")
    last_expense = _sum_by_type(last_week_txns, "expense")
    expense_delta = this_expense - last_expense
    return WeekComparison(
        expense_delta=expense_delta,
        expense_delta_pct=(expense_delta / last_expense * 100) if last_expense > 0 else 0,
        income_delta=_sum_by_type(this_week_txns, "income") - _sum_by_type(last_week_txns, "income"),
    )


def _savings_suggestion(category_breakdown: list[CategoryBreakdownItem]) -> SavingsSuggestion | None:
    if not category_breakdown:
        return None
    biggest = max(category_breakdown, key=lambda c: c.total)
    if biggest.total <= 0:
        return None
    return SavingsSuggestion(
        category_name=biggest.category_name,
        current_weekly_spend=biggest.total,
        suggested_cut_pct=SUGGESTED_CUT_PCT,
        suggested_savings_try=biggest.total * SUGGESTED_CUT_PCT / 100,
    )
