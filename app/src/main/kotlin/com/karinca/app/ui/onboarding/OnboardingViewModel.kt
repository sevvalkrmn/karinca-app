package com.karinca.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karinca.app.data.local.CoachTone
import com.karinca.app.data.local.ReferenceType
import com.karinca.app.data.repository.AuthRepository
import com.karinca.app.data.repository.ProfileRepository
import com.karinca.app.data.sync.SyncScheduler
import com.karinca.app.ui.components.filterAmountInput
import com.karinca.app.ui.toUserMessage
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

enum class OnboardingStep { INCOME, LIMIT, ALREADY_SPENT, COACH_TONE }

// The coach tone only feeds the AI weekly report, which is switched off for now, so
// both its pickers are hidden (the wizard's COACH_TONE step and ProfileSetupScreen's
// "Koç Tonu" row) and profiles keep the default BALANCED tone. Their UI and state are
// left intact — flip this back to true when the report returns.
const val COACH_TONE_ENABLED = false

val onboardingSteps: List<OnboardingStep> =
    OnboardingStep.entries.filter { COACH_TONE_ENABLED || it != OnboardingStep.COACH_TONE }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.INCOME,
    val incomeText: String = "",
    val limitText: String = "",
    val alreadySpentText: String = "",
    val coachTone: CoachTone = CoachTone.BALANCED,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    val income: Double get() = incomeText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val limit: Double get() = limitText.replace(',', '.').toDoubleOrNull() ?: 0.0
    // Optional — a blank answer just means "starting fresh", identical to today's
    // behavior, so this never blocks continuing.
    val alreadySpent: Double get() = alreadySpentText.replace(',', '.').toDoubleOrNull() ?: 0.0

    // Spending more than the whole month's income doesn't make sense within this
    // model at all — blocks continuing, same rigor as the limit/income ratio check.
    val isAlreadySpentTooHigh: Boolean
        get() = income > 0.0 && alreadySpent > income

    // Already over the limit (but still under income) is a real, valid situation —
    // not blocked — but it means nothing goes to Kumbara this month, worth flagging
    // rather than silently showing a surprising ₺0 later.
    val isAlreadySpentOverLimit: Boolean
        get() = limit > 0.0 && alreadySpent > limit && !isAlreadySpentTooHigh

    // Purely derived from what the user typed — nothing pre-fills limitText, so this
    // is 0 until they enter a spending amount.
    val spendRatioPercent: Int
        get() = if (income > 0.0) (limit / income * 100).roundToInt().coerceIn(0, 100) else 0

    // Required, not just a nudge — "karınca kararınca" is the whole premise of the app,
    // so a limit that leaves less than 20% for saving blocks continuing until it's fixed.
    val isSpendRatioTooHigh: Boolean
        get() = income > 0.0 && limit > 0.0 && spendRatioPercent > 80

    val maxAllowedLimit: Double get() = income * 0.8

    val isLastStep: Boolean get() = step == onboardingSteps.last()

    val canContinue: Boolean
        get() = when (step) {
            OnboardingStep.INCOME -> income > 0.0
            OnboardingStep.LIMIT -> limit > 0.0 && !isSpendRatioTooHigh
            OnboardingStep.ALREADY_SPENT -> !isAlreadySpentTooHigh && !isSaving
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

    fun onIncomeChange(text: String) = _uiState.update { it.copy(incomeText = filterAmountInput(text)) }

    fun onCoachToneChange(tone: CoachTone) = _uiState.update { it.copy(coachTone = tone) }

    fun onLimitTextChange(text: String) = _uiState.update { it.copy(limitText = filterAmountInput(text)) }

    fun onAlreadySpentTextChange(text: String) = _uiState.update { it.copy(alreadySpentText = filterAmountInput(text)) }

    fun onBack() = _uiState.update { state ->
        val previous = when (state.step) {
            OnboardingStep.INCOME -> OnboardingStep.INCOME
            OnboardingStep.LIMIT -> OnboardingStep.INCOME
            OnboardingStep.ALREADY_SPENT -> OnboardingStep.LIMIT
            OnboardingStep.COACH_TONE -> OnboardingStep.ALREADY_SPENT
        }
        state.copy(step = previous)
    }

    fun onContinue() {
        when (uiState.value.step) {
            OnboardingStep.INCOME -> _uiState.update { it.copy(step = OnboardingStep.LIMIT) }
            OnboardingStep.LIMIT -> _uiState.update { it.copy(step = OnboardingStep.ALREADY_SPENT) }
            OnboardingStep.ALREADY_SPENT ->
                if (COACH_TONE_ENABLED) _uiState.update { it.copy(step = OnboardingStep.COACH_TONE) }
                else completeOnboarding()
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
                    initialAlreadySpentAmount = state.alreadySpent.takeIf { it > 0.0 },
                )
                syncScheduler.requestImmediateSync()
                _events.emit(OnboardingEvent.Completed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.toUserMessage("Kaydedilemedi, tekrar dener misin?")) }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
