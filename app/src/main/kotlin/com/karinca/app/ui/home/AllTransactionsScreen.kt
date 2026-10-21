package com.karinca.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayMonthFormat: DateTimeFormatter by lazy {
    DateTimeFormatter.ofPattern("d MMMM", Locale("tr", "TR"))
}
private val dayMonthYearFormat: DateTimeFormatter by lazy {
    DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("tr", "TR"))
}

// Every transaction, newest first, grouped under a header per day. Reached from Home's
// "Tümünü Gör"; uses Home's own TransactionRow so both lists look identical, and a row
// tap opens the same edit screen Home's recent list does.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTransactionsScreen(
    onBack: () -> Unit,
    onEditTransaction: (String) -> Unit,
    viewModel: AllTransactionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tüm İşlemler",
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
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.days.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                Text(
                    "Henüz işlem yok.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                uiState.days.forEachIndexed { index, day ->
                    // `key`s keep each row's state attached to the right transaction when
                    // a new one is added and everything below shifts down.
                    item(key = day.date.toString()) {
                        Text(
                            dayLabel(day.date),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            // A little extra air above every day except the first, so the
                            // groups read apart from each other.
                            modifier = Modifier.padding(top = if (index == 0) 0.dp else 8.dp),
                        )
                    }
                    items(day.transactions, key = { it.transaction.id }) { entry ->
                        TransactionRow(entry, onClick = { onEditTransaction(entry.transaction.id) })
                    }
                }
            }
        }
    }
}

private fun dayLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when {
        date == today -> "Bugün"
        date == today.minusDays(1) -> "Dün"
        date.year == today.year -> date.format(dayMonthFormat)
        else -> date.format(dayMonthYearFormat)
    }
}
