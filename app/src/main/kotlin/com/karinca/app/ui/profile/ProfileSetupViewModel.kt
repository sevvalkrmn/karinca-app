package com.karinca.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karinca.app.data.local.CoachTone
import com.karinca.app.data.local.ReferenceType
import com.karinca.app.data.repository.AuthRepository
import com.karinca.app.data.repository.ProfileRepository
import com.karinca.app.data.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.math.roundToInt
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

data class ProfileSetupUiState(
    val firstNameText: String = "",
    val lastNameText: String = "",
    val email: String = "",
    // Still a plain ReferenceType.LIMIT under the hood (Home's "Bütçe Durumu" card reads
    // profile.referenceAmount regardless of type), but the Limit/Hedef choice and target
    // date are gone from this screen — the Hedeflerim tab is where goals actually live now.
    val budgetLimitText: String = "",
    val regularIncomeText: String = "",
    val coachTone: CoachTone = CoachTone.BALANCED,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    private val income: Double get() = regularIncomeText.replace(',', '.').toDoubleOrNull() ?: 0.0
    private val limit: Double get() = budgetLimitText.replace(',', '.').toDoubleOrNull() ?: 0.0

    // Required, not just a nudge — same 80%-limit rule enforced during onboarding, kept
    // consistent here since Kişisel Bilgiler can also change income/limit later.
    val isSpendRatioTooHigh: Boolean
        get() = income > 0.0 && limit > 0.0 && (limit / income * 100).roundToInt() > 80

    val maxAllowedLimit: Double get() = income * 0.8

    val canSave: Boolean
        get() = !isLoading &&
            firstNameText.isNotBlank() &&
            lastNameText.isNotBlank() &&
            limit > 0.0 &&
            income > 0.0 &&
            !isSpendRatioTooHigh
}

sealed interface ProfileSetupEvent {
    data object Saved : ProfileSetupEvent
}

@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val userId = authRepository.currentUserIdOrNull()

    private val _uiState = MutableStateFlow(ProfileSetupUiState())
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProfileSetupEvent>()
    val events: SharedFlow<ProfileSetupEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val existing = userId?.let { profileRepository.get(it) }
            val firstName = authRepository.currentUserFirstNameOrNull() ?: ""
            val lastName = authRepository.currentUserLastNameOrNull() ?: ""
            val email = authRepository.currentUserEmailOrNull() ?: ""
            _uiState.update {
                if (existing == null) {
                    it.copy(firstNameText = firstName, lastNameText = lastName, email = email, isLoading = false)
                } else {
                    it.copy(
                        firstNameText = firstName,
                        lastNameText = lastName,
                        email = email,
                        budgetLimitText = formatAmount(existing.referenceAmount),
                        regularIncomeText = formatAmount(existing.regularMonthlyIncome),
                        coachTone = existing.coachTone,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun onFirstNameChange(text: String) = _uiState.update { it.copy(firstNameText = text) }
    fun onLastNameChange(text: String) = _uiState.update { it.copy(lastNameText = text) }
    fun onBudgetLimitChange(text: String) = _uiState.update { it.copy(budgetLimitText = text) }
    fun onRegularIncomeChange(text: String) = _uiState.update { it.copy(regularIncomeText = text) }
    fun onCoachToneChange(tone: CoachTone) = _uiState.update { it.copy(coachTone = tone) }

    fun save() {
        val uid = userId ?: return
        val state = uiState.value
        val budgetLimit = state.budgetLimitText.replace(',', '.').toDoubleOrNull() ?: return
        val regularIncome = state.regularIncomeText.replace(',', '.').toDoubleOrNull() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                authRepository.updateName(state.firstNameText.trim(), state.lastNameText.trim())
                profileRepository.save(
                    userId = uid,
                    referenceType = ReferenceType.LIMIT,
                    referenceAmount = budgetLimit,
                    targetDate = null,
                    regularMonthlyIncome = regularIncome,
                    variableMonthlyIncome = null,
                    coachTone = state.coachTone,
                )
                syncScheduler.requestImmediateSync()
                _events.emit(ProfileSetupEvent.Saved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Kaydedilemedi, tekrar dener misin?") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun formatAmount(amount: Double): String {
        val whole = amount.toLong()
        return if (amount == whole.toDouble()) whole.toString() else amount.toString().replace('.', ',')
    }
}
