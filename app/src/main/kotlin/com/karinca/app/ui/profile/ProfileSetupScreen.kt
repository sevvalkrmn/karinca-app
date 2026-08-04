@file:OptIn(ExperimentalMaterial3Api::class)

package com.karinca.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karinca.app.data.local.CoachTone
import com.karinca.app.ui.components.PrimaryButton
import com.karinca.app.ui.components.SpendRatioWarning
import com.karinca.app.ui.components.ThousandsSeparatorTransformation
import java.text.NumberFormat
import java.util.Locale

private val PillShape = RoundedCornerShape(percent = 50)

private val turkishCurrencyFormat: NumberFormat by lazy {
    NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
}

@Composable
fun ProfileSetupScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ProfileSetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { onSaved() }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Kişisel Bilgiler", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = uiState.firstNameText,
                onValueChange = viewModel::onFirstNameChange,
                label = { Text("Ad") },
                shape = PillShape,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.lastNameText,
                onValueChange = viewModel::onLastNameChange,
                label = { Text("Soyad") },
                shape = PillShape,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.email,
                onValueChange = {},
                readOnly = true,
                label = { Text("E-posta") },
                shape = PillShape,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.regularIncomeText,
                onValueChange = viewModel::onRegularIncomeChange,
                label = { Text("Aylık Gelir (₺)") },
                singleLine = true,
                visualTransformation = ThousandsSeparatorTransformation,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = PillShape,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.budgetLimitText,
                onValueChange = viewModel::onBudgetLimitChange,
                label = { Text("Aylık Bütçe Limiti (₺)") },
                singleLine = true,
                visualTransformation = ThousandsSeparatorTransformation,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = PillShape,
                modifier = Modifier.fillMaxWidth(),
            )

            if (uiState.isSpendRatioTooHigh) {
                SpendRatioWarning(
                    "Harcama limitin gelirinin %80'inden (${turkishCurrencyFormat.format(uiState.maxAllowedLimit)}) fazla olamaz.",
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Koç Tonu", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val tones = listOf(CoachTone.GENTLE to "Nazik", CoachTone.BALANCED to "Dengeli", CoachTone.BRUTAL to "Sert")
                    tones.forEachIndexed { index, (tone, label) ->
                        SegmentedButton(
                            selected = uiState.coachTone == tone,
                            onClick = { viewModel.onCoachToneChange(tone) },
                            shape = SegmentedButtonDefaults.itemShape(index, tones.size),
                            label = { Text(label) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            PrimaryButton(
                text = "Kaydet",
                onClick = viewModel::save,
                enabled = uiState.canSave,
                loading = uiState.isSaving,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
