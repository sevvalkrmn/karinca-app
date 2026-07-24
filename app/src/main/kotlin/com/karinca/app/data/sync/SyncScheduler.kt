package com.karinca.app.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // 15 minutes is WorkManager's floor for periodic work — this is the sync backstop,
    // not the primary trigger. Call requestImmediateSync() right after sign-in/sign-up
    // so a fresh session doesn't sit with empty local data for up to 15 minutes.
    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(networkConstraints())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    // One-off pull — used right after authentication (a brand-new or returning session
    // has no local data yet: categories, profile, etc. all live server-side until this
    // runs) and at cold start when a session is already active. REPLACE, not KEEP: each
    // call is a meaningful new event (sign-in, app restart), not a rapid duplicate — if a
    // prior attempt is stuck retrying (e.g. hit an error), this should still force a fresh
    // attempt rather than silently leaving the stuck one in place.
    fun requestImmediateSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraints())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun networkConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private companion object {
        const val PERIODIC_WORK_NAME = "karinca_sync"
        const val IMMEDIATE_WORK_NAME = "karinca_sync_now"
    }
}
