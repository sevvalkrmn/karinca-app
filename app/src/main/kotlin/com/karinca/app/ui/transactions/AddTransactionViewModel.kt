package com.karinca.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karinca.app.data.local.CategoryKind
import com.karinca.app.data.local.RecurringDestination
import com.karinca.app.data.local.ReferenceType
import com.karinca.app.data.local.entity.CategoryEntity
import com.karinca.app.data.repository.AuthRepository
import com.karinca.app.data.repository.CategoryRepository
import com.karinca.app.data.repository.PiggyBankRepository
import com.karinca.app.data.repository.ProfileRepository
import com.karinca.app.data.repository.RecurringPaymentRepository
import com.karinca.app.data.repository.TransactionRepository
import com.karinca.app.data.sync.SyncScheduler
import com.karinca.app.ui.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AddTransactionUiState(
    val type: CategoryKind = CategoryKind.EXPENSE,
    val amountText: String = "0",
    val selectedCategoryId: String? = null,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val errorMessage: String? = null,
    // Only ever meaningful for a brand-new expense or income — hidden for edits, and
    // reset whenever the Harcama/Gelir mode switches.
    val isRecurring: Boolean = false,
) {
    val canSave: Boolean
        get() = selectedCategoryId != null && (amountText.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0.0
}

sealed interface AddTransactionEvent {
    data object Saved : AddTransactionEvent
    data object Deleted : AddTransactionEvent
    // This new expense would push the month past the profile's LIMIT reference —
    // covering the overage from the Kumbara instead needs the user's confirmation first.
    data class ConfirmOverspend(val overage: Double) : AddTransactionEvent
    // A brand-new income needs the user to say whether it should count toward the
    // everyday spendable budget, or go straight into the Kumbara instead.
    data class ConfirmIncomeDestination(val amount: Double) : AddTransactionEvent
}

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val profileRepository: ProfileRepository,
    private val piggyBankRepository: PiggyBankRepository,
    private val recurringPaymentRepository: RecurringPaymentRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val userId = authRepository.currentUserIdOrNull()

    // Ranked by how often the user actually picks each one (all-time transaction count),
    // not alphabetically — the "Diğer" catch-all always sinks to the end regardless of its
    // count, and ties (typically every category before any usage exists) fall back to id
    // order, which reads as arbitrary rather than alphabetical by name.
    val categories: StateFlow<List<CategoryEntity>> =
        if (userId == null) {
            flowOf(emptyList())
        } else {
            combine(
                categoryRepository.observeSelectable(userId),
                transactionRepository.observeCategoryUsageCounts(userId),
            ) { categories, usageCounts ->
                val usageById = usageCounts.associate { it.categoryId to it.count }
                categories.sortedWith(
                    compareBy<CategoryEntity> { it.name.startsWith("Diğer") }
                        .thenByDescending { usageById[it.id] ?: 0 }
                        .thenBy { it.id },
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddTransactionEvent>()
    val events: SharedFlow<AddTransactionEvent> = _events.asSharedFlow()

    private var editingId: String? = null
    private var editingOccurredAt: LocalDate = LocalDate.now()
    private var initialized = false
    private var pendingOverspendOverage: Double? = null
    private var pendingIncomeSave: PendingIncomeSave? = null

    private data class PendingIncomeSave(val categoryId: String, val amount: Double)

    // Called once from the screen's LaunchedEffect — Hilt ViewModels can't take
    // per-navigation arguments via constructor injection, so the route's optional args
    // are pushed in explicitly instead (same pattern as OtpVerifyScreen's `phone` param).
    fun initialize(transactionId: String?, presetCategoryId: String?) {
        if (initialized) return
        initialized = true
        when {
            transactionId != null -> {
                editingId = transactionId
                viewModelScope.launch {
                    val existing = transactionRepository.get(transactionId) ?: return@launch
                    editingOccurredAt = existing.occurredAt
                    _uiState.update {
                        it.copy(
                            type = existing.type,
                            amountText = formatForEdit(existing.amount),
                            selectedCategoryId = existing.categoryId,
                            isEditMode = true,
                        )
                    }
                }
            }
            presetCategoryId != null -> _uiState.update { it.copy(selectedCategoryId = presetCategoryId) }
        }
    }

    fun onTypeChange(type: CategoryKind) = _uiState.update {
        it.copy(type = type, selectedCategoryId = null, isRecurring = false)
    }

    fun onSelectCategory(categoryId: String) = _uiState.update { it.copy(selectedCategoryId = categoryId) }

    fun onRecurringToggle(checked: Boolean) = _uiState.update { it.copy(isRecurring = checked) }

    fun onDigit(digit: Char) {
        _uiState.update { state ->
            val current = state.amountText
            val next = when {
                current == "0" && digit != ',' -> digit.toString()
                digit == ',' && current.contains(',') -> current
                else -> current + digit
            }
            state.copy(amountText = next)
        }
    }

    fun onBackspace() {
        _uiState.update { state ->
            state.copy(amountText = if (state.amountText.length > 1) state.amountText.dropLast(1) else "0")
        }
    }

    fun save() {
        val state = uiState.value
        val uid = userId ?: return
        val categoryId = state.selectedCategoryId ?: return
        val amount = state.amountText.replace(',', '.').toDoubleOrNull() ?: return
        if (amount <= 0.0) return

        viewModelScope.launch {
            // Only a brand-new one of either type can trigger these — editing an
            // existing transaction skips both, since it's already accounted for.
            if (editingId == null && state.type == CategoryKind.INCOME) {
                pendingIncomeSave = PendingIncomeSave(categoryId, amount)
                _events.emit(AddTransactionEvent.ConfirmIncomeDestination(amount))
                return@launch
            }
            if (editingId == null && state.type == CategoryKind.EXPENSE) {
                val overage = computeOverage(uid, amount)
                if (overage > 0.0) {
                    pendingOverspendOverage = overage
                    _events.emit(AddTransactionEvent.ConfirmOverspend(overage))
                    return@launch
                }
            }
            performSave(uid, categoryId, amount, state.type)
        }
    }

    // Only called once the user has seen the overspend warning and agreed to it.
    fun confirmSaveDespiteOverspend() {
        val overage = pendingOverspendOverage ?: return
        val state = uiState.value
        val uid = userId ?: return
        val categoryId = state.selectedCategoryId ?: return
        val amount = state.amountText.replace(',', '.').toDoubleOrNull() ?: return
        pendingOverspendOverage = null
        viewModelScope.launch { performSave(uid, categoryId, amount, state.type, overageToWithdraw = overage) }
    }

    fun dismissOverspendWarning() {
        pendingOverspendOverage = null
    }

    // "Mevcut Bütçeye Ekle" — the normal path, an ordinary income transaction.
    fun confirmIncomeToBudget() {
        val pending = pendingIncomeSave ?: return
        val uid = userId ?: return
        pendingIncomeSave = null
        viewModelScope.launch { performSave(uid, pending.categoryId, pending.amount, CategoryKind.INCOME) }
    }

    // "Kumbaraya At" — no transaction is created; the amount only ever exists as a
    // Kumbara deposit, so it never counts toward the everyday spendable budget.
    fun confirmIncomeToKumbara() {
        val pending = pendingIncomeSave ?: return
        val uid = userId ?: return
        pendingIncomeSave = null
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                piggyBankRepository.recordManualDeposit(uid, pending.amount)
                createRecurringIfRequested(
                    userId = uid,
                    amount = pending.amount,
                    categoryId = pending.categoryId,
                    type = CategoryKind.INCOME,
                    destination = RecurringDestination.KUMBARA,
                    dayOfMonth = LocalDate.now().dayOfMonth,
                )
                syncScheduler.requestImmediateSync()
                _events.emit(AddTransactionEvent.Saved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.toUserMessage("Kaydedilemedi, tekrar dener misin?")) }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun dismissIncomeDestinationChoice() {
        pendingIncomeSave = null
    }

    // How much of THIS new expense would land outside the month's LIMIT reference —
    // 0 if the profile isn't LIMIT-based, or if the month (including this amount)
    // still fits under it.
    private suspend fun computeOverage(userId: String, amount: Double): Double {
        val profile = profileRepository.get(userId) ?: return 0.0
        if (profile.referenceType != ReferenceType.LIMIT) return 0.0
        val month = DateRanges.current(ComparisonPeriod.MONTH)
        val before = transactionRepository.observeTotal(userId, CategoryKind.EXPENSE, month.from, month.to).first()
        val after = before + amount
        if (after <= profile.referenceAmount) return 0.0
        return after - maxOf(before, profile.referenceAmount)
    }

    private suspend fun performSave(
        userId: String,
        categoryId: String,
        amount: Double,
        type: CategoryKind,
        overageToWithdraw: Double = 0.0,
    ) {
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        try {
            val id = editingId
            val transactionId: String
            val occurredAt = id?.let { editingOccurredAt } ?: LocalDate.now()
            if (id != null) {
                transactionRepository.update(
                    id = id,
                    userId = userId,
                    categoryId = categoryId,
                    type = type,
                    amount = amount,
                    occurredAt = occurredAt,
                    note = null,
                )
                transactionId = id
            } else {
                transactionId = transactionRepository.create(
                    userId = userId,
                    categoryId = categoryId,
                    type = type,
                    amount = amount,
                    occurredAt = occurredAt,
                    note = null,
                )
            }
            if (overageToWithdraw > 0.0) {
                piggyBankRepository.recordOverspendWithdrawal(userId, overageToWithdraw, transactionId)
            }
            if (id == null) {
                createRecurringIfRequested(
                    userId = userId,
                    amount = amount,
                    categoryId = categoryId,
                    type = type,
                    destination = RecurringDestination.BUDGET,
                    dayOfMonth = occurredAt.dayOfMonth,
                )
            }
            syncScheduler.requestImmediateSync()
            _events.emit(AddTransactionEvent.Saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = e.toUserMessage("Kaydedilemedi, tekrar dener misin?")) }
        } finally {
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    // This manual entry IS this month's one; the AI service's monthly job only starts
    // auto-creating it from next month on, as a transaction (expense / BUDGET income)
    // or a Kumbara deposit (KUMBARA income).
    private suspend fun createRecurringIfRequested(
        userId: String,
        amount: Double,
        categoryId: String,
        type: CategoryKind,
        destination: RecurringDestination,
        dayOfMonth: Int,
    ) {
        if (!uiState.value.isRecurring) return
        // No separate name is asked for — the category already says what it is
        // ("Abonelik", "Maaş/Burs", ...), so that's what Abonelikler lists it as.
        val name = categories.value.firstOrNull { it.id == categoryId }?.name ?: return
        recurringPaymentRepository.create(
            userId = userId,
            name = name,
            amount = amount,
            categoryId = categoryId,
            dayOfMonth = dayOfMonth,
            type = type,
            destination = destination,
        )
    }

    fun delete() {
        val id = editingId ?: return
        val uid = userId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                transactionRepository.softDelete(id, uid)
                piggyBankRepository.reverseOverspendWithdrawalIfAny(uid, id)
                syncScheduler.requestImmediateSync()
                _events.emit(AddTransactionEvent.Deleted)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.toUserMessage("Silinemedi, tekrar dener misin?")) }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun formatForEdit(amount: Double): String {
        val whole = amount.toLong()
        return if (amount == whole.toDouble()) whole.toString() else amount.toString().replace('.', ',')
    }
}
