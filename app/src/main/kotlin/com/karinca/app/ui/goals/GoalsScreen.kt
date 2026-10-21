@file:OptIn(ExperimentalMaterial3Api::class)

package com.karinca.app.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karinca.app.R
import com.karinca.app.data.local.entity.GoalEntity
import com.karinca.app.ui.components.KarincaBottomBar
import com.karinca.app.ui.components.KarincaTopBar
import com.karinca.app.ui.components.MascotWaveVideo
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val turkishCurrencyFormat: NumberFormat by lazy {
    NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
}

private val goalDateFormat: DateTimeFormatter by lazy {
    DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("tr", "TR"))
}

private val PillShape = RoundedCornerShape(percent = 50)

@Composable
fun GoalsScreen(
    onNavigateHome: () -> Unit,
    onNavigateReports: () -> Unit,
    onNavigateProfile: () -> Unit,
    onAddTransaction: () -> Unit,
    viewModel: GoalsViewModel = hiltViewModel(),
) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var showInsufficientBudgetWarning by remember { mutableStateOf(false) }
    var pendingConfirmation by remember { mutableStateOf<GoalsEvent.ConfirmDailyAmount?>(null) }
    var failedGoalNames by remember { mutableStateOf<List<String>?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                GoalsEvent.Created -> showAddDialog = false
                GoalsEvent.InsufficientBudget -> showInsufficientBudgetWarning = true
                is GoalsEvent.ConfirmDailyAmount -> pendingConfirmation = event
                is GoalsEvent.GoalsFailed -> failedGoalNames = event.names
            }
        }
    }

    Scaffold(
        topBar = { KarincaTopBar(onProfileClick = onNavigateProfile) },
        bottomBar = {
            KarincaBottomBar(
                activeTab = KarincaBottomBar.Tab.HEDEFLER,
                onNavigateHome = onNavigateHome,
                onNavigateGoals = {},
                onNavigateReports = onNavigateReports,
                onNavigateProfile = onNavigateProfile,
                onAddTransaction = onAddTransaction,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Hedef Ekle") },
            )
        },
    ) { padding ->
        // Captured into a plain local val — collectAsStateWithLifecycle's delegated
        // property can't be smart-cast across branches on its own.
        val currentGoals = goals
        if (currentGoals == null) {
            // Room's first emission hasn't arrived yet — render nothing rather than
            // flashing the empty state for goals that actually exist.
        } else if (currentGoals.isEmpty()) {
            Column(

                modifier = Modifier.fillMaxSize().padding(padding).padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(140.dp),
                ) {
                    MascotWaveVideo(rawResId = R.raw.karinca_goals, modifier = Modifier.padding(24.dp))
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    "Hedefler nasıl çalışır?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Bir tutar ve tarih seç, günlük ne kadar biriktirmen gerektiğini hesaplayalım. Bütçen gerçekten yettiği sürece çubuğun dolar; yetmezse haber veririz. Sağ alttaki + ile başla.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().padding(padding),
            ) {
                items(currentGoals, key = { it.id }) { goal ->
                    GoalCard(
                        goal = goal,
                        onClick = { editingGoal = goal },
                        onComplete = { viewModel.completeGoal(goal.id) },
                    )
                }
            }
        }

        if (showAddDialog) {
            AddGoalSheet(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, target, targetDate ->
                    viewModel.addGoal(name, target, targetDate)
                },
            )
        }

        if (showInsufficientBudgetWarning) {
            InsufficientBudgetDialog(onDismiss = { showInsufficientBudgetWarning = false })
        }

        pendingConfirmation?.let { confirmation ->
            ConfirmDailyAmountDialog(
                dailyAmount = confirmation.dailyAmount,
                onConfirm = {
                    viewModel.confirmAddGoal(confirmation.name, confirmation.targetAmount, confirmation.targetDate)
                    pendingConfirmation = null
                },
                onDismiss = { pendingConfirmation = null },
            )
        }

        failedGoalNames?.let { names ->
            GoalFailedDialog(names = names, onDismiss = { failedGoalNames = null })
        }

        editingGoal?.let { goal ->
            EditGoalSheet(
                goal = goal,
                onDismiss = { editingGoal = null },
                onSave = { name, target, current, targetDate ->
                    viewModel.updateGoal(goal.id, name, target, current, targetDate)
                    editingGoal = null
                },
                onDelete = {
                    viewModel.deleteGoal(goal.id)
                    editingGoal = null
                },
            )
        }
    }
}

@Composable
private fun GoalCard(goal: GoalEntity, onClick: () -> Unit, onComplete: () -> Unit) {
    val savedSoFar = goal.displayedCurrentAmount()
    val percent = if (goal.targetAmount > 0) (savedSoFar / goal.targetAmount * 100.0) else 0.0
    val remaining = (goal.targetAmount - savedSoFar).coerceAtLeast(0.0)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(goal.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    val subtitle = "Hedef: ${turkishCurrencyFormat.format(goal.targetAmount)} · ${goal.targetDate.format(goalDateFormat)}"
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text("%${percent.roundToInt()}", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary)
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { (percent / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    if (remaining > 0) "${turkishCurrencyFormat.format(remaining)} kaldı" else "Hedefe ulaşıldı!",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onComplete) {
                    Text("Tamamla")
                }
            }
        }
    }
}

@Composable
private fun AddGoalSheet(onDismiss: () -> Unit, onConfirm: (String, Double, LocalDate) -> Unit) {
    var name by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf<LocalDate?>(null) }
    val target = targetText.replace(',', '.').toDoubleOrNull()
    val date = targetDate

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        GoalForm(
            title = "Yeni Hedef",
            name = name,
            onNameChange = { name = it },
            targetText = targetText,
            onTargetTextChange = { targetText = filterAmountInput(it) },
            amount = null,
            targetDate = targetDate,
            onTargetDateChange = { targetDate = it },
            onDelete = null,
            confirmLabel = "Oluştur",
            confirmEnabled = name.isNotBlank() && target != null && target > 0 && date != null,
            onConfirm = { if (target != null && date != null) onConfirm(name, target, date) },
        )
    }
}

private class GoalAmountField(val text: String, val onTextChange: (String) -> Unit, val label: String)

@Composable
private fun EditGoalSheet(
    goal: GoalEntity,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, LocalDate) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember(goal.id) { mutableStateOf(goal.name) }
    var targetText by remember(goal.id) { mutableStateOf(formatPlain(goal.targetAmount)) }
    var currentText by remember(goal.id) { mutableStateOf(formatPlain(goal.currentAmount)) }
    var targetDate by remember(goal.id) { mutableStateOf<LocalDate?>(goal.targetDate) }
    val target = targetText.replace(',', '.').toDoubleOrNull()
    val current = currentText.replace(',', '.').toDoubleOrNull()
    val date = targetDate

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        GoalForm(
            title = "Hedefi Düzenle",
            name = name,
            onNameChange = { name = it },
            targetText = targetText,
            onTargetTextChange = { targetText = filterAmountInput(it) },
            amount = GoalAmountField(currentText, { currentText = filterAmountInput(it) }, "Mevcut Tutar (₺)"),
            targetDate = targetDate,
            onTargetDateChange = { targetDate = it },
            onDelete = onDelete,
            confirmLabel = "Kaydet",
            confirmEnabled = name.isNotBlank() && target != null && target > 0 && current != null && current >= 0 && date != null,
            onConfirm = { if (target != null && current != null && date != null) onSave(name, target, current, date) },
        )
    }
}

@Composable
private fun GoalForm(
    title: String,
    name: String,
    onNameChange: (String) -> Unit,
    targetText: String,
    onTargetTextChange: (String) -> Unit,
    amount: GoalAmountField?,
    targetDate: LocalDate?,
    onTargetDateChange: (LocalDate?) -> Unit,
    onDelete: (() -> Unit)?,
    confirmLabel: String,
    confirmEnabled: Boolean,
    onConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Without these, opening the keyboard to type a name/amount shrinks the
            // sheet's visible area with nothing scrollable to reach whatever falls below
            // — which was cutting off the confirm button. imePadding keeps the content
            // clear of the keyboard; verticalScroll is the fallback if it still doesn't fit.
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        }
        HorizontalDivider()
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Hedef Adı") },
            leadingIcon = { Icon(Icons.Filled.Savings, contentDescription = null) },
            singleLine = true,
            shape = PillShape,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = targetText,
            onValueChange = onTargetTextChange,
            label = { Text("Hedef Tutar (₺)") },
            leadingIcon = { Icon(Icons.Filled.Payments, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = PillShape,
            modifier = Modifier.fillMaxWidth(),
        )
        if (amount != null) {
            OutlinedTextField(
                value = amount.text,
                onValueChange = amount.onTextChange,
                label = { Text(amount.label) },
                leadingIcon = { Icon(Icons.Filled.Payments, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = PillShape,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        GoalDatePickerField(targetDate = targetDate, onTargetDateChange = onTargetDateChange)
        if (onDelete != null) {
            TextButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(4.dp))
                Text("Hedefi Sil", color = MaterialTheme.colorScheme.error)
            }
        }
        Button(
            onClick = onConfirm,
            enabled = confirmEnabled,
            shape = PillShape,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(confirmLabel)
        }
    }
}

@Composable
private fun GoalDatePickerField(targetDate: LocalDate?, onTargetDateChange: (LocalDate?) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    // OutlinedTextField's own tap gesture (placing a cursor) can swallow a plain
    // Modifier.clickable overlay before it ever fires, so the field goes unresponsive.
    // Passing the same interactionSource in and watching its press state is the reliable
    // way to detect a tap on a readOnly field.
    val isPressed by interactionSource.collectIsPressedAsState()
    LaunchedEffect(isPressed) {
        if (isPressed) showPicker = true
    }

    OutlinedTextField(
        value = targetDate?.format(goalDateFormat) ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text("Hedef Tarihi") },
        placeholder = { Text("Tarih seç") },
        leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
        interactionSource = interactionSource,
        shape = PillShape,
        modifier = Modifier.fillMaxWidth(),
    )

    if (showPicker) {
        GoalDatePickerDialog(
            initialDate = targetDate,
            onDismiss = { showPicker = false },
            onConfirm = { date ->
                onTargetDateChange(date)
                showPicker = false
            },
        )
    }
}

@Composable
private fun GoalDatePickerDialog(initialDate: LocalDate?, onDismiss: () -> Unit, onConfirm: (LocalDate?) -> Unit) {
    val today = LocalDate.now()
    val todayMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = (initialDate ?: today).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= todayMillis
        },
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    onConfirm(millis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() })
                },
            ) { Text("Tamam") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    ) {
        DatePicker(state = datePickerState)
    }
}

private fun formatPlain(amount: Double): String {
    val whole = amount.toLong()
    return if (amount == whole.toDouble()) whole.toString() else amount.toString().replace('.', ',')
}

// Keeps only digits and a single decimal separator (comma or dot) — the on-screen
// KeyboardType.Decimal hint alone doesn't stop a physical keyboard or paste from
// entering letters, which used to leave the amount field silently un-parseable and the
// confirm button permanently disabled with no explanation.
private fun filterAmountInput(raw: String): String {
    val sb = StringBuilder()
    var seenSeparator = false
    for (c in raw) {
        when {
            c.isDigit() -> sb.append(c)
            (c == ',' || c == '.') && !seenSeparator -> {
                seenSeparator = true
                sb.append(c)
            }
        }
    }
    return sb.toString()
}

@Composable
private fun ConfirmDailyAmountDialog(dailyAmount: Double, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bu hedefi onaylıyor musun?") },
        text = {
            Text(
                "Bu hedef için hesabından her gün ${turkishCurrencyFormat.format(dailyAmount)} eksilecek (harcanabilir bakiyenden ayrılacak). Yine de oluşturmak istiyor musun?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Oluştur") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        },
    )
}

@Composable
private fun InsufficientBudgetDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Bütçen buna yetmiyor", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
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
                    "Bu hedefi eklersen harcanabilir bütçen 0 TL'nin altına düşer. Karınca kararınca — önce mevcut dengeni koruyalım, sonra hedefe bakarız.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Anladım") }
        },
    )
}

@Composable
private fun GoalFailedDialog(names: List<String>, onDismiss: () -> Unit) {
    val nameList = names.joinToString(", ") { "\"$it\"" }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Hedefini gerçekleştiremedin", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
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
                    "$nameList hedefini gerçekleştiremedin — bütçen yetmediği için hedefi kaldırdık.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Anladım") }
        },
    )
}
