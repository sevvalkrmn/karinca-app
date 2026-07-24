package com.karinca.app.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karinca.app.data.local.CategoryKind
import com.karinca.app.data.local.entity.GoalEntity
import com.karinca.app.data.repository.AuthRepository
import com.karinca.app.data.repository.GoalRepository
import com.karinca.app.data.repository.TransactionRepository
import com.karinca.app.data.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

// Every goal defaults to the same icon — no icon-picker UI has been designed yet, so
// letting the user choose one per goal isn't possible without inventing that screen.
const val DEFAULT_GOAL_ICON = "savings"

sealed interface GoalsEvent {
    data object Created : GoalsEvent
    // The goal, as proposed, would push spendableBalance (Home's "Harcanabilir Bakiye")
    // below zero — refused rather than letting the user's paced budget go negative.
    data object InsufficientBudget : GoalsEvent
    // Budget is sufficient, but the user should see exactly how much this commits them to
    // per day before it's actually created — confirmAddGoal() does the real write.
    data class ConfirmDailyAmount(
        val dailyAmount: Double,
        val name: String,
        val targetAmount: Double,
        val targetDate: LocalDate,
    ) : GoalsEvent
    // Names of goals evaluateProgress() just deleted because real budget no longer
    // covered what they'd already promised.
    data class GoalsFailed(val names: List<String>) : GoalsEvent
}

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val goalRepository: GoalRepository,
    private val transactionRepository: TransactionRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val userId = authRepository.currentUserIdOrNull()

    // null means "not loaded yet" — distinct from an empty list, which means "loaded, and
    // there really are no goals." Without this distinction the screen would show the empty
    // state's mascot/copy for a beat on every open before Room's first real emission
    // arrives, even when goals already exist.
    val goals: StateFlow<List<GoalEntity>?> =
        (userId?.let(goalRepository::observeActive) ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _events = MutableSharedFlow<GoalsEvent>()
    val events: SharedFlow<GoalsEvent> = _events.asSharedFlow()

    init {
        // Re-runs the same check AppEntryViewModel already did at app open — idempotent if
        // nothing changed since, but this is the one place that can actually show the
        // user why a goal disappeared.
        val uid = userId
        if (uid != null) {
            viewModelScope.launch {
                val failed = goalRepository.evaluateProgress(uid)
                if (failed.isNotEmpty()) {
                    _events.emit(GoalsEvent.GoalsFailed(failed))
                }
            }
        }
    }

    fun addGoal(name: String, targetAmount: Double, targetDate: LocalDate) {
        val uid = userId ?: return
        viewModelScope.launch {
            val today = LocalDate.now()
            val proposed = buildProposedGoal(uid, name, targetAmount, targetDate)
            if (wouldGoNegative(uid, proposed)) {
                _events.emit(GoalsEvent.InsufficientBudget)
                return@launch
            }
            _events.emit(
                GoalsEvent.ConfirmDailyAmount(
                    dailyAmount = proposed.pacedAmount(today),
                    name = name,
                    targetAmount = targetAmount,
                    targetDate = targetDate,
                ),
            )
        }
    }

    // Only called once the user has seen the daily-amount confirmation and agreed to it.
    fun confirmAddGoal(name: String, targetAmount: Double, targetDate: LocalDate) {
        val uid = userId ?: return
        viewModelScope.launch {
            goalRepository.create(uid, name, DEFAULT_GOAL_ICON, targetAmount, initialAmount = 0.0, targetDate)
            syncScheduler.requestImmediateSync()
            _events.emit(GoalsEvent.Created)
        }
    }

    private fun buildProposedGoal(userId: String, name: String, targetAmount: Double, targetDate: LocalDate): GoalEntity {
        val now = Instant.now()
        return GoalEntity(
            id = "",
            userId = userId,
            name = name,
            icon = DEFAULT_GOAL_ICON,
            targetAmount = targetAmount,
            currentAmount = 0.0,
            targetDate = targetDate,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            completedAt = null,
            lastEvaluatedDate = null,
        )
    }

    fun updateGoal(id: String, name: String, targetAmount: Double, currentAmount: Double, targetDate: LocalDate) {
        val uid = userId ?: return
        viewModelScope.launch {
            goalRepository.updateDetails(id, uid, name, targetAmount, currentAmount, targetDate)
            syncScheduler.requestImmediateSync()
        }
    }

    fun completeGoal(id: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            goalRepository.complete(id, uid)
            syncScheduler.requestImmediateSync()
        }
    }

    fun deleteGoal(id: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            goalRepository.softDelete(id, uid)
            syncScheduler.requestImmediateSync()
        }
    }

    // Mirrors HomeViewModel's spendableBalance math (real balance minus every active
    // goal's paced-so-far reservation) but also folds in the goal being proposed, so we
    // can tell — before ever writing it — whether adding it would drop that number below
    // zero.
    private suspend fun wouldGoNegative(userId: String, proposedGoal: GoalEntity): Boolean {
        val today = LocalDate.now()
        val income = transactionRepository.observeAllTimeTotal(userId, CategoryKind.INCOME).first()
        val expense = transactionRepository.observeAllTimeTotal(userId, CategoryKind.EXPENSE).first()
        val balance = income - expense
        val reserved = goals.value.orEmpty().sumOf { it.pacedAmount(today) } + proposedGoal.pacedAmount(today)
        return balance - reserved < 0.0
    }
}
