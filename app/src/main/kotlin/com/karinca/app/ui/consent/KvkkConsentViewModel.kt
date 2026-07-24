package com.karinca.app.ui.consent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karinca.app.data.repository.AuthRepository
import com.karinca.app.data.repository.ConsentRepository
import com.karinca.app.data.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KvkkConsentUiState(
    val dataStorageChecked: Boolean = false,
    val llmProcessingChecked: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val canContinue: Boolean get() = dataStorageChecked && llmProcessingChecked
}

@HiltViewModel
class KvkkConsentViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val consentRepository: ConsentRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(KvkkConsentUiState())
    val uiState: StateFlow<KvkkConsentUiState> = _uiState.asStateFlow()

    private val _accepted = MutableSharedFlow<Unit>()
    val accepted: SharedFlow<Unit> = _accepted.asSharedFlow()

    fun onDataStorageCheckedChange(checked: Boolean) =
        _uiState.update { it.copy(dataStorageChecked = checked) }

    fun onLlmProcessingCheckedChange(checked: Boolean) =
        _uiState.update { it.copy(llmProcessingChecked = checked) }

    fun accept() {
        val userId = authRepository.currentUserIdOrNull()
        if (userId == null) {
            // Defense in depth: the app shouldn't route here without an active session,
            // but a silently-inert button is a worse failure mode than a visible error.
            _uiState.update { it.copy(errorMessage = "Oturum bulunamadı, tekrar giriş yapmayı dener misin?") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            consentRepository.recordConsent(userId, KvkkPolicy.CURRENT_VERSION)
            syncScheduler.requestImmediateSync()
            _uiState.update { it.copy(isLoading = false) }
            _accepted.emit(Unit)
        }
    }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }
}
