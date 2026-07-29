package com.karinca.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karinca.app.data.local.entity.PiggyBankEntryEntity
import com.karinca.app.data.repository.AuthRepository
import com.karinca.app.data.repository.PiggyBankRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class KumbaramUiState(
    val balance: Double = 0.0,
    val entries: List<PiggyBankEntryEntity> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class KumbaramViewModel @Inject constructor(
    authRepository: AuthRepository,
    piggyBankRepository: PiggyBankRepository,
) : ViewModel() {

    private val userId = authRepository.currentUserIdOrNull()

    val uiState: StateFlow<KumbaramUiState> =
        if (userId == null) {
            flowOf(KumbaramUiState(isLoading = false))
        } else {
            combine(
                piggyBankRepository.observeBalance(userId),
                piggyBankRepository.observeEntries(userId),
            ) { balance, entries -> KumbaramUiState(balance = balance, entries = entries, isLoading = false) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KumbaramUiState())
}
