package com.karinca.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karinca.app.data.local.CategoryKind
import com.karinca.app.data.local.entity.GoalEntity
import com.karinca.app.ui.categories.categoryIcon
import com.karinca.app.ui.components.KarincaBottomBar
import com.karinca.app.ui.components.KarincaWordmark
import com.karinca.app.ui.theme.toComposeColor
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val turkishCurrencyFormat: NumberFormat by lazy {
    NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
}
private val dayMonthTimeFormat: DateTimeFormatter by lazy {
    DateTimeFormatter.ofPattern("d MMMM", Locale("tr", "TR"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenProfile: () -> Unit,
    onOpenProfileSetup: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenAnalysis: () -> Unit,
    onOpenCoach: () -> Unit,
    onAddTransaction: () -> Unit,
    onEditTransaction: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { KarincaWordmark() },
                actions = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(
                            Icons.Filled.AccountCircle,
                            contentDescription = "Profilim",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                },
            )
        },
        bottomBar = {
            KarincaBottomBar(
                activeTab = KarincaBottomBar.Tab.GUNLUK,
                onNavigateHome = {},
                onNavigateGoals = onOpenGoals,
                onNavigateReports = onOpenAnalysis,
                onOpenCoach = onOpenCoach,
                onAddTransaction = onAddTransaction,
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Merhaba${uiState.displayName?.let { " $it" } ?: ""},",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        "\"Küçük adımlar, büyük birikimlere dönüşür.\"",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                BalanceCard(
                    balance = uiState.balance,
                    spendableBalance = uiState.spendableBalance,
                    trendPercent = uiState.balanceTrendPercent,
                    monthIncome = uiState.monthIncome,
                    monthExpense = uiState.monthExpense,
                    onClick = onOpenAnalysis,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    DailySummaryCard(uiState.dailyExpense, modifier = Modifier.weight(1f))
                    StatusCard(
                        hasProfile = uiState.profile != null,
                        budgetPercent = uiState.budgetPercent,
                        onClick = onOpenProfileSetup,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item { GoalsSection(uiState.topGoals, onOpenGoals) }
            item { RecentTransactionsSection(uiState.recentTransactions, onEditTransaction) }
            item { Spacer(Modifier.height(4.dp)) }
        }
    }
}

@Composable
private fun BalanceCard(
    balance: Double,
    spendableBalance: Double,
    trendPercent: Double?,
    monthIncome: Double,
    monthExpense: Double,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                "MEVCUT BAKİYE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                turkishCurrencyFormat.format(balance),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            // Only shown once active goals actually reserve something — otherwise it would
            // just repeat the balance above with an extra label.
            if (spendableBalance < balance - 0.01) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Harcanabilir: ${turkishCurrencyFormat.format(spendableBalance)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Gelir ${turkishCurrencyFormat.format(monthIncome)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Gider ${turkishCurrencyFormat.format(monthExpense)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (trendPercent != null) {
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                    ) {
                        Text(
                            "%${trendPercent.roundToInt()} bu ay",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun DailySummaryCard(dailyExpense: Double, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 2.dp,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Spacer(Modifier.height(12.dp))
            Text("Günlük Harcama", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(turkishCurrencyFormat.format(dailyExpense), style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun StatusCard(
    hasProfile: Boolean,
    budgetPercent: Double?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 2.dp,
        // Only the "complete your profile" CTA state actually has somewhere useful to
        // send you — once a budget percentage is showing, the card is a plain readout.
        modifier = if (!hasProfile) modifier.clickable(onClick = onClick) else modifier,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            }
            Spacer(Modifier.height(12.dp))
            when {
                !hasProfile -> {
                    Text("Profilini Tamamla", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Başla", style = MaterialTheme.typography.headlineMedium)
                }
                budgetPercent != null -> {
                    Text("Bütçe Durumu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("%${budgetPercent.roundToInt()}", style = MaterialTheme.typography.headlineMedium)
                }
                else -> {
                    Text("Durum", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("—", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
    }
}

@Composable
private fun GoalsSection(goals: List<GoalEntity>, onOpenGoals: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenGoals),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Yaklaşan Hedefler", style = MaterialTheme.typography.headlineMedium)
                Icon(Icons.Filled.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (goals.isEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Henüz bir hedefin yok, eklemek için dokun.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Spacer(Modifier.height(16.dp))
                goals.forEachIndexed { index, goal ->
                    if (index > 0) Spacer(Modifier.height(16.dp))
                    val savedSoFar = goal.displayedCurrentAmount()
                    val percent = if (goal.targetAmount > 0) (savedSoFar / goal.targetAmount * 100.0) else 0.0
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(goal.name, style = MaterialTheme.typography.labelMedium)
                            Text(
                                "${turkishCurrencyFormat.format(savedSoFar)} / ${turkishCurrencyFormat.format(goal.targetAmount)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (percent / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        )
                    }
                }
                val closest = goals.maxByOrNull { g -> if (g.targetAmount > 0) g.displayedCurrentAmount() / g.targetAmount else 0.0 }
                if (closest != null) {
                    val remaining = (closest.targetAmount - closest.displayedCurrentAmount()).coerceAtLeast(0.0)
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (remaining > 0) {
                                    "\"${closest.name} hedefine sadece ${turkishCurrencyFormat.format(remaining)} kaldı!\""
                                } else {
                                    "\"${closest.name} hedefine ulaştın!\""
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionsSection(transactions: List<TransactionWithCategory>, onEditTransaction: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Son Hareketler", style = MaterialTheme.typography.headlineMedium)
            // "Tümünü Gör" has no destination yet — no chronological all-transactions
            // screen exists (Günlük tab is still undesigned), so it's inert for now.
            TextButton(onClick = {}) { Text("Tümünü Gör") }
        }
        Spacer(Modifier.height(8.dp))
        if (transactions.isEmpty()) {
            Text(
                "Henüz işlem yok.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                transactions.forEach { entry ->
                    TransactionRow(entry, onClick = { onEditTransaction(entry.transaction.id) })
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(entry: TransactionWithCategory, onClick: () -> Unit) {
    val category = entry.category
    val tint = category?.color?.toComposeColor() ?: MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(tint.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (category != null) {
                    Icon(categoryIcon(category.icon), contentDescription = null, tint = tint)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category?.name ?: "İşlem", style = MaterialTheme.typography.labelMedium)
                Text(
                    entry.transaction.occurredAt.format(dayMonthTimeFormat),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val isExpense = entry.transaction.type == CategoryKind.EXPENSE
            Text(
                "${if (isExpense) "-" else "+"}${turkishCurrencyFormat.format(entry.transaction.amount).removePrefix("-")}",
                style = MaterialTheme.typography.labelMedium,
                color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.End,
            )
        }
    }
}
