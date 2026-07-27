"""Orchestrates one cron run: figure out the week, compute facts + narrate for
every consented user, and make sure one user's failure never stops the batch.
"""

import uuid
from datetime import date, timedelta

from app.financial_engine import FinancialEngine
from app.models import ProfileSnapshot
from app.narrator import narrate
from app.supabase_client import SupabaseGateway


def last_completed_week(today: date) -> tuple[date, date]:
    """Monday-Sunday weeks. Run any day after a week ends and this always resolves
    to that most recently completed week, never the week still in progress."""
    this_monday = today - timedelta(days=today.weekday())
    week_start = this_monday - timedelta(days=7)
    week_end = week_start + timedelta(days=6)
    return week_start, week_end


def generate_all_reports(gateway: SupabaseGateway, llm_api_key: str, llm_model: str) -> dict:
    engine = FinancialEngine(gateway)
    week_start, week_end = last_completed_week(date.today())

    profiles = gateway.fetch_consented_profiles()
    pro_user_ids = gateway.fetch_pro_user_ids()
    generated: list[str] = []
    skipped_non_pro: list[str] = []
    failed: list[dict] = []

    for profile in profiles:
        user_id = profile["id"]
        if user_id not in pro_user_ids:
            skipped_non_pro.append(user_id)
            continue
        try:
            facts = engine.compute_facts(user_id, profile, week_start, week_end)
            profile_snapshot = ProfileSnapshot(
                coach_tone=profile["coach_tone"],
                reference_type=profile["reference_type"],
                reference_amount=profile["reference_amount"],
            )

            # Reuse the existing row's id if this week was already generated once
            # (e.g. a retried/rerun cron), so upsert's ON CONFLICT genuinely
            # updates in place instead of fighting over which id wins.
            report_id = gateway.fetch_existing_report_id(user_id, facts.week_start) or str(uuid.uuid4())
            gateway.upsert_weekly_report(
                {
                    "id": report_id,
                    "user_id": user_id,
                    "week_start": facts.week_start,
                    "week_end": facts.week_end,
                    "contract_version": facts.contract_version,
                    "facts_json": facts.model_dump(mode="json"),
                    "narration": None,
                    "coach_tone_used": profile_snapshot.coach_tone,
                    "profile_snapshot_json": profile_snapshot.model_dump(mode="json"),
                }
            )

            # Facts + row are already durable at this point — if narrate() fails
            # (timeout, rate limit), the row still exists with narration=null and
            # the client already renders that as "hazırlanıyor" rather than an error.
            narration = narrate(facts, profile_snapshot, llm_api_key, llm_model)
            gateway.update_narration(report_id, narration)
            generated.append(user_id)
        except Exception as exc:  # noqa: BLE001 - one user's failure must not stop the batch
            failed.append({"user_id": user_id, "error": str(exc)})

    return {
        "week_start": week_start.isoformat(),
        "week_end": week_end.isoformat(),
        "generated": len(generated),
        "skipped_non_pro": len(skipped_non_pro),
        "failed": failed,
    }
