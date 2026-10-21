@file:OptIn(ExperimentalMaterial3Api::class)

package com.karinca.app.ui.profile

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karinca.app.data.local.CategoryKind
import com.karinca.app.ui.categories.categoryIcon
import com.karinca.app.ui.theme.toComposeColor
import java.text.NumberFormat
import java.util.Locale

private val turkishCurrencyFormat: NumberFormat by lazy {
    NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
}

@Composable
fun SubscriptionsScreen(
    onBack: () -> Unit,
    viewModel: SubscriptionsViewModel = hiltViewModel(),
) {
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    var pendingCancel by remember { mutableStateOf<SubscriptionWithCategory?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Abonelikler", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (subscriptions.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "Tekrar eden bir ödemen yok, ne güzel!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Netflix, Spotify gibi her ay tekrarlanan bir ödemen olursa, Harcama Ekle'de \"Bu her ay tekrar eden bir ödeme\" olarak işaretle — Karınca senin için takip etsin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    items(subscriptions, key = { it.payment.id }) { subscription ->
                        SubscriptionRow(subscription, onCancelClick = { pendingCancel = subscription })
                    }
                }
            }
        }

        pendingCancel?.let { subscription ->
            AlertDialog(
                onDismissRequest = { pendingCancel = null },
                title = { Text("${subscription.payment.name} iptal edilsin mi?") },
                text = { Text("Bu ödeme artık otomatik olarak eklenmeyecek. Geçmiş kayıtlar silinmez.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.cancel(subscription.payment.id)
                        pendingCancel = null
                    }) {
                        Text("İptal Et", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingCancel = null }) { Text("Vazgeç") }
                },
            )
        }
    }
}

@Composable
private fun SubscriptionRow(subscription: SubscriptionWithCategory, onCancelClick: () -> Unit) {
    val category = subscription.category
    val tint = category?.color?.toComposeColor() ?: MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(tint.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (category != null) {
                    Icon(categoryIcon(category.icon), contentDescription = null, tint = tint)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(subscription.payment.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                // Incomes show a leading "+" so they read apart from the payments in the same list.
                val sign = if (subscription.payment.type == CategoryKind.INCOME) "+" else ""
                Text(
                    "Her ayın ${subscription.payment.dayOfMonth}'i · $sign${turkishCurrencyFormat.format(subscription.payment.amount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onCancelClick) {
                Icon(Icons.Filled.Close, contentDescription = "İptal Et", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
