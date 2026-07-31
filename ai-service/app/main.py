from dotenv import load_dotenv

load_dotenv()  # before importing anything that reads env vars at import time

from fastapi import FastAPI, Header, HTTPException, Request  # noqa: E402

from app.config import load_settings  # noqa: E402
from app.monthly_income import process_monthly_income  # noqa: E402
from app.recurring_payments import process_recurring_payments  # noqa: E402
from app.report_service import generate_all_reports  # noqa: E402
from app.revenuecat import fetch_is_pro  # noqa: E402
from app.supabase_client import SupabaseGateway  # noqa: E402

settings = load_settings()
gateway = SupabaseGateway(settings.supabase_url, settings.supabase_service_role_key)

app = FastAPI(title="Karınca AI Coach Service")


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}


@app.post("/internal/generate-weekly-reports")
def generate_weekly_reports(x_cron_secret: str = Header(default="")) -> dict:
    # Server-scheduled cron calls this, authenticated by a shared secret — the
    # Android client never calls this endpoint directly.
    if x_cron_secret != settings.report_cron_secret:
        raise HTTPException(status_code=401, detail="invalid cron secret")
    return generate_all_reports(gateway, settings.llm_api_key, settings.llm_model)


@app.post("/internal/process-monthly-income")
def process_monthly_income_endpoint(x_cron_secret: str = Header(default="")) -> dict:
    if x_cron_secret != settings.report_cron_secret:
        raise HTTPException(status_code=401, detail="invalid cron secret")
    return process_monthly_income(gateway)


@app.post("/internal/process-recurring-payments")
def process_recurring_payments_endpoint(x_cron_secret: str = Header(default="")) -> dict:
    if x_cron_secret != settings.report_cron_secret:
        raise HTTPException(status_code=401, detail="invalid cron secret")
    return process_recurring_payments(gateway)


@app.post("/internal/revenuecat-webhook")
async def revenuecat_webhook(request: Request, authorization: str = Header(default="")) -> dict:
    # RevenueCat, not our own cron, calls this — authenticated by the "Authorization
    # header value" configured in the RevenueCat dashboard, compared here the same
    # way X-Cron-Secret is above.
    if authorization != f"Bearer {settings.revenuecat_webhook_secret}":
        raise HTTPException(status_code=401, detail="invalid webhook secret")

    body = await request.json()
    app_user_id = body["event"]["app_user_id"]

    # Every event type is treated as just a trigger to re-check RevenueCat's own
    # subscriber record, which already accounts for grace periods, billing retries,
    # and refunds — no need to hand-interpret each event type ourselves.
    is_pro = fetch_is_pro(app_user_id, settings.revenuecat_entitlement_id, settings.revenuecat_secret_api_key)
    gateway.upsert_entitlement(app_user_id, is_pro, app_user_id)
    return {"user_id": app_user_id, "is_pro": is_pro}


@app.post("/account/delete")
def delete_account(authorization: str = Header(default="")) -> dict:
    # Unlike every other endpoint above, this one IS client-invoked — authenticated
    # by the caller's own Supabase access token (verified against Supabase itself),
    # not a shared secret, since it's the user deleting their own account.
    token = authorization.removeprefix("Bearer ").strip()
    if not token:
        raise HTTPException(status_code=401, detail="missing token")
    try:
        user_id = gateway.verify_user_jwt(token)
    except Exception:
        raise HTTPException(status_code=401, detail="invalid token")
    gateway.delete_user(user_id)
    return {"deleted": user_id}
