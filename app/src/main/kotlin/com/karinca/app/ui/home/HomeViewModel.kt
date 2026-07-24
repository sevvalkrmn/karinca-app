package com.karinca.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karinca.app.data.local.CategoryKind
import com.karinca.app.data.local.entity.CategoryEntity
import com.karinca.app.data.local.entity.GoalEntity
import com.karinca.app.data.local.entity.ProfileEntity
import com.karinca.app.data.local.entity.TransactionEntity
import com.karinca.app.data.repository.AuthRepository
import com.karinca.app.data.repository.CategoryRepository
import com.karinca.app.data.repository.GoalRepository
import com.karinca.app.data.repository.ProfileRepository
import com.karinca.app.data.repository.TransactionRepository
import com.karinca.app.ui.transactions.ComparisonPeriod
import com.karinca.app.ui.transactions.DateRanges
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class TransactionWithCategory(val transaction: TransactionEntity, val category: CategoryEntity?)

private data class BalanceInfo(
    val balance: Double,
    val monthNet: Double,
    val monthIncome: Double,
    val monthExpense: Double,
    val dailyExpense: Double,
)

data class HomeUiState(
    val displayName: String? = null,
    val balance: Double = 0.0,
    // Real balance minus what active goals should have accumulated by today at a steady
    // daily pace toward their target date — a nudge to spend less, never a real deduction
    // (no transaction is created for it, and it never touches `balance` itself).
    val spendableBalance: Double = 0.0,
    val balanceTrendPercent: Double? = null,
    // This month's real income/expense so far — shown on the balance card from day one,
    // unlike balanceTrendPercent which needs a full previous month to mean anything.
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val dailyExpense: Double = 0.0,
    val profile: ProfileEntity? = null,
    // % of this month's expenses against the profile's single reference amount, regardless
    // of whether the user labeled that reference a "Limit" or a "Goal" — the home card
    // always reads as a plain budget-usage percentage, with no branching on referenceType.
    val budgetPercent: Double? = null,
    val topGoals: List<GoalEntity> = emptyList(),
    val recentTransactions: List<TransactionWithCategory> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    profileRepository: ProfileRepository,
    categoryRepository: CategoryRepository,
    transactionRepository: TransactionRepository,
    goalRepository: GoalRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = run {
        val uid = authRepository.currentUserIdOrNull()
        if (uid == null) {
            flowOf(HomeUiState(isLoading = false))
        } else {
            val today = LocalDate.now()
            val thisMonth = DateRanges.current(ComparisonPeriod.MONTH)
            val lastMonth = DateRanges.previous(ComparisonPeriod.MONTH)

            val balanceInfo = combine(
                transactionRepository.observeAllTimeTotal(uid, CategoryKind.INCOME),
                transactionRepository.observeAllTimeTotal(uid, CategoryKind.EXPENSE),
                transactionRepository.observeTotal(uid, CategoryKind.INCOME, thisMonth.from, thisMonth.to),
                transactionRepository.observeTotal(uid, CategoryKind.EXPENSE, thisMonth.from, thisMonth.to),
                transactionRepository.observeTotal(uid, CategoryKind.EXPENSE, today, today),
            ) { allIncome, allExpense, monthIncome, monthExpense, dailyExpense ->
                BalanceInfo(
                    balance = allIncome - allExpense,
                    monthNet = monthIncome - monthExpense,
                    monthIncome = monthIncome,
                    monthExpense = monthExpense,
                    dailyExpense = dailyExpense,
                )
            }

            val previousMonthNet = combine(
                transactionRepository.observeTotal(uid, CategoryKind.INCOME, lastMonth.from, lastMonth.to),
                transactionRepository.observeTotal(uid, CategoryKind.EXPENSE, lastMonth.from, lastMonth.to),
            ) { income, expense -> income - expense }

            val recentWithCategory = combine(
                transactionRepository.observeRecentAll(uid, limit = 5),
                categoryRepository.observeSelectable(uid),
            ) { transactions, categories ->
                val byId = categories.associateBy { it.id }
                transactions.map { TransactionWithCategory(it, byId[it.categoryId]) }
            }

            val data = combine(
                balanceInfo,
                previousMonthNet,
                profileRepository.observe(uid),
                goalRepository.observeActive(uid),
                recentWithCategory,
            ) { balance, prevNet, profile, goals, recent ->
                val trend = if (prevNet > 0) (balance.monthNet - prevNet) / prevNet * 100.0 else null
                val topGoals = goals.sortedBy { it.targetDate }.take(2)
                val reservedForGoals = goals.sumOf { it.pacedAmount(today) }
                HomeUiState(
                    balance = balance.balance,
                    spendableBalance = balance.balance - reservedForGoals,
                    balanceTrendPercent = trend,
                    monthIncome = balance.monthIncome,
                    monthExpense = balance.monthExpense,
                    dailyExpense = balance.dailyExpense,
                    profile = profile,
                    budgetPercent = if (profile != null && profile.referenceAmount > 0) {
                        balance.monthExpense / profile.referenceAmount * 100.0
                    } else null,
                    topGoals = topGoals,
                    recentTransactions = recent,
                    isLoading = false,
                )
            }

            // authRepository.state re-emits whenever the Supabase session changes (e.g.
            // after AuthRepository.updateFullName()), which is what makes displayName
            // re-read instead of freezing at whatever it was when this Flow first combined.
            combine(data, authRepository.state) { state, _ ->
                state.copy(
                    displayName = authRepository.currentUserFullNameOrNull()?.trim()?.substringBefore(" ")?.takeIf { it.isNotBlank() },
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
}
