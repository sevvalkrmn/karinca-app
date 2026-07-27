@file:OptIn(ExperimentalMaterial3Api::class)

package com.karinca.app.ui.transactions

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karinca.app.R
import com.karinca.app.data.local.CategoryKind
import com.karinca.app.data.local.entity.CategoryEntity
import com.karinca.app.ui.categories.categoryIcon
import com.karinca.app.ui.components.KarincaWordmark
import com.karinca.app.ui.components.LabeledIconField
import com.karinca.app.ui.components.MascotWaveVideo
import com.karinca.app.ui.components.PrimaryButton
import java.text.NumberFormat
import java.util.Locale

private val PillShape = RoundedCornerShape(percent = 50)

private val turkishCurrencyFormat: NumberFormat by lazy {
    NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
}

@Composable
fun AddTransactionScreen(
    transactionId: String?,
    presetCategoryId: String?,
    onSaved: () -> Unit,
    onClose: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingOverage by remember { mutableStateOf<Double?>(null) }
    var pendingIncomeAmount by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(transactionId, presetCategoryId) {
        viewModel.initialize(transactionId, presetCategoryId)
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AddTransactionEvent.Saved, AddTransactionEvent.Deleted -> onSaved()
                is AddTransactionEvent.ConfirmOverspend -> pendingOverage = event.overage
                is AddTransactionEvent.ConfirmIncomeDestination -> pendingIncomeAmount = event.amount
            }
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    val visibleCategories = categories.filter { it.kind == uiState.type }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { KarincaWordmark() },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Kapat")
                    }
                },
                actions = {
                    if (uiState.isEditMode) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(16.dp))

            // Harcama / Gelir mode switch.
            Surface(
                shape = PillShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    ModeSwitchButton(
                        text = "Harcama",
                        selected = uiState.type == CategoryKind.EXPENSE,
                        selectedColor = MaterialTheme.colorScheme.primary,
                        onClick = { viewModel.onTypeChange(CategoryKind.EXPENSE) },
                        modifier = Modifier.weight(1f),
                    )
                    ModeSwitchButton(
                        text = "Gelir",
                        selected = uiState.type == CategoryKind.INCOME,
                        selectedColor = MaterialTheme.colorScheme.secondary,
                        onClick = { viewModel.onTypeChange(CategoryKind.INCOME) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // Amount display + mascot tip.
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    ) {
                        Text(
                            "MİKTAR GİRİN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                uiState.amountText,
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                " ₺",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                    ) {
                        Image(painterResource(R.drawable.ic_karinca_logo), contentDescription = null, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (uiState.type == CategoryKind.INCOME) {
                                "\"Bugün ne kazandın? Her birikim seni hedeflerine biraz daha yaklaştırır!\""
                            } else {
                                "\"Bugün ne için harcama yaptık? Küçük adımlar büyük birikimlere dönüşür!\""
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Category selection — a single horizontally scrolling row so every category is
            // reachable without a separate "see all" sheet, and so the space a multi-row grid
            // used to reserve goes back to the numpad below instead.
            Text("Kategori", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(visibleCategories, key = { it.id }) { category ->
                    CategoryChip(
                        category = category,
                        selected = category.id == uiState.selectedCategoryId,
                        onClick = { viewModel.onSelectCategory(category.id) },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            // Only a brand-new expense can be marked recurring — editing an existing
            // one, or an income, never shows this.
            if (!uiState.isEditMode && uiState.type == CategoryKind.EXPENSE) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.onRecurringToggle(!uiState.isRecurring) },
                ) {
                    Checkbox(checked = uiState.isRecurring, onCheckedChange = viewModel::onRecurringToggle)
                    Text(
                        "Bu her ay tekrar eden bir ödeme",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (uiState.isRecurring) {
                    Spacer(Modifier.height(8.dp))
                    LabeledIconField(
                        label = "Ödeme Adı",
                        value = uiState.recurringName,
                        onValueChange = viewModel::onRecurringNameChange,
                        leadingIcon = Icons.Filled.Autorenew,
                        placeholder = "Netflix, kira, vb.",
                    )
                }
                Spacer(Modifier.height(4.dp))
            }

            NumericKeypad(onDigit = viewModel::onDigit, onBackspace = viewModel::onBackspace)
            Spacer(Modifier.height(16.dp))

            PrimaryButton(
                text = if (uiState.isEditMode) "Güncelle" else "Kaydet",
                onClick = viewModel::save,
                enabled = uiState.canSave,
                loading = uiState.isSaving,
                shape = PillShape,
                trailingIcon = Icons.Filled.CheckCircle,
            )
            Spacer(Modifier.height(24.dp))
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("İşlemi sil") },
                text = { Text("Bu işlemi silmek istediğine emin misin? Bu işlem geri alınamaz.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        viewModel.delete()
                    }) {
                        Text("Sil", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Vazgeç")
                    }
                },
            )
        }

        pendingOverage?.let { overage ->
            OverspendWarningDialog(
                overage = overage,
                onConfirm = {
                    viewModel.confirmSaveDespiteOverspend()
                    pendingOverage = null
                },
                onDismiss = {
                    viewModel.dismissOverspendWarning()
                    pendingOverage = null
                },
            )
        }

        pendingIncomeAmount?.let { amount ->
            IncomeDestinationDialog(
                amount = amount,
                onAddToBudget = {
                    viewModel.confirmIncomeToBudget()
                    pendingIncomeAmount = null
                },
                onAddToKumbara = {
                    viewModel.confirmIncomeToKumbara()
                    pendingIncomeAmount = null
                },
                onDismiss = {
                    viewModel.dismissIncomeDestinationChoice()
                    pendingIncomeAmount = null
                },
            )
        }
    }
}

@Composable
private fun OverspendWarningDialog(overage: Double, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Bu ayki limitini aştın", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    modifier = Modifier.size(120.dp),
                ) {
                    MascotWaveVideo(rawResId = R.raw.karinca_sad)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Bu ayki limitinin tamamını harcadın. Bu harcamanın ${turkishCurrencyFormat.format(overage)}'lik kısmı Kumbaranından düşülecek. Yine de devam etmek istiyor musun?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Devam Et") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        },
    )
}

@Composable
private fun IncomeDestinationDialog(
    amount: Double,
    onAddToBudget: () -> Unit,
    onAddToKumbara: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("${turkishCurrencyFormat.format(amount)} nereye eklensin?", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Text(
                "Bu geliri harcanabilir bütçene mi eklemek istersin, yoksa doğrudan Kumbarana mı atmak istersin?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        },
        confirmButton = {
            Button(onClick = onAddToBudget) { Text("Mevcut Bütçeye Ekle") }
        },
        dismissButton = {
            TextButton(onClick = onAddToKumbara) { Text("Kumbaraya At") }
        },
    )
}

@Composable
private fun ModeSwitchButton(
    text: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = PillShape,
        color = if (selected) selectedColor else Color.Transparent,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        )
    }
}

@Composable
private fun CategoryChip(category: CategoryEntity, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(categoryIcon(category.icon), contentDescription = null, tint = tint)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            category.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NumericKeypad(onDigit: (Char) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf(' ', '0', ','), // ' ' marker = backspace key
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (key == ' ') MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.weight(1f).aspectRatio(1.9f).clickable {
                            if (key == ' ') onBackspace() else onDigit(key)
                        },
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            if (key == ' ') {
                                Icon(Icons.Filled.Backspace, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error)
                            } else {
                                Text(key.toString(), style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
