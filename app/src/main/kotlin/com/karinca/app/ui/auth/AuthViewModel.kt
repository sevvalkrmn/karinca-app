package com.karinca.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karinca.app.data.repository.AuthRepository
import com.karinca.app.data.repository.ConsentRepository
import com.karinca.app.data.repository.ProfileRepository
import com.karinca.app.data.sync.SyncScheduler
import com.karinca.app.ui.consent.KvkkPolicy
import com.karinca.app.ui.toUserMessage
import com.karinca.app.ui.navigation.KarincaRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthMode { EMAIL, PHONE }

data class AuthUiState(
    val mode: AuthMode = AuthMode.EMAIL,
    val firstName: String = "", // sign-up only
    val lastName: String = "", // sign-up only
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "", // sign-up only
    val phone: String = "",
    val otpCode: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    // Live, per-keystroke checks (sign-up only) — shown as a checklist under the
    // password fields instead of making the user submit repeatedly to discover each
    // unmet rule one at a time via an error snackbar.
    val passwordHasMinLength: Boolean get() = password.length >= 8
    val passwordHasUppercase: Boolean get() = password.any { it.isUpperCase() }
    val passwordHasDigit: Boolean get() = password.any { it.isDigit() }
    // Only counts as unmet once the user has actually started the confirm field —
    // an empty confirm field shouldn't read as a mismatch before they've typed anything.
    val passwordsMatch: Boolean get() = confirmPassword.isNotEmpty() && password == confirmPassword

    val isSignUpPasswordValid: Boolean
        get() = passwordHasMinLength && passwordHasUppercase && passwordHasDigit && passwordsMatch
}

// Sign-up-only — sign-in intentionally never re-validates against this, since an
// existing account's password was already accepted under whatever rules were live when it
// was created.
private val signUpEmailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}$")

private fun validateSignUpEmail(email: String): String? =
    if (!signUpEmailRegex.matches(email)) "Geçerli bir e-posta adresi gir (örn. isim@ornek.com)." else null

sealed interface AuthEvent {
    // Carries the resolved post-auth destination (Home or KvkkConsent) — computed here,
    // not assumed from which screen/flow triggered it, because Google sign-in (and phone
    // OTP) can transparently create a brand-new account from either the Sign In or Sign
    // Up screen. Every successful auth path must go through the same consent check.
    data class Authenticated(val destination: KarincaRoute) : AuthEvent
    data class OtpSent(val phone: String) : AuthEvent
    // Account created, but the Supabase project requires email confirmation before a
    // session exists — the user must click the link emailed to them, then sign in.
    data object EmailConfirmationRequired : AuthEvent
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val consentRepository: ConsentRepository,
    private val profileRepository: ProfileRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>()
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun onModeChange(mode: AuthMode) = _uiState.update { it.copy(mode = mode, errorMessage = null) }
    fun onFirstNameChange(value: String) = _uiState.update { it.copy(firstName = value) }
    fun onLastNameChange(value: String) = _uiState.update { it.copy(lastName = value) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value) }
    fun onConfirmPasswordChange(value: String) = _uiState.update { it.copy(confirmPassword = value) }
    fun onPhoneChange(value: String) = _uiState.update { it.copy(phone = value) }
    fun onOtpCodeChange(value: String) = _uiState.update { it.copy(otpCode = value) }
    fun onTogglePasswordVisibility() = _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    fun onToggleConfirmPasswordVisibility() = _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }

    fun signIn() = launchAuthAction {
        when (uiState.value.mode) {
            AuthMode.EMAIL -> {
                authRepository.signInWithEmail(uiState.value.email, uiState.value.password)
                emitAuthenticated()
            }
            AuthMode.PHONE -> {
                val phone = uiState.value.phone
                authRepository.sendPhoneOtp(phone)
                _events.emit(AuthEvent.OtpSent(phone))
            }
        }
    }

    fun signUp() {
        val state = uiState.value
        // Password requirements are already enforced by disabling the submit button
        // (see AuthUiState.isSignUpPasswordValid) — only email format still needs a
        // submit-time check here.
        if (state.mode == AuthMode.EMAIL) {
            val validationError = validateSignUpEmail(state.email)
            if (validationError != null) {
                _uiState.update { it.copy(errorMessage = validationError) }
                return
            }
        }
        launchAuthAction {
            when (uiState.value.mode) {
                AuthMode.EMAIL -> {
                    val sessionEstablished = authRepository.signUpWithEmail(
                        email = uiState.value.email,
                        password = uiState.value.password,
                        firstName = uiState.value.firstName,
                        lastName = uiState.value.lastName,
                    )
                    if (sessionEstablished) {
                        emitAuthenticated()
                    } else {
                        _events.emit(AuthEvent.EmailConfirmationRequired)
                    }
                }
                AuthMode.PHONE -> {
                    val phone = uiState.value.phone
                    authRepository.sendPhoneOtp(phone)
                    _events.emit(AuthEvent.OtpSent(phone))
                }
            }
        }
    }

    fun verifyOtp(phone: String) = launchAuthAction {
        authRepository.verifyPhoneOtp(phone, uiState.value.otpCode)
        emitAuthenticated()
    }

    // Takes phone explicitly (not from uiState) since OtpVerifyScreen gets its own fresh
    // AuthViewModel instance, scoped separately from whichever screen sent the first code.
    fun resendOtp(phone: String) = launchAuthAction {
        authRepository.sendPhoneOtp(phone)
        _events.emit(AuthEvent.OtpSent(phone))
    }

    fun signInWithGoogleIdToken(idToken: String, rawNonce: String?) = launchAuthAction {
        authRepository.signInWithGoogleIdToken(idToken, rawNonce)
        emitAuthenticated()
    }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    // For errors surfaced outside launchAuthAction's try/catch — e.g. Credential Manager
    // failures from rememberGoogleSignInAction, which run in their own coroutine scope.
    fun showError(message: String) = _uiState.update { it.copy(errorMessage = message) }

    private suspend fun emitAuthenticated() {
        // A fresh session has no local data yet (categories, profile, consent from other
        // devices, etc. all live server-side) — don't wait on the 15-minute periodic
        // backstop for it to show up.
        syncScheduler.requestImmediateSync()
        val userId = authRepository.currentUserIdOrNull()
        val destination = if (userId == null || !consentRepository.hasConsented(userId, KvkkPolicy.CURRENT_VERSION)) {
            KarincaRoute.KvkkConsent
        } else if (!profileRepository.hasProfileSetup(userId)) {
            // Consented before (e.g. quit mid-wizard last time) but never finished
            // onboarding — send them back to it instead of Home.
            KarincaRoute.Onboarding
        } else {
            KarincaRoute.Home
        }
        _events.emit(AuthEvent.Authenticated(destination))
    }

    private fun launchAuthAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.toUserMessage()) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
