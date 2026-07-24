package com.karinca.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karinca.app.R
import com.karinca.app.ui.components.GoogleSignInButton
import com.karinca.app.ui.components.KarincaWordmark
import com.karinca.app.ui.components.LabeledIconField
import com.karinca.app.ui.components.MascotWaveVideo
import com.karinca.app.ui.components.PrimaryButton
import com.karinca.app.ui.navigation.KarincaRoute

private val PillShape = RoundedCornerShape(percent = 50)

@Composable
fun SignUpScreen(
    onAuthenticated: (KarincaRoute) -> Unit,
    onNeedsOtp: (String) -> Unit,
    onNavigateToSignIn: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val onGoogleClick = rememberGoogleSignInAction(
        onResult = { result -> viewModel.signInWithGoogleIdToken(result.idToken, result.rawNonce) },
        onError = viewModel::showError,
    )

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthEvent.Authenticated -> onAuthenticated(event.destination)
                is AuthEvent.OtpSent -> onNeedsOtp(event.phone)
                is AuthEvent.EmailConfirmationRequired -> {
                    snackbarHostState.showSnackbar("E-postana gönderilen bağlantıya tıklayıp hesabını onayla, sonra giriş yap.")
                    onNavigateToSignIn()
                }
            }
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            KarincaWordmark(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            ) {
                // Speech-bubble quote above the mascot.
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.padding(bottom = 16.dp),
                ) {
                    Text(
                        "\"Büyük hedefler, küçük adımlarla başlar. Aramıza hoş geldin!\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(112.dp).padding(bottom = 24.dp),
                ) {
                    MascotWaveVideo(rawResId = R.raw.karinca_talk)
                }

                // Registration card.
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(32.dp)) {
                        Text(
                            "Aramıza Katıl!",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Hayallerini biriktirmeye bugün başla.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(24.dp))

                        LabeledIconField(
                            label = "Ad Soyad",
                            value = uiState.name,
                            onValueChange = viewModel::onNameChange,
                            leadingIcon = Icons.Filled.Person,
                            placeholder = "Adın ve Soyadın",
                            shape = PillShape,
                            containerColor = MaterialTheme.colorScheme.surfaceBright,
                        )
                        Spacer(Modifier.height(16.dp))
                        LabeledIconField(
                            label = "E-posta",
                            value = uiState.email,
                            onValueChange = viewModel::onEmailChange,
                            leadingIcon = Icons.Filled.Email,
                            placeholder = "ornek@eposta.com",
                            shape = PillShape,
                            containerColor = MaterialTheme.colorScheme.surfaceBright,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        )
                        Spacer(Modifier.height(16.dp))
                        LabeledIconField(
                            label = "Şifre",
                            value = uiState.password,
                            onValueChange = viewModel::onPasswordChange,
                            leadingIcon = Icons.Filled.Lock,
                            placeholder = "En az 8 karakter",
                            shape = PillShape,
                            containerColor = MaterialTheme.colorScheme.surfaceBright,
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
                            label = "Şifre Tekrar",
                            value = uiState.confirmPassword,
                            onValueChange = viewModel::onConfirmPasswordChange,
                            leadingIcon = Icons.Filled.Lock,
                            placeholder = "Şifreni tekrar gir",
                            shape = PillShape,
                            containerColor = MaterialTheme.colorScheme.surfaceBright,
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
                        Spacer(Modifier.height(24.dp))
                        PrimaryButton(
                            text = "Kayıt Ol",
                            shape = PillShape,
                            loading = uiState.isLoading,
                            onClick = viewModel::signUp,
                        )

                        Spacer(Modifier.height(20.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HorizontalDivider(modifier = Modifier.weight(1f))
                            Text(
                                "  veya  ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(20.dp))
                        GoogleSignInButton(
                            text = "Google ile devam et",
                            shape = PillShape,
                            enabled = !uiState.isLoading,
                            onClick = onGoogleClick,
                        )

                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Zaten hesabın var mı? ", style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = onNavigateToSignIn, contentPadding = PaddingValues(0.dp)) {
                                Text("Giriş Yap")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
