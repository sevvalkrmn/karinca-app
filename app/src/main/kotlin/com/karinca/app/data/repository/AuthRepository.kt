package com.karinca.app.data.repository

import com.karinca.app.data.local.KarincaDatabase
import com.karinca.app.data.sync.SyncCursorStore
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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
    private val database: KarincaDatabase,
    private val cursorStore: SyncCursorStore,
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

    // Stored in Supabase's user_metadata at sign-up time (see signUpWithEmail) as two
    // separate keys, not a single "full_name" — not a separate local column either,
    // since it's account-identity data, not financial-profile data.
    fun currentUserFirstNameOrNull(): String? =
        (auth.currentUserOrNull()?.userMetadata?.get("first_name") as? JsonPrimitive)?.contentOrNull

    fun currentUserLastNameOrNull(): String? =
        (auth.currentUserOrNull()?.userMetadata?.get("last_name") as? JsonPrimitive)?.contentOrNull

    // Display-only convenience — joins the two separately-stored fields. Never write
    // this back as a single value; updateName()/signUpWithEmail() always take the two
    // parts separately.
    fun currentUserFullNameOrNull(): String? =
        listOfNotNull(currentUserFirstNameOrNull(), currentUserLastNameOrNull())
            .joinToString(" ")
            .ifBlank { null }

    // Lets accounts that never captured a name (Google/phone sign-in, or accounts created
    // before this field existed) set/correct it later from Kişisel Bilgiler. updateUser
    // refreshes the SDK's cached current user by default, so currentUserFirstNameOrNull()/
    // currentUserLastNameOrNull() reflect the change immediately after this returns.
    suspend fun updateName(firstName: String, lastName: String) {
        auth.updateUser {
            data = buildJsonObject {
                put("first_name", firstName)
                put("last_name", lastName)
            }
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
    suspend fun signUpWithEmail(email: String, password: String, firstName: String, lastName: String): Boolean {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
            this.data = buildJsonObject {
                put("first_name", firstName)
                put("last_name", lastName)
            }
        }
        return currentUserIdOrNull() != null
    }

    suspend fun signInWithEmail(email: String, password: String) {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    // Local Room is a re-derivable sync cache, never the source of truth — so signing
    // out must leave this device with nothing of the outgoing account still sitting on
    // disk. Without this, the next sign-in here (same account on a second device
    // catching up, or a different account on a previously-used device) would either see
    // the outgoing account's leftover local rows, or — worse — silently skip pulling its
    // own older rows because the sync cursors were still pointing at wherever the
    // outgoing account's last sync left them. Wiping both means every sign-in always
    // starts from a full pull straight from Supabase.
    suspend fun signOut() {
        auth.signOut()
        withContext(Dispatchers.IO) { database.clearAllTables() }
        cursorStore.resetAll()
    }

    // Cold-start-only check: a cached access token can still look "signed in" for up
    // to its own lifetime even after the account behind it was deleted server-side,
    // since token validity is checked locally first — this calls Supabase directly to
    // confirm the account is still actually there. Only an explicit rejection (account
    // gone) signs out; any other failure (offline, slow/unreachable server) leaves the
    // cached session trusted, so a normal offline cold start is never punished for not
    // being able to reach the network.
    suspend fun validateCurrentSessionOrSignOut(): Boolean {
        if (currentUserIdOrNull() == null) return true
        return try {
            auth.retrieveUserForCurrentSession()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: RestException) {
            if (e.statusCode == 401 || e.statusCode == 403 || e.statusCode == 404) {
                signOut()
                false
            } else {
                true
            }
        } catch (e: Exception) {
            true
        }
    }
}
