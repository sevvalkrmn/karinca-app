package com.karinca.app.data.repository

import com.karinca.app.data.local.dao.EntitlementDao
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.awaitCustomerInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Pro status itself is read from the local Room mirror of Supabase's `entitlements`
// table (synced down like everything else, see SyncWorker.pullEntitlements) — the
// same source of truth ai-service's report-gating already reads server-side, not a
// second, separately-fetched RevenueCat SDK state that could drift from it. The SDK
// is only used here to keep RevenueCat's own notion of "who is this" pointed at the
// same id as everything else in the app (Supabase's auth user id).
@Singleton
class EntitlementRepository @Inject constructor(
    private val entitlementDao: EntitlementDao,
) {
    // No row for a user means "not Pro" — mirrors the same convention documented on
    // the entitlements table itself (only ever written once someone actually is Pro).
    fun observeIsPro(userId: String): Flow<Boolean> =
        entitlementDao.observe(userId).map { it?.isPro ?: false }

    fun linkIdentity(userId: String) {
        Purchases.sharedInstance.logIn(userId)
    }

    fun unlinkIdentity() {
        Purchases.sharedInstance.logOut()
    }

    // RevenueCat gives back a store-appropriate URL for managing/cancelling an
    // existing subscription (Play Store's own subscription page on Android) — apps
    // can't cancel a Play subscription themselves via any API, this is the standard
    // way of handing the user off to the platform's own management screen for it.
    suspend fun getManagementUrl(): String? =
        Purchases.sharedInstance.awaitCustomerInfo().managementURL?.toString()
}
