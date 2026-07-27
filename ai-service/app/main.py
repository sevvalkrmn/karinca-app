from dotenv import load_dotenv

load_dotenv()  # before importing anything that reads env vars at import time

from fastapi import FastAPI, Header, HTTPException  # noqa: E402

from app.config import load_settings  # noqa: E402
from app.monthly_income import process_monthly_income  # noqa: E402
from app.recurring_payments import process_recurring_payments  # noqa: E402
from app.report_service import generate_all_reports  # noqa: E402
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
