package com.karinca.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karinca.app.data.repository.AuthRepository
import com.karinca.app.data.repository.ConsentRepository
import com.karinca.app.data.repository.EntitlementRepository
import com.karinca.app.data.repository.GoalRepository
import com.karinca.app.data.repository.ProfileRepository
import com.karinca.app.data.sync.SyncScheduler
import com.karinca.app.ui.consent.KvkkPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface StartDestination {
    data object Loading : StartDestination
    data class Route(val route: KarincaRoute) : StartDestination
}

// Resolved once at process start so the NavHost doesn't flash Welcome for an
// already-signed-in, already-consented user.
@HiltViewModel
class AppEntryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val consentRepository: ConsentRepository,
    private val syncScheduler: SyncScheduler,
    private val goalRepository: GoalRepository,
    private val profileRepository: ProfileRepository,
    private val entitlementRepository: EntitlementRepository,
) : ViewModel() {

    private val _startDestination = MutableStateFlow<StartDestination>(StartDestination.Loading)
    val startDestination: StateFlow<StartDestination> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.awaitInitialization()
            val userId = authRepository.currentUserIdOrNull()
            val route = if (userId == null) {
                KarincaRoute.Welcome
            } else if (!authRepository.validateCurrentSessionOrSignOut()) {
                // The cached session looked signed-in, but Supabase just rejected it
                // outright (e.g. the account was deleted server-side) — signOut() has
                // already wiped local data, so this is a real, fresh sign-out.
                KarincaRoute.Welcome
            } else {
                entitlementRepository.linkIdentity(userId)
                syncScheduler.requestImmediateSync()
                // Silent here — GoalsViewModel runs the same check again (a no-op if
                // nothing's changed) and is the one that actually shows a failure dialog.
                // This just keeps Home's goal previews from going stale for someone who
                // never opens Hedeflerim.
                viewModelScope.launch { goalRepository.evaluateProgress(userId) }
                val hasConsented = consentRepository.hasConsented(userId, KvkkPolicy.CURRENT_VERSION)
                if (!hasConsented) {
                    KarincaRoute.KvkkConsent
                } else if (!profileRepository.hasProfileSetup(userId)) {
                    // Consented but never finished onboarding (quit mid-wizard) — send
                    // them back to it instead of Home, where it'd only be a skippable nudge.
                    KarincaRoute.Onboarding
                } else {
                    KarincaRoute.Home
                }
            }
            _startDestination.value = StartDestination.Route(route)
        }
    }
}
