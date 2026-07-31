import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    supabase_url: str
    supabase_service_role_key: str
    llm_api_key: str
    llm_provider: str
    llm_model: str
    report_cron_secret: str
    revenuecat_webhook_secret: str
    revenuecat_secret_api_key: str
    revenuecat_entitlement_id: str


def load_settings() -> Settings:
    return Settings(
        supabase_url=_require("SUPABASE_URL"),
        supabase_service_role_key=_require("SUPABASE_SERVICE_ROLE_KEY"),
        llm_api_key=_require("LLM_API_KEY"),
        llm_provider=os.environ.get("LLM_PROVIDER", "openai"),
        llm_model=os.environ.get("LLM_MODEL", "gpt-4o-mini"),
        report_cron_secret=_require("REPORT_CRON_SECRET"),
        # Optional, not _require()'d — RevenueCat isn't set up yet, and the rest of
        # this service (account deletion, monthly income, weekly reports) must keep
        # working regardless. Left blank, the webhook endpoint just never matches a
        # real "Bearer ..." header, so it stays effectively disabled until set.
        revenuecat_webhook_secret=os.environ.get("REVENUECAT_WEBHOOK_SECRET", ""),
        revenuecat_secret_api_key=os.environ.get("REVENUECAT_SECRET_API_KEY", ""),
        revenuecat_entitlement_id=os.environ.get("REVENUECAT_ENTITLEMENT_ID", "pro"),
    )


def _require(name: str) -> str:
    value = os.environ.get(name)
    if not value:
        raise RuntimeError(f"Missing required environment variable: {name}")
    return value
