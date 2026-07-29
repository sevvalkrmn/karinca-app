@file:OptIn(ExperimentalMaterial3Api::class)

package com.karinca.app.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karinca.app.ui.components.LabeledIconField
import com.karinca.app.ui.components.PasswordRequirementsChecklist
import com.karinca.app.ui.components.PrimaryButton
import com.karinca.app.ui.navigation.KarincaRoute

@Composable
fun ResetPasswordScreen(
    email: String,
    onAuthenticated: (KarincaRoute) -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthEvent.Authenticated -> onAuthenticated(event.destination)
                is AuthEvent.PasswordResetCodeSent -> snackbarHostState.showSnackbar("Kod tekrar gönderildi")
                // Not reachable from this screen, but AuthEvent is shared across every
                // auth screen so every collector must handle every case.
                is AuthEvent.OtpSent, is AuthEvent.EmailConfirmationRequired -> Unit
            }
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Şifreni Yenile", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(
                "$email adresine gönderilen kodu gir",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = uiState.otpCode,
                onValueChange = { if (it.length <= 6) viewModel.onOtpCodeChange(it) },
                label = { Text("Doğrulama Kodu") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { viewModel.resendPasswordResetCode(email) }) {
                Text("Kodu tekrar gönder")
            }

            Spacer(Modifier.height(16.dp))
            LabeledIconField(
                label = "Yeni Şifre",
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                leadingIcon = Icons.Filled.Lock,
                placeholder = "En az 8 karakter",
                visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = viewModel::onTogglePasswordVisibility) {
                        Icon(
                            if (uiState.isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                        )
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
            LabeledIconField(
                label = "Yeni Şifre Tekrar",
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                leadingIcon = Icons.Filled.Lock,
                placeholder = "Şifreni tekrar gir",
                visualTransformation = if (uiState.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = viewModel::onToggleConfirmPasswordVisibility) {
                        Icon(
                            if (uiState.isConfirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                        )
                    }
                },
            )
            Spacer(Modifier.height(12.dp))
            PasswordRequirementsChecklist(uiState)
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = "Şifreyi Güncelle",
                loading = uiState.isLoading,
                enabled = uiState.otpCode.length == 6 && uiState.isSignUpPasswordValid,
                onClick = { viewModel.resetPassword(email) },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
