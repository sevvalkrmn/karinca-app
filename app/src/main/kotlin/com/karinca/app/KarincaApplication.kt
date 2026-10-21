package com.karinca.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.karinca.app.data.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KarincaApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    // WorkManager.getInstance() (called inside SyncScheduler) triggers on-demand init
    // from our Configuration.Provider above (the default androidx-startup initializer is
    // disabled in the manifest so SyncWorker, a @HiltWorker, is always constructed via
    // the Hilt-injected factory).
    override fun onCreate() {
        super.onCreate()

        syncScheduler.schedulePeriodic()

        // This is what makes multi-device usage feel current: without it, a change made
        // on one phone only reaches another phone whenever the 15-minute periodic backstop
        // happens to land on both ends. Requesting a sync every time the app is foregrounded
        // (not just on the very first cold start) means switching back to the app is enough
        // to pull in whatever changed elsewhere.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                syncScheduler.requestImmediateSync()
            }
        })
    }
}
