package com.karinca.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karinca.app.ui.components.KarincaBottomBar
import com.karinca.app.ui.components.KarincaTopBar
import java.text.NumberFormat
import java.util.Locale

private val turkishCurrencyFormat: NumberFormat by lazy {
    NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSignedOut: () -> Unit,
    onOpenProfileSetup: () -> Unit,
    onOpenKumbara: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateGoals: () -> Unit,
    onNavigateReports: () -> Unit,
    onAddTransaction: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val isDeleting by viewModel.isDeleting.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.signedOut.collect { onSignedOut() }
    }
    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = { KarincaTopBar() },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            KarincaBottomBar(
                activeTab = KarincaBottomBar.Tab.PROFIL,
                onNavigateHome = onNavigateHome,
                onNavigateGoals = onNavigateGoals,
                onNavigateReports = onNavigateReports,
                onNavigateProfile = {},
                onAddTransaction = onAddTransaction,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(24.dp))

            // Profile header — no photo upload yet, no Pro/subscription badge yet.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                val fullName = uiState.fullName
                if (fullName != null) {
                    Text(fullName, style = MaterialTheme.typography.headlineMedium)
                }
                Text(
                    uiState.email ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(32.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            ) {
                ProfileMenuRow(
                    icon = Icons.Filled.Person,
                    title = "Kişisel Bilgiler",
                    subtitle = "Gelir ve bütçe hedefini ayarla",
                    onClick = onOpenProfileSetup,
                )
                ProfileMenuRow(
                    icon = Icons.Filled.Savings,
                    title = "Kumbaram",
                    // "..." while the real balance is still loading — showing "₺0,00
                    // biriktin" as a placeholder would misreport an existing balance as
                    // empty for the instant before the first Room emission lands.
                    subtitle = if (uiState.isLoading) "..." else "${turkishCurrencyFormat.format(uiState.piggyBankBalance)} biriktin",
                    onClick = onOpenKumbara,
                )
                ProfileMenuRow(
                    icon = Icons.Filled.Autorenew,
                    title = "Abonelikler",
                    subtitle = "Tekrar eden ödemelerini yönet",
                    onClick = onOpenSubscriptions,
                )
                ProfileMenuRow(
                    icon = Icons.Filled.Logout,
                    title = "Çıkış Yap",
                    subtitle = "Oturumu sonlandır",
                    isDestructive = true,
                    onClick = viewModel::signOut,
                )
                ProfileMenuRow(
                    icon = Icons.Filled.DeleteForever,
                    title = "Hesabımı Sil",
                    subtitle = "Hesabını ve tüm verilerini kalıcı olarak sil",
                    isDestructive = true,
                    onClick = { showDeleteConfirm = true },
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Hesabını silmek istediğine emin misin?") },
                text = { Text("Bu işlem geri alınamaz. Tüm gelir/gider kayıtların, hedeflerin ve Kumbara geçmişin kalıcı olarak silinir.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteAccount()
                    }) {
                        Text("Sil", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Vazgeç") }
                },
            )
        }

        // Blocking on purpose: the request can take up to a minute while the server
        // wakes up, and leaving or retrying mid-way would only confuse things.
        if (isDeleting) {
            AlertDialog(
                onDismissRequest = {},
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
                confirmButton = {},
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                        Spacer(Modifier.width(16.dp))
                        Text("Hesabın siliniyor, bu bir dakika kadar sürebilir…")
                    }
                },
            )
        }
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val contentColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val iconContainerColor = if (isDestructive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(iconContainerColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = contentColor)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = contentColor)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDestructive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
