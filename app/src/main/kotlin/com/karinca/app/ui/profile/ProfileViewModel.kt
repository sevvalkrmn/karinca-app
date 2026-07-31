package com.karinca.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karinca.app.data.repository.AuthRepository
import com.karinca.app.data.repository.PiggyBankRepository
import com.karinca.app.ui.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val fullName: String?,
    val email: String?,
    val piggyBankBalance: Double = 0.0,
    val isLoading: Boolean = true,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    piggyBankRepository: PiggyBankRepository,
) : ViewModel() {

    // authRepository.state re-emits whenever the Supabase session changes — including
    // after AuthRepository.updateName() — so this re-reads the name instead of
    // freezing it at whatever it was when the ViewModel was first created.
    val uiState: StateFlow<ProfileUiState> = combine(
        authRepository.state,
        authRepository.currentUserIdOrNull()?.let(piggyBankRepository::observeBalance) ?: flowOf(0.0),
    ) { _, balance ->
        ProfileUiState(
            fullName = authRepository.currentUserFullNameOrNull(),
            email = authRepository.currentUserEmailOrNull(),
            piggyBankBalance = balance,
            isLoading = false,
        )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            ProfileUiState(
                fullName = authRepository.currentUserFullNameOrNull(),
                email = authRepository.currentUserEmailOrNull(),
            ),
        )

    private val _signedOut = MutableSharedFlow<Unit>()
    val signedOut: SharedFlow<Unit> = _signedOut.asSharedFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _signedOut.emit(Unit)
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            try {
                authRepository.deleteAccount()
                _signedOut.emit(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.emit(e.toUserMessage("Hesap silinemedi, tekrar dener misin?"))
            }
        }
    }
}
