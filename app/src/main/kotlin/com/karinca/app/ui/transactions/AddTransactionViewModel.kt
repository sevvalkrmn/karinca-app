package com.karinca.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karinca.app.data.local.CategoryKind
import com.karinca.app.data.local.entity.CategoryEntity
import com.karinca.app.data.repository.AuthRepository
import com.karinca.app.data.repository.CategoryRepository
import com.karinca.app.data.repository.TransactionRepository
import com.karinca.app.data.sync.SyncScheduler
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
) {
    val canSave: Boolean
        get() = selectedCategoryId != null && (amountText.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0.0
}

sealed interface AddTransactionEvent {
    data object Saved : AddTransactionEvent
    data object Deleted : AddTransactionEvent
}

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
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

    fun onTypeChange(type: CategoryKind) = _uiState.update { it.copy(type = type, selectedCategoryId = null) }

    fun onSelectCategory(categoryId: String) = _uiState.update { it.copy(selectedCategoryId = categoryId) }

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
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val id = editingId
                if (id != null) {
                    transactionRepository.update(
                        id = id,
                        userId = uid,
                        categoryId = categoryId,
                        type = state.type,
                        amount = amount,
                        occurredAt = editingOccurredAt,
                        note = null,
                    )
                } else {
                    transactionRepository.create(
                        userId = uid,
                        categoryId = categoryId,
                        type = state.type,
                        amount = amount,
                        occurredAt = LocalDate.now(),
                        note = null,
                    )
                }
                syncScheduler.requestImmediateSync()
                _events.emit(AddTransactionEvent.Saved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Kaydedilemedi, tekrar dener misin?") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun delete() {
        val id = editingId ?: return
        val uid = userId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                transactionRepository.softDelete(id, uid)
                syncScheduler.requestImmediateSync()
                _events.emit(AddTransactionEvent.Deleted)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Silinemedi, tekrar dener misin?") }
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
