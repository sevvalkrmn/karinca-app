"""One call: ask RevenueCat's own "Get Subscriber" endpoint whether an entitlement
is currently active for a user. RevenueCat's subscriber record already accounts for
grace periods, billing retries, and refunds, so this is the single source of truth
to mirror into `entitlements.is_pro` — nothing here re-derives that logic from
individual webhook event types.
"""

import httpx


def fetch_is_pro(app_user_id: str, entitlement_id: str, secret_api_key: str) -> bool:
    response = httpx.get(
        f"https://api.revenuecat.com/v1/subscribers/{app_user_id}",
        headers={"Authorization": f"Bearer {secret_api_key}"},
        timeout=10.0,
    )
    response.raise_for_status()
    entitlements = response.json()["subscriber"]["entitlements"]
    # RevenueCat only includes an entitlement in this dict while it's currently
    # active — no separate expiry-date comparison needed.
    return entitlement_id in entitlements
