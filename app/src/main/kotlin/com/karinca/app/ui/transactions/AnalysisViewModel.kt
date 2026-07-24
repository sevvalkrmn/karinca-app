package com.karinca.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karinca.app.data.local.CategoryKind
import com.karinca.app.data.local.entity.CategoryEntity
import com.karinca.app.data.repository.AuthRepository
import com.karinca.app.data.repository.CategoryRepository
import com.karinca.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CategoryTotalWithInfo(
    val category: CategoryEntity,
    val total: Double,
    val count: Int,
    val percent: Double,
)

data class AnalysisUiState(
    val period: ComparisonPeriod = ComparisonPeriod.WEEK,
    val total: Double = 0.0,
    // null when there's no prior-period data to compare against (avoids a misleading 0%).
    val trendPercent: Double? = null,
    val categoryTotals: List<CategoryTotalWithInfo> = emptyList(),
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    authRepository: AuthRepository,
    categoryRepository: CategoryRepository,
    transactionRepository: TransactionRepository,
) : ViewModel() {

    private val userId = authRepository.currentUserIdOrNull()
    private val _period = MutableStateFlow(ComparisonPeriod.WEEK)

    val uiState: StateFlow<AnalysisUiState> = _period
        .flatMapLatest { period ->
            val uid = userId
            if (uid == null) {
                flowOf(AnalysisUiState(period = period))
            } else {
                val current = DateRanges.current(period)
                val previous = DateRanges.previous(period)
                combine(
                    categoryRepository.observeSelectable(uid),
                    transactionRepository.observeCategoryTotals(uid, CategoryKind.EXPENSE, current.from, current.to),
                    transactionRepository.observeTotal(uid, CategoryKind.EXPENSE, current.from, current.to),
                    transactionRepository.observeTotal(uid, CategoryKind.EXPENSE, previous.from, previous.to),
                ) { categories, categoryTotals, total, previousTotal ->
                    val categoryById = categories.associateBy { it.id }
                    val breakdown = categoryTotals
                        .mapNotNull { ct ->
                            categoryById[ct.categoryId]?.let { category ->
                                CategoryTotalWithInfo(
                                    category = category,
                                    total = ct.total,
                                    count = ct.count,
                                    percent = if (total > 0) ct.total / total * 100.0 else 0.0,
                                )
                            }
                        }
                        .sortedByDescending { it.total }
                    val trend = if (previousTotal > 0) (total - previousTotal) / previousTotal * 100.0 else null
                    AnalysisUiState(period = period, total = total, trendPercent = trend, categoryTotals = breakdown)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalysisUiState())

    fun onPeriodChange(period: ComparisonPeriod) {
        _period.value = period
    }
}
