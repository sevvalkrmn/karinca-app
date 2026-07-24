package com.karinca.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.karinca.app.R

/**
 * Shared bottom navigation. Four real tabs (Günlük -> Home, Hedefler -> Goals,
 * Raporlar -> Analysis, Analiz -> Karınca Koçu weekly report) plus a center FAB for adding
 * a transaction. Profile is no longer a bottom-bar tab — it's reached only through the
 * profile icon in each screen's own top bar. There is no "Genel Bakış" tab either —
 * Categories is reached via the "Hepsini Gör" link on the add-transaction category grid
 * instead, so [Tab] has no case for it.
 */
object KarincaBottomBar {
    // NONE is for screens reached outside the four primary tabs (e.g. Categories, Profile)
    // — no item highlights in that case.
    enum class Tab { GUNLUK, HEDEFLER, RAPORLAR, KOC, NONE }
}

@Composable
fun KarincaBottomBar(
    activeTab: KarincaBottomBar.Tab,
    onNavigateHome: () -> Unit,
    onNavigateGoals: () -> Unit,
    onNavigateReports: () -> Unit,
    onOpenCoach: () -> Unit,
    onAddTransaction: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                BottomNavItem(
                    label = "Günlük",
                    active = activeTab == KarincaBottomBar.Tab.GUNLUK,
                    onClick = onNavigateHome,
                    icon = { tint -> Icon(Icons.Filled.Savings, contentDescription = null, tint = tint) },
                )
                BottomNavItem(
                    label = "Hedefler",
                    active = activeTab == KarincaBottomBar.Tab.HEDEFLER,
                    onClick = onNavigateGoals,
                    icon = { tint -> Icon(Icons.Filled.Flag, contentDescription = null, tint = tint) },
                )
                // Reserves the center gap the floating action button sits above.
                Box(modifier = Modifier.padding(horizontal = 24.dp))
                BottomNavItem(
                    label = "Raporlar",
                    active = activeTab == KarincaBottomBar.Tab.RAPORLAR,
                    onClick = onNavigateReports,
                    icon = { tint -> Icon(Icons.Filled.BarChart, contentDescription = null, tint = tint) },
                )
                BottomNavItem(
                    label = "Analiz",
                    active = activeTab == KarincaBottomBar.Tab.KOC,
                    onClick = onOpenCoach,
                    icon = { tint ->
                        Icon(
                            painterResource(R.drawable.karinca_maskot),
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                )
            }
        }
        FloatingActionButton(
            onClick = onAddTransaction,
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.TopCenter).offset(y = (-28).dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Harcama Ekle", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    icon: @Composable (tint: Color) -> Unit,
) {
    val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        icon(tint)
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}
