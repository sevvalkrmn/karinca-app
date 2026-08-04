package com.karinca.app.ui.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karinca.app.data.local.entity.WeeklyReportEntity
import com.karinca.app.data.repository.AuthRepository
import com.karinca.app.data.repository.EntitlementRepository
import com.karinca.app.data.repository.WeeklyReportRepository
import com.karinca.app.data.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeeklyReportViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val weeklyReportRepository: WeeklyReportRepository,
    entitlementRepository: EntitlementRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val userId = authRepository.currentUserIdOrNull()

    val latestReport: StateFlow<WeeklyReportEntity?> =
        (userId?.let(weeklyReportRepository::observeLatest) ?: flowOf(null))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // null = not confirmed yet — neither the report nor the paywall should render off
    // of a guessed default, since a stale/leftover Pro=true from a previous account
    // in this same process (or the reverse) would otherwise flash before the real,
    // synced value lands.
    val isPro: StateFlow<Boolean?> =
        (userId?.let(entitlementRepository::observeIsPro) ?: flowOf(false))
            .map<Boolean, Boolean?> { it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Opening the report screen directly (via the coach FAB, without ever seeing the
        // envelope reveal) should still suppress a future envelope pop for this report.
        viewModelScope.launch {
            latestReport.collect { report ->
                report?.let { weeklyReportRepository.markSeen(it.id) }
            }
        }
    }

    // Whatever just happened in the paywall (a completed purchase or just closing
    // it), an immediate sync shrinks the gap between RevenueCat's webhook firing and
    // `isPro` actually flipping true here — `entitlements.is_pro` in Supabase stays
    // the single source of truth, this just stops waiting for the next periodic sync.
    fun onPaywallDismissed() = syncScheduler.requestImmediateSync()
}
