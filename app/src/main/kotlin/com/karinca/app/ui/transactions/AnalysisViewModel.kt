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

data class CategoryComparison(
    val category: CategoryEntity,
    val currentTotal: Double,
    val previousTotal: Double,
)

data class AnalysisUiState(
    val period: ComparisonPeriod = ComparisonPeriod.WEEK,
    val total: Double = 0.0,
    val previousTotal: Double = 0.0,
    // null when there's no prior-period data to compare against (avoids a misleading 0%).
    val trendPercent: Double? = null,
    val categoryTotals: List<CategoryTotalWithInfo> = emptyList(),
    // Every category with spend in either period, sorted by this period's total —
    // powers the "Bu Hafta vs Geçen Hafta" (or Ay) comparison table.
    val categoryComparisons: List<CategoryComparison> = emptyList(),
    val isLoading: Boolean = true,
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
                flowOf(AnalysisUiState(period = period, isLoading = false))
            } else {
                val current = DateRanges.current(period)
                val previous = DateRanges.previous(period)
                combine(
                    categoryRepository.observeSelectable(uid),
                    transactionRepository.observeCategoryTotals(uid, CategoryKind.EXPENSE, current.from, current.to),
                    transactionRepository.observeCategoryTotals(uid, CategoryKind.EXPENSE, previous.from, previous.to),
                    transactionRepository.observeTotal(uid, CategoryKind.EXPENSE, current.from, current.to),
                    transactionRepository.observeTotal(uid, CategoryKind.EXPENSE, previous.from, previous.to),
                ) { categories, categoryTotals, previousCategoryTotals, total, previousTotal ->
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

                    val currentByCategory = categoryTotals.associateBy { it.categoryId }
                    val previousByCategory = previousCategoryTotals.associateBy { it.categoryId }
                    val comparisons = (currentByCategory.keys + previousByCategory.keys)
                        .distinct()
                        .mapNotNull { categoryId -> categoryById[categoryId] }
                        .map { category ->
                            CategoryComparison(
                                category = category,
                                currentTotal = currentByCategory[category.id]?.total ?: 0.0,
                                previousTotal = previousByCategory[category.id]?.total ?: 0.0,
                            )
                        }
                        .sortedByDescending { it.currentTotal }

                    AnalysisUiState(
                        period = period,
                        total = total,
                        previousTotal = previousTotal,
                        trendPercent = trend,
                        categoryTotals = breakdown,
                        categoryComparisons = comparisons,
                        isLoading = false,
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalysisUiState())

    fun onPeriodChange(period: ComparisonPeriod) {
        _period.value = period
    }
}
