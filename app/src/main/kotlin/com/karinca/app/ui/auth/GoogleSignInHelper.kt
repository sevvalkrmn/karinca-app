package com.karinca.app.ui.auth

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.karinca.app.BuildConfig
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

data class GoogleIdTokenResult(
    val idToken: String,
    val rawNonce: String,
    val firstName: String?,
    val lastName: String?,
)

// Wraps Android's Credential Manager so AuthViewModel (and AuthRepository below it) never
// needs a Context. webClientId comes from BuildConfig.GOOGLE_WEB_CLIENT_ID — until the
// real OAuth Web Client ID is configured (external Supabase/Google Cloud dashboard work),
// this call fails with a clear CredentialManager exception rather than silently no-op-ing.
suspend fun requestGoogleIdToken(context: Context, webClientId: String): GoogleIdTokenResult {
    val rawNonce = UUID.randomUUID().toString()
    val hashedNonce = MessageDigest.getInstance("SHA-256")
        .digest(rawNonce.toByteArray())
        .joinToString("") { "%02x".format(it) }

    // GetSignInWithGoogleOption, not GetGoogleIdOption — the latter is meant for a passive
    // "One Tap" auto-prompt on app start (and needs setFilterByAuthorizedAccounts to decide
    // silent vs picker behavior); this is an explicit button tap, which is exactly what
    // GetSignInWithGoogleOption is for, and it always shows the full account picker.
    val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
        .setNonce(hashedNonce)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInWithGoogleOption)
        .build()

    val credential = CredentialManager.create(context).getCredential(context, request).credential

    require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        "Beklenmeyen kimlik bilgisi türü"
    }
    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
    return GoogleIdTokenResult(
        idToken = googleIdTokenCredential.idToken,
        rawNonce = rawNonce,
        firstName = googleIdTokenCredential.givenName,
        lastName = googleIdTokenCredential.familyName,
    )
}

// Shared by every screen with a Google button (Welcome, Sign In, Sign Up) so the
// Context/CoroutineScope/BuildConfig wiring exists in exactly one place.
@Composable
fun rememberGoogleSignInAction(
    onResult: (GoogleIdTokenResult) -> Unit,
    onError: (String) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return {
        scope.launch {
            runCatching { requestGoogleIdToken(context, BuildConfig.GOOGLE_WEB_CLIENT_ID) }
                .onSuccess(onResult)
                // Never it.message here — Android's Credential Manager exceptions carry
                // internal-sounding text ("No credentials available", etc.), not
                // anything meant for a user to read. Logged instead, for diagnosis.
                .onFailure {
                    Log.e("GoogleSignIn", "Google sign-in failed", it)
                    onError("Google ile giriş başarısız oldu, tekrar dener misin?")
                }
        }
    }
}
