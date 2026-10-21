"""Monthly job: auto-generate each active recurring row (Aboneliklerim) for the
month — mirrors monthly_income.py's per-row idempotent loop. An expense (or a
'budget' income) becomes a transaction of that type; a 'kumbara' income becomes a
manual_deposit Kumbara entry instead, matching what "Kumbaraya At" does in the app.
One row's failure never stops the batch.

Runs server-side for the same reason the income job does: a client-side "check on
app open" would risk two devices both creating the same month's payment.
"""

import uuid
from datetime import date

from app.date_utils import add_month, clamp_day
from app.supabase_client import SupabaseGateway


def process_recurring_payments(gateway: SupabaseGateway) -> dict:
    current_month = date.today().replace(day=1)
    processed: list[str] = []
    failed: list[dict] = []

    for payment in gateway.fetch_active_recurring_payments():
        payment_id = payment["id"]
        try:
            last_processed_month_str = payment.get("last_processed_month")
            month = (
                add_month(date.fromisoformat(last_processed_month_str))
                if last_processed_month_str
                else current_month
            )

            # The `or "expense"` fallback keeps this working against a database where the
            # type/destination migration hasn't been applied yet.
            transaction_type = payment.get("type") or "expense"
            to_kumbara = transaction_type == "income" and payment.get("destination") == "kumbara"

            while month <= current_month:
                occurred_at = clamp_day(month, payment["day_of_month"])
                # Same deterministic id for either target: it's per row and month, and
                # transactions / piggy_bank_entries never share ids.
                entry_id = str(uuid.uuid5(uuid.NAMESPACE_DNS, f"{payment_id}:{month.isoformat()}"))
                if to_kumbara:
                    gateway.ensure_recurring_income_deposit(entry_id, payment["user_id"], payment["amount"])
                else:
                    gateway.ensure_recurring_payment_transaction(
                        entry_id,
                        payment["user_id"],
                        payment.get("category_id"),
                        payment["amount"],
                        occurred_at,
                        transaction_type,
                    )
                month = add_month(month)

            gateway.update_recurring_payment_last_processed_month(payment_id, current_month)
            processed.append(payment_id)
        except Exception as exc:  # noqa: BLE001 - one payment's failure must not stop the batch
            failed.append({"payment_id": payment_id, "error": str(exc)})

    return {"month": current_month.isoformat(), "processed": len(processed), "failed": failed}
