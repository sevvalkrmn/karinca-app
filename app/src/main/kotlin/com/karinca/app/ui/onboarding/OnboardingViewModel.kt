package com.karinca.app.ui.onboarding

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

enum class OnboardingStep { INCOME, LIMIT, COACH_TONE }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.INCOME,
    val incomeText: String = "",
    val limitText: String = "",
    val coachTone: CoachTone = CoachTone.BALANCED,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    val income: Double get() = incomeText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val limit: Double get() = limitText.replace(',', '.').toDoubleOrNull() ?: 0.0

    // Purely derived from what the user typed — nothing pre-fills limitText, so this
    // is 0 until they enter a spending amount.
    val spendRatioPercent: Int
        get() = if (income > 0.0) (limit / income * 100).roundToInt().coerceIn(0, 100) else 0

    // Required, not just a nudge — "karınca kararınca" is the whole premise of the app,
    // so a limit that leaves less than 20% for saving blocks continuing until it's fixed.
    val isSpendRatioTooHigh: Boolean
        get() = income > 0.0 && limit > 0.0 && spendRatioPercent > 80

    val maxAllowedLimit: Double get() = income * 0.8

    val canContinue: Boolean
        get() = when (step) {
            OnboardingStep.INCOME -> income > 0.0
            OnboardingStep.LIMIT -> limit > 0.0 && !isSpendRatioTooHigh
            OnboardingStep.COACH_TONE -> !isSaving
        }
}

sealed interface OnboardingEvent {
    data object Completed : OnboardingEvent
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val userId = authRepository.currentUserIdOrNull()

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OnboardingEvent>()
    val events: SharedFlow<OnboardingEvent> = _events.asSharedFlow()

    fun onIncomeChange(text: String) = _uiState.update { it.copy(incomeText = text) }

    fun onCoachToneChange(tone: CoachTone) = _uiState.update { it.copy(coachTone = tone) }

    fun onLimitTextChange(text: String) = _uiState.update { it.copy(limitText = text) }

    fun onBack() = _uiState.update { state ->
        val previous = when (state.step) {
            OnboardingStep.INCOME -> OnboardingStep.INCOME
            OnboardingStep.LIMIT -> OnboardingStep.INCOME
            OnboardingStep.COACH_TONE -> OnboardingStep.LIMIT
        }
        state.copy(step = previous)
    }

    fun onContinue() {
        when (uiState.value.step) {
            OnboardingStep.INCOME -> _uiState.update { it.copy(step = OnboardingStep.LIMIT) }
            OnboardingStep.LIMIT -> _uiState.update { it.copy(step = OnboardingStep.COACH_TONE) }
            OnboardingStep.COACH_TONE -> completeOnboarding()
        }
    }

    private fun completeOnboarding() {
        val uid = userId ?: return
        val state = uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                profileRepository.save(
                    userId = uid,
                    referenceType = ReferenceType.LIMIT,
                    referenceAmount = state.limit,
                    targetDate = null,
                    regularMonthlyIncome = state.income,
                    variableMonthlyIncome = null,
                    coachTone = state.coachTone,
                )
                syncScheduler.requestImmediateSync()
                _events.emit(OnboardingEvent.Completed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Kaydedilemedi, tekrar dener misin?") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
