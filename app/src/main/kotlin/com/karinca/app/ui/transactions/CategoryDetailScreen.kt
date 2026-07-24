package com.karinca.app.ui.transactions

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karinca.app.data.local.CategoryKind
import com.karinca.app.data.local.entity.TransactionEntity
import com.karinca.app.ui.categories.categoryIcon
import com.karinca.app.ui.theme.toComposeColor
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val turkishCurrencyFormat: NumberFormat by lazy {
    NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
}
private val dayMonthFormat: DateTimeFormatter by lazy {
    DateTimeFormatter.ofPattern("d MMMM", Locale("tr", "TR"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    categoryId: String,
    onBack: () -> Unit,
    onEditTransaction: (String) -> Unit,
    onAddTransaction: (String) -> Unit,
    viewModel: CategoryDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(categoryId) { viewModel.initialize(categoryId) }

    val category = uiState.category
    val tint = category?.color?.toComposeColor() ?: MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        category?.name ?: "",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = MaterialTheme.colorScheme.primary)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddTransaction(categoryId) }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Filled.Add, contentDescription = "Harcama Ekle", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item {
                MonthTotalCard(
                    categoryName = category?.name ?: "",
                    icon = category?.icon,
                    tint = tint,
                    total = uiState.monthTotal,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    val trendPercent = uiState.trendPercent
                    StatTile(
                        label = "Geçen Aya Göre",
                        value = trendPercent?.let { "%${if (it >= 0) "+" else ""}${it.roundToInt()}" } ?: "—",
                        valueColor = when {
                            trendPercent == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            trendPercent >= 0 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.tertiary
                        },
                        icon = trendPercent?.let { if (it >= 0) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward },
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Günlük Ortalama",
                        value = turkishCurrencyFormat.format(uiState.dailyAverage),
                        valueColor = MaterialTheme.colorScheme.onSurface,
                        icon = null,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Text("Son İşlemler", style = MaterialTheme.typography.headlineMedium)
            }
            if (uiState.recentTransactions.isEmpty()) {
                item {
                    Text(
                        "Bu kategoride henüz işlem yok.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(uiState.recentTransactions, key = { it.id }) { transaction ->
                TransactionRow(transaction, tint, onClick = { onEditTransaction(transaction.id) })
            }
        }
    }
}

@Composable
private fun MonthTotalCard(categoryName: String, icon: String?, tint: androidx.compose.ui.graphics.Color, total: Double) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).background(tint.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (icon != null) {
                    Icon(categoryIcon(icon), contentDescription = null, tint = tint)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    "BU AY $categoryName",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    turkishCurrencyFormat.format(total),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = valueColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor)
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: TransactionEntity, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.note?.takeIf { it.isNotBlank() } ?: transaction.occurredAt.format(dayMonthFormat),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    transaction.occurredAt.format(dayMonthFormat),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val sign = if (transaction.type == CategoryKind.EXPENSE) "-" else "+"
            Text(
                "$sign${turkishCurrencyFormat.format(transaction.amount).removePrefix("-")}",
                style = MaterialTheme.typography.bodyLarge,
                color = if (transaction.type == CategoryKind.EXPENSE) MaterialTheme.colorScheme.onSurface else tint,
                textAlign = TextAlign.End,
            )
        }
    }
}
