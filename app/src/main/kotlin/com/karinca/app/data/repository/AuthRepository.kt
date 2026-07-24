package com.karinca.app.data.repository

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

// Wraps the Auth plugin's Supabase-shaped session state; owns no UI, no platform
// sign-in SDK calls (Google Credential Manager, SMS retrieval, etc. belong to the
// screen/ViewModel layer once it exists) — this is purely the Supabase Auth boundary.
@Singleton
class AuthRepository @Inject constructor(
    private val auth: Auth,
) {
    // Cold map over the plugin's StateFlow — a future ViewModel turns this into UI
    // state via stateIn(viewModelScope, ...), same as any other repository Flow here.
    val state: Flow<AuthState> = auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> status.session.user?.let { AuthState.SignedIn(it.id) }
                ?: AuthState.Unknown
            is SessionStatus.NotAuthenticated -> AuthState.SignedOut
            is SessionStatus.RefreshFailure -> AuthState.SignedOut
            is SessionStatus.Initializing -> AuthState.Unknown
        }
    }

    fun currentUserIdOrNull(): String? = auth.currentUserOrNull()?.id

    fun currentUserEmailOrNull(): String? = auth.currentUserOrNull()?.email

    // Stored in Supabase's user_metadata at sign-up time (see signUpWithEmail) — not a
    // separate local column, since it's account-identity data, not financial-profile data.
    fun currentUserFullNameOrNull(): String? =
        (auth.currentUserOrNull()?.userMetadata?.get("full_name") as? JsonPrimitive)?.contentOrNull

    // Lets accounts that never captured a name (Google/phone sign-in, or accounts created
    // before this field existed) set/correct it later from Kişisel Bilgiler. updateUser
    // refreshes the SDK's cached current user by default, so currentUserFullNameOrNull()
    // reflects the change immediately after this returns.
    suspend fun updateFullName(fullName: String) {
        auth.updateUser {
            data = buildJsonObject { put("full_name", fullName) }
        }
    }

    // Blocks until the session has been loaded from storage and sessionStatus has
    // settled to Authenticated/NotAuthenticated/RefreshFailure — call once at startup
    // before deciding whether to show sign-in or the main app.
    suspend fun awaitInitialization() = auth.awaitInitialization()

    // The raw Google ID token (and, if used, the un-hashed nonce) is obtained via
    // Android's Credential Manager / Google Identity Services in the sign-in
    // screen/ViewModel; this just forwards it to Supabase.
    suspend fun signInWithGoogleIdToken(idToken: String, rawNonce: String? = null) {
        auth.signInWith(IDToken) {
            this.idToken = idToken
            provider = Google
            nonce = rawNonce
        }
    }

    // Phone OTP is two calls: send the code, then verify it. createUser defaults to
    // true on the OTP provider, so a first-time phone number transparently becomes
    // a new account on verification (sign-up and sign-in are the same flow here).
    suspend fun sendPhoneOtp(phone: String) {
        auth.signInWith(OTP) { this.phone = phone }
    }

    suspend fun verifyPhoneOtp(phone: String, code: String) {
        auth.verifyPhoneOtp(type = OtpType.Phone.SMS, phone = phone, token = code)
    }

    // Secondary per the agreed provider priority (Google + phone OTP primary).
    // Returns true if sign-up established an active session immediately, false if the
    // Supabase project has "Confirm email" enabled — in that case the account exists but
    // there is no session until the user clicks the link emailed to them, and the caller
    // must not treat this as a successful login.
    suspend fun signUpWithEmail(email: String, password: String, fullName: String): Boolean {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
            this.data = buildJsonObject { put("full_name", fullName) }
        }
        return currentUserIdOrNull() != null
    }

    suspend fun signInWithEmail(email: String, password: String) {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }
}
