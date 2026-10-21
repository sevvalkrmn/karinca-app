package com.karinca.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karinca.app.data.repository.AuthRepository
import com.karinca.app.data.repository.CategoryRepository
import com.karinca.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

// One day's worth of transactions, already in newest-first order.
data class TransactionDayGroup(val date: LocalDate, val transactions: List<TransactionWithCategory>)

data class AllTransactionsUiState(
    val days: List<TransactionDayGroup> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class AllTransactionsViewModel @Inject constructor(
    authRepository: AuthRepository,
    categoryRepository: CategoryRepository,
    transactionRepository: TransactionRepository,
) : ViewModel() {

    val uiState: StateFlow<AllTransactionsUiState> = run {
        val uid = authRepository.currentUserIdOrNull()
        if (uid == null) {
            flowOf(AllTransactionsUiState(isLoading = false))
        } else {
            combine(
                transactionRepository.observeAll(uid),
                categoryRepository.observeSelectable(uid),
            ) { transactions, categories ->
                val byId = categories.associateBy { it.id }
                // The query is already sorted by date, and groupBy keeps first-seen order,
                // so the days come out newest-first without another sort.
                val days = transactions
                    .groupBy { it.occurredAt }
                    .map { (date, dayTransactions) ->
                        TransactionDayGroup(date, dayTransactions.map { TransactionWithCategory(it, byId[it.categoryId]) })
                    }
                AllTransactionsUiState(days = days, isLoading = false)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AllTransactionsUiState())
}
