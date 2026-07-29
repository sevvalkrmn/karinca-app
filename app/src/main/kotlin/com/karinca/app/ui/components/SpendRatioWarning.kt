package com.karinca.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

private val turkishCurrencyFormat: NumberFormat by lazy {
    NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
}

// Shared by the onboarding wizard's Harcama Limiti step and Kişisel Bilgiler — shown
// whenever the spend/income ratio leaves less than 20% for saving. Both screens also
// block continuing/saving while this is true (see isSpendRatioTooHigh in each
// ViewModel) — this explains why the button is disabled, not just a passive nudge.
@Composable
fun SpendRatioWarning(maxAllowedLimit: Double, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Harcama limitin gelirinin %80'inden (${turkishCurrencyFormat.format(maxAllowedLimit)}) fazla olamaz.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}
