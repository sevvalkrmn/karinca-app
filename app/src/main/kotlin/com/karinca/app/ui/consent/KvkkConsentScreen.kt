package com.karinca.app.ui.consent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karinca.app.ui.components.PrimaryButton

@Composable
fun KvkkConsentScreen(
    onAccepted: () -> Unit,
    viewModel: KvkkConsentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.accepted.collect { onAccepted() }
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(24.dp))
            Text("KVKK Onayı", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Karınca'yı kullanmaya başlamadan önce, aşağıdaki iki maddeyi onaylaman gerekiyor:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            ConsentItem(
                checked = uiState.dataStorageChecked,
                onCheckedChange = viewModel::onDataStorageCheckedChange,
                title = "Finansal verilerimin saklanmasına onay veriyorum",
                // Placeholder — the real KVKK policy text (data storage clause) replaces
                // this once finalized; see KvkkPolicy.kt.
                body = "[KVKK metni buraya gelecek — placeholder]",
            )
            Spacer(Modifier.height(16.dp))
            ConsentItem(
                checked = uiState.llmProcessingChecked,
                onCheckedChange = viewModel::onLlmProcessingCheckedChange,
                title = "Özet verilerimin AI koç için işlenmesine onay veriyorum",
                // Placeholder — the real KVKK policy text (LLM processing clause) replaces
                // this once finalized; see KvkkPolicy.kt.
                body = "[KVKK metni buraya gelecek — placeholder]",
            )

            Spacer(Modifier.height(32.dp))
            PrimaryButton(
                text = "Devam Et",
                enabled = uiState.canContinue,
                loading = uiState.isLoading,
                onClick = viewModel::accept,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ConsentItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    body: String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
