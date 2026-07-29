package com.karinca.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karinca.app.ui.auth.AuthUiState

// Live checklist under the password fields — updates on every keystroke instead of
// making the user submit repeatedly to discover one unmet rule at a time via a snackbar.
// Shared by SignUpScreen and ResetPasswordScreen — both set a new password using the
// exact same AuthUiState fields/rules, so this one component covers both.
@Composable
fun PasswordRequirementsChecklist(uiState: AuthUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PasswordRequirementRow("En az 8 karakter", uiState.passwordHasMinLength)
        PasswordRequirementRow("En az 1 büyük harf", uiState.passwordHasUppercase)
        PasswordRequirementRow("En az 1 rakam", uiState.passwordHasDigit)
        if (uiState.confirmPassword.isNotEmpty()) {
            PasswordRequirementRow("Şifreler eşleşiyor", uiState.passwordsMatch)
        }
    }
}

@Composable
private fun PasswordRequirementRow(label: String, satisfied: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (satisfied) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = null,
            tint = if (satisfied) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (satisfied) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
