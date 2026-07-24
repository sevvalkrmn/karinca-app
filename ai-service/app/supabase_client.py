"""All raw Supabase reads/writes for the coach service, via the service-role key
(bypasses RLS entirely — the client app never has this key). Aggregation itself
happens in FinancialEngine on the plain rows returned here, not in SQL, since a
single user's weekly transaction volume is tiny and this keeps every query a plain
PostgREST select/filter with no stored procedures to maintain.
"""

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


def _now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()
