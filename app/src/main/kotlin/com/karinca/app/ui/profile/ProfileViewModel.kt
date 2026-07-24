package com.karinca.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karinca.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val fullName: String?,
    val email: String?,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    // authRepository.state re-emits whenever the Supabase session changes — including
    // after AuthRepository.updateFullName() — so this re-reads the name instead of
    // freezing it at whatever it was when the ViewModel was first created.
    val uiState: StateFlow<ProfileUiState> = authRepository.state
        .map {
            ProfileUiState(
                fullName = authRepository.currentUserFullNameOrNull(),
                email = authRepository.currentUserEmailOrNull(),
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

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _signedOut.emit(Unit)
        }
    }
}
