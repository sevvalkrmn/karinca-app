package com.karinca.app.ui.auth

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.karinca.app.BuildConfig
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

data class GoogleIdTokenResult(val idToken: String, val rawNonce: String)

// Wraps Android's Credential Manager so AuthViewModel (and AuthRepository below it) never
// needs a Context. webClientId comes from BuildConfig.GOOGLE_WEB_CLIENT_ID — until the
// real OAuth Web Client ID is configured (external Supabase/Google Cloud dashboard work),
// this call fails with a clear CredentialManager exception rather than silently no-op-ing.
suspend fun requestGoogleIdToken(context: Context, webClientId: String): GoogleIdTokenResult {
    val rawNonce = UUID.randomUUID().toString()
    val hashedNonce = MessageDigest.getInstance("SHA-256")
        .digest(rawNonce.toByteArray())
        .joinToString("") { "%02x".format(it) }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(webClientId)
        .setNonce(hashedNonce)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    val credential = CredentialManager.create(context).getCredential(context, request).credential

    require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        "Beklenmeyen kimlik bilgisi türü"
    }
    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
    return GoogleIdTokenResult(idToken = googleIdTokenCredential.idToken, rawNonce = rawNonce)
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
                .onFailure { onError(it.message ?: "Google ile giriş başarısız oldu.") }
        }
    }
}
