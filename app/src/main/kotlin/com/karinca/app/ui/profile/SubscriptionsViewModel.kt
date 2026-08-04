package com.karinca.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karinca.app.data.local.entity.CategoryEntity
import com.karinca.app.data.local.entity.RecurringPaymentEntity
import com.karinca.app.data.repository.AuthRepository
import com.karinca.app.data.repository.CategoryRepository
import com.karinca.app.data.repository.EntitlementRepository
import com.karinca.app.data.repository.RecurringPaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

data class SubscriptionWithCategory(val payment: RecurringPaymentEntity, val category: CategoryEntity?)

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val recurringPaymentRepository: RecurringPaymentRepository,
    private val entitlementRepository: EntitlementRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    private val userId = authRepository.currentUserIdOrNull()

    val isPro: StateFlow<Boolean> =
        (userId?.let(entitlementRepository::observeIsPro) ?: flowOf(false))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val subscriptions: StateFlow<List<SubscriptionWithCategory>> =
        if (userId == null) {
            flowOf(emptyList())
        } else {
            combine(
                recurringPaymentRepository.observeActive(userId),
                categoryRepository.observeSelectable(userId),
            ) { payments, categories ->
                val categoryById = categories.associateBy { it.id }
                payments.map { SubscriptionWithCategory(it, categoryById[it.categoryId]) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _managementUrl = MutableSharedFlow<String>()
    val managementUrl: SharedFlow<String> = _managementUrl.asSharedFlow()

    fun cancel(id: String) {
        val uid = userId ?: return
        viewModelScope.launch { recurringPaymentRepository.cancel(id, uid) }
    }

    // Play Store subscriptions can't be cancelled/managed through any API — this
    // hands off to Play Store's own management page for it (see
    // EntitlementRepository.getManagementUrl for why).
    fun openProManagement() {
        viewModelScope.launch {
            entitlementRepository.getManagementUrl()?.let { _managementUrl.emit(it) }
        }
    }
}
