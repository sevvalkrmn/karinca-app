package com.karinca.app.ui.transactions

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karinca.app.ui.categories.categoryIcon
import com.karinca.app.ui.components.KarincaBottomBar
import com.karinca.app.ui.components.KarincaWordmark
import com.karinca.app.ui.theme.toComposeColor
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private val turkishCurrencyFormat: NumberFormat by lazy {
    NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    onCategoryClick: (String) -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateGoals: () -> Unit,
    onNavigateProfile: () -> Unit,
    onOpenCoach: () -> Unit,
    onAddTransaction: () -> Unit,
    viewModel: AnalysisViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { KarincaWordmark() },
                actions = {
                    IconButton(onClick = onNavigateProfile) {
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
                activeTab = KarincaBottomBar.Tab.RAPORLAR,
                onNavigateHome = onNavigateHome,
                onNavigateGoals = onNavigateGoals,
                onNavigateReports = {},
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
                PeriodToggle(selected = uiState.period, onSelect = viewModel::onPeriodChange)
            }
            item {
                TotalSpendCard(total = uiState.total, trendPercent = uiState.trendPercent)
            }
            if (uiState.categoryTotals.isNotEmpty()) {
                item {
                    DonutChartCard(uiState.categoryTotals)
                }
            }
            items(uiState.categoryTotals, key = { it.category.id }) { entry ->
                CategoryRow(entry, onClick = { onCategoryClick(entry.category.id) })
            }
        }
    }
}

@Composable
private fun PeriodToggle(selected: ComparisonPeriod, onSelect: (ComparisonPeriod) -> Unit) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            listOf(ComparisonPeriod.WEEK to "Haftalık", ComparisonPeriod.MONTH to "Aylık").forEach { (period, label) ->
                val active = period == selected
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = if (active) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                    modifier = Modifier.weight(1f).clickable { onSelect(period) },
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalSpendCard(total: Double, trendPercent: Double?) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "TOPLAM HARCAMA",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                turkishCurrencyFormat.format(total),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (trendPercent != null) {
                Spacer(Modifier.height(8.dp))
                val isUp = trendPercent >= 0
                val trendColor = if (isUp) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isUp) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                        contentDescription = null,
                        tint = trendColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "%${kotlin.math.abs(trendPercent).roundToInt()} önceki döneme göre",
                        style = MaterialTheme.typography.labelMedium,
                        color = trendColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun DonutChartCard(entries: List<CategoryTotalWithInfo>) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = size.minDimension * 0.22f
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    var startAngle = -90f
                    entries.forEach { entry ->
                        val sweep = (entry.percent / 100.0 * 360.0).toFloat()
                        drawArc(
                            color = entry.category.color.toComposeColor(),
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2),
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Butt),
                        )
                        startAngle += sweep
                    }
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                entries.take(5).forEach { entry ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(entry.category.color.toComposeColor(), CircleShape),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            entry.category.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(entry: CategoryTotalWithInfo, onClick: () -> Unit) {
    val tint = entry.category.color.toComposeColor()
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(tint.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(categoryIcon(entry.category.icon), contentDescription = null, tint = tint)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.category.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "${entry.count} işlem",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                turkishCurrencyFormat.format(entry.total),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
