package com.karinca.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karinca.app.data.local.entity.CategoryEntity
import com.karinca.app.data.local.entity.RecurringPaymentEntity
import com.karinca.app.data.repository.AuthRepository
import com.karinca.app.data.repository.CategoryRepository
import com.karinca.app.data.repository.RecurringPaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    categoryRepository: CategoryRepository,
) : ViewModel() {

    private val userId = authRepository.currentUserIdOrNull()

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

    fun cancel(id: String) {
        val uid = userId ?: return
        viewModelScope.launch { recurringPaymentRepository.cancel(id, uid) }
    }
}
