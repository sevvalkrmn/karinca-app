package com.karinca.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karinca.app.data.local.entity.CategoryEntity
import com.karinca.app.data.local.entity.TransactionEntity
import com.karinca.app.data.repository.AuthRepository
import com.karinca.app.data.repository.CategoryRepository
import com.karinca.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class CategoryDetailUiState(
    val category: CategoryEntity? = null,
    val monthTotal: Double = 0.0,
    // null when there's no prior-month data for this category to compare against.
    val trendPercent: Double? = null,
    val dailyAverage: Double = 0.0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
)

@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val userId = authRepository.currentUserIdOrNull()
    private val categoryIdFlow = MutableStateFlow<String?>(null)

    val uiState = categoryIdFlow
        .filterNotNull()
        .flatMapLatest { categoryId ->
            val uid = userId
            if (uid == null) {
                flowOf(CategoryDetailUiState())
            } else {
                flow {
                    val category = categoryRepository.get(categoryId)
                    if (category == null) {
                        emit(CategoryDetailUiState())
                        return@flow
                    }
                    val current = DateRanges.current(ComparisonPeriod.MONTH)
                    val previous = DateRanges.previous(ComparisonPeriod.MONTH)
                    emitAll(
                        combine(
                            transactionRepository.observeCategoryTotals(uid, category.kind, current.from, current.to),
                            transactionRepository.observeCategoryTotals(uid, category.kind, previous.from, previous.to),
                            transactionRepository.observeRecentByCategory(uid, categoryId, limit = 20),
                        ) { currentTotals, previousTotals, recent ->
                            val total = currentTotals.find { it.categoryId == categoryId }?.total ?: 0.0
                            val previousTotal = previousTotals.find { it.categoryId == categoryId }?.total ?: 0.0
                            val trend = if (previousTotal > 0) (total - previousTotal) / previousTotal * 100.0 else null
                            val daysElapsed = LocalDate.now().dayOfMonth.coerceAtLeast(1)
                            CategoryDetailUiState(
                                category = category,
                                monthTotal = total,
                                trendPercent = trend,
                                dailyAverage = total / daysElapsed,
                                recentTransactions = recent,
                            )
                        }
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoryDetailUiState())

    fun initialize(categoryId: String) {
        if (categoryIdFlow.value != categoryId) {
            categoryIdFlow.value = categoryId
        }
    }
}
