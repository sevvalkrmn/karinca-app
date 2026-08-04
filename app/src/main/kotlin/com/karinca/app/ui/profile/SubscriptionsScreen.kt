@file:OptIn(ExperimentalMaterial3Api::class)

package com.karinca.app.ui.profile

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karinca.app.R
import com.karinca.app.ui.categories.categoryIcon
import com.karinca.app.ui.components.MascotWaveVideo
import com.karinca.app.ui.theme.toComposeColor
import java.text.NumberFormat
import java.util.Locale

private val turkishCurrencyFormat: NumberFormat by lazy {
    NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
}

@Composable
fun SubscriptionsScreen(
    onBack: () -> Unit,
    onOpenCoach: () -> Unit,
    viewModel: SubscriptionsViewModel = hiltViewModel(),
) {
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    var pendingCancel by remember { mutableStateOf<SubscriptionWithCategory?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.managementUrl.collect { url ->
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

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
            ProStatusCard(
                isPro = isPro,
                onManageClick = viewModel::openProManagement,
                onUpgradeClick = onOpenCoach,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )

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
private fun ProStatusCard(
    isPro: Boolean,
    onManageClick: () -> Unit,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isPro) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth().clickable(onClick = if (isPro) onManageClick else onUpgradeClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(64.dp),
            ) {
                MascotWaveVideo(rawResId = R.raw.karinca_wave)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isPro) "Karınca Pro Üyesisin" else "Karınca Pro'ya henüz üye değilsin",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    if (isPro) "Aboneliğini yönetmek veya iptal etmek için dokun" else "Haftalık AI koçunu açmak için Pro'ya geç",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(
                    "Her ayın ${subscription.payment.dayOfMonth}'i · ${turkishCurrencyFormat.format(subscription.payment.amount)}",
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
