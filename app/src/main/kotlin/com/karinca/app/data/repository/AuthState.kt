package com.karinca.app.data.repository

// App-owned auth state, deliberately not the raw supabase-kt SessionStatus — insulates
// ViewModels from that SDK's shape so a future provider/library swap doesn't ripple
// past AuthRepository.
sealed interface AuthState {
    // Session not yet loaded from storage / still being resolved at startup.
    data object Unknown : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val userId: String) : AuthState
}
