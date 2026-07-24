"""Monthly job: auto-generate each user's salary transaction and, for
LIMIT-referenced profiles, sweep the salary/limit difference and any month-end
leftover into their Kumbara (piggy bank). One user's failure never stops the batch.

Runs server-side (not client-side, unlike Goals' evaluateProgress) specifically so
two devices opening the app around the same time can never both create the same
month's salary transaction — there's exactly one process doing this, ever.
"""

import uuid
from datetime import date, timedelta

from app.supabase_client import SupabaseGateway

DEFAULT_INCOME_CATEGORY_ID = "f7a0e280-296e-4b37-876a-89611afea4be"  # seeded "Maaş/Burs"


def _add_month(d: date) -> date:
    if d.month == 12:
        return date(d.year + 1, 1, 1)
    return date(d.year, d.month + 1, 1)


def _month_end(d: date) -> date:
    return _add_month(d) - timedelta(days=1)


def process_monthly_income(gateway: SupabaseGateway) -> dict:
    current_month = date.today().replace(day=1)
    processed: list[str] = []
    failed: list[dict] = []

    for profile in gateway.fetch_all_profiles():
        user_id = profile["id"]
        try:
            income = profile.get("regular_monthly_income")
            if income is None or income <= 0:
                continue

            is_limit = profile.get("reference_type") == "limit"
            reference_amount = profile.get("reference_amount")

            last_income_month_str = profile.get("last_auto_income_month")
            month = _add_month(date.fromisoformat(last_income_month_str)) if last_income_month_str else current_month

            while month <= current_month:
                transaction_id = str(uuid.uuid5(uuid.NAMESPACE_DNS, f"{user_id}:auto_income:{month.isoformat()}"))
                created = gateway.ensure_auto_income_transaction(
                    transaction_id, user_id, DEFAULT_INCOME_CATEGORY_ID, income, month
                )

                # The immediate "maaş geldi, limit belirlendi" sweep — only the first
                # time this month's salary is actually created, never on a re-run.
                if created and is_limit and reference_amount is not None:
                    delta = income - reference_amount
                    if delta > 0:
                        gateway.insert_piggy_bank_entry(user_id, delta, "salary_limit_diff")

                # Settle the month we're leaving, once it's fully in the past — its
                # real expense total is now final.
                if month < current_month and is_limit and reference_amount is not None:
                    actual_expense = gateway.fetch_total_expense(user_id, month, _month_end(month))
                    leftover = max(reference_amount - actual_expense, 0.0)
                    if leftover > 0:
                        gateway.insert_piggy_bank_entry(user_id, leftover, "monthly_leftover")

                month = _add_month(month)

            gateway.update_last_auto_income_month(user_id, current_month)
            processed.append(user_id)
        except Exception as exc:  # noqa: BLE001 - one user's failure must not stop the batch
            failed.append({"user_id": user_id, "error": str(exc)})

    return {"month": current_month.isoformat(), "processed": len(processed), "failed": failed}
