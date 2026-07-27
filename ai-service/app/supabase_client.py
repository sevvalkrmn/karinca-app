"""All raw Supabase reads/writes for the coach service, via the service-role key
(bypasses RLS entirely — the client app never has this key). Aggregation itself
happens in FinancialEngine on the plain rows returned here, not in SQL, since a
single user's weekly transaction volume is tiny and this keeps every query a plain
PostgREST select/filter with no stored procedures to maintain.
"""

import uuid
from datetime import date, datetime, timezone

from supabase import Client, create_client


class SupabaseGateway:
    def __init__(self, url: str, service_role_key: str) -> None:
        self._client: Client = create_client(url, service_role_key)

    def fetch_consented_profiles(self) -> list[dict]:
        """Users with both a completed profile AND recorded KVKK consent — the two
        gates required before a report is worth generating at all."""
        consented_ids = {row["id"] for row in self._client.table("consents").select("id").execute().data}
        profiles = self._client.table("profiles").select("*").execute().data
        return [p for p in profiles if p["id"] in consented_ids]

    def fetch_transactions(self, user_id: str, from_date: date, to_date: date) -> list[dict]:
        rows = (
            self._client.table("transactions")
            .select("category_id, type, amount, occurred_at")
            .eq("user_id", user_id)
            .is_("deleted_at", "null")
            .gte("occurred_at", from_date.isoformat())
            .lte("occurred_at", to_date.isoformat())
            .execute()
            .data
        )
        for row in rows:
            row["occurred_at"] = date.fromisoformat(row["occurred_at"])
        return rows

    def fetch_categories_by_ids(self, category_ids: list[str]) -> dict[str, dict]:
        if not category_ids:
            return {}
        rows = self._client.table("categories").select("id, name, kind").in_("id", category_ids).execute().data
        return {row["id"]: row for row in rows}

    def fetch_month_expense(self, user_id: str, as_of: date) -> float:
        month_start = as_of.replace(day=1)
        rows = (
            self._client.table("transactions")
            .select("amount")
            .eq("user_id", user_id)
            .eq("type", "expense")
            .is_("deleted_at", "null")
            .gte("occurred_at", month_start.isoformat())
            .lte("occurred_at", as_of.isoformat())
            .execute()
            .data
        )
        return sum(row["amount"] for row in rows)

    def fetch_active_goals(self, user_id: str) -> list[dict]:
        return (
            self._client.table("goals")
            .select("name, target_amount, current_amount, target_date")
            .eq("user_id", user_id)
            .is_("deleted_at", "null")
            .is_("completed_at", "null")
            .execute()
            .data
        )

    def fetch_existing_report_id(self, user_id: str, week_start_iso: str) -> str | None:
        rows = (
            self._client.table("weekly_reports")
            .select("id")
            .eq("user_id", user_id)
            .eq("week_start", week_start_iso)
            .execute()
            .data
        )
        return rows[0]["id"] if rows else None

    def upsert_weekly_report(self, row: dict) -> None:
        row = {**row, "updated_at": _now_iso()}
        self._client.table("weekly_reports").upsert(row, on_conflict="user_id,week_start").execute()

    def update_narration(self, report_id: str, narration: str) -> None:
        self._client.table("weekly_reports").update(
            {"narration": narration, "updated_at": _now_iso()}
        ).eq("id", report_id).execute()

    # ---------- monthly income / Kumbara ----------

    def fetch_all_profiles(self) -> list[dict]:
        """Every profile, no KVKK gate — auto-income generation isn't LLM-related,
        so it doesn't need the consent gate the weekly report does."""
        return self._client.table("profiles").select("*").execute().data

    def fetch_total_expense(self, user_id: str, from_date: date, to_date: date) -> float:
        rows = (
            self._client.table("transactions")
            .select("amount")
            .eq("user_id", user_id)
            .eq("type", "expense")
            .is_("deleted_at", "null")
            .gte("occurred_at", from_date.isoformat())
            .lte("occurred_at", to_date.isoformat())
            .execute()
            .data
        )
        return sum(row["amount"] for row in rows)

    def ensure_auto_income_transaction(
        self, transaction_id: str, user_id: str, category_id: str, amount: float, occurred_at: date
    ) -> bool:
        """Returns True only if this call actually inserted the row — False if it
        already existed (deterministic id from the caller makes this check exact,
        not just best-effort)."""
        existing = self._client.table("transactions").select("id").eq("id", transaction_id).execute().data
        if existing:
            return False
        now = _now_iso()
        self._client.table("transactions").insert(
            {
                "id": transaction_id,
                "user_id": user_id,
                "category_id": category_id,
                "type": "income",
                "amount": amount,
                "occurred_at": occurred_at.isoformat(),
                "source": "auto_monthly_income",
                "created_at": now,
                "updated_at": now,
            }
        ).execute()
        return True

    def insert_piggy_bank_entry(self, user_id: str, amount: float, reason: str) -> None:
        now = _now_iso()
        self._client.table("piggy_bank_entries").insert(
            {
                "id": str(uuid.uuid4()),
                "user_id": user_id,
                "amount": amount,
                "reason": reason,
                "created_at": now,
                "updated_at": now,
            }
        ).execute()

    def update_last_auto_income_month(self, user_id: str, month: date) -> None:
        self._client.table("profiles").update(
            {"last_auto_income_month": month.isoformat()}
        ).eq("id", user_id).execute()

    # ---------- recurring payments (Aboneliklerim) ----------

    def fetch_active_recurring_payments(self) -> list[dict]:
        """active = true and not soft-deleted — cancelling from the client sets
        active = false, which is enough to stop this job from ever seeing it again."""
        return (
            self._client.table("recurring_payments")
            .select("*")
            .eq("active", True)
            .is_("deleted_at", "null")
            .execute()
            .data
        )

    def ensure_recurring_payment_transaction(
        self, transaction_id: str, user_id: str, category_id: str | None, amount: float, occurred_at: date
    ) -> bool:
        """Same check-then-insert idempotency as ensure_auto_income_transaction, just
        for an expense instead of the fixed income category."""
        existing = self._client.table("transactions").select("id").eq("id", transaction_id).execute().data
        if existing:
            return False
        now = _now_iso()
        self._client.table("transactions").insert(
            {
                "id": transaction_id,
                "user_id": user_id,
                "category_id": category_id,
                "type": "expense",
                "amount": amount,
                "occurred_at": occurred_at.isoformat(),
                "source": "auto_recurring_payment",
                "created_at": now,
                "updated_at": now,
            }
        ).execute()
        return True

    def update_recurring_payment_last_processed_month(self, payment_id: str, month: date) -> None:
        self._client.table("recurring_payments").update(
            {"last_processed_month": month.isoformat()}
        ).eq("id", payment_id).execute()

    # ---------- Pro entitlement ----------

    def fetch_pro_user_ids(self) -> set[str]:
        rows = self._client.table("entitlements").select("user_id").eq("is_pro", True).execute().data
        return {row["user_id"] for row in rows}

    def upsert_entitlement(self, user_id: str, is_pro: bool, revenuecat_customer_id: str) -> None:
        self._client.table("entitlements").upsert(
            {
                "user_id": user_id,
                "is_pro": is_pro,
                "revenuecat_customer_id": revenuecat_customer_id,
                "updated_at": _now_iso(),
            }
        ).execute()


def _now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()
