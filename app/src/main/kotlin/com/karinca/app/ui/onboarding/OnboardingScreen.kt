@file:OptIn(ExperimentalMaterial3Api::class)

package com.karinca.app.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karinca.app.R
import com.karinca.app.data.local.CoachTone
import com.karinca.app.ui.components.MascotWaveVideo
import com.karinca.app.ui.components.PrimaryButton
import com.karinca.app.ui.components.SpendRatioWarning
import com.karinca.app.ui.components.ThousandsSeparatorTransformation
import java.text.NumberFormat
import java.util.Locale

private val turkishCurrencyFormat: NumberFormat by lazy {
    NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
}

@Composable
fun OnboardingScreen(
    onCompleted: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is OnboardingEvent.Completed) onCompleted()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    if (uiState.step != OnboardingStep.INCOME) {
                        IconButton(onClick = viewModel::onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
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
            StepProgressRow(step = uiState.step, modifier = Modifier.padding(vertical = 16.dp))

            when (uiState.step) {
                OnboardingStep.INCOME -> IncomeStep(uiState, viewModel::onIncomeChange, viewModel::onContinue)
                OnboardingStep.LIMIT -> LimitStep(
                    uiState,
                    viewModel::onLimitTextChange,
                    viewModel::onContinue,
                )
                OnboardingStep.ALREADY_SPENT -> AlreadySpentStep(
                    uiState,
                    viewModel::onAlreadySpentTextChange,
                    viewModel::onContinue,
                )
                OnboardingStep.COACH_TONE -> CoachToneStep(uiState, viewModel::onCoachToneChange, viewModel::onContinue)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StepProgressRow(step: OnboardingStep, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        onboardingSteps.forEach { s ->
            val filled = s.ordinal <= step.ordinal
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(
                        if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                        RoundedCornerShape(percent = 50),
                    ),
            )
        }
    }
}

@Composable
private fun IncomeStep(uiState: OnboardingUiState, onIncomeChange: (String) -> Unit, onContinue: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.padding(bottom = 16.dp),
        ) {
            Text(
                "Birlikten kuvvet doğar!",
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

        Text(
            "Aylık Geliriniz",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Bütçeni planlamaya başlamak için düzenli aylık gelirini girer misin?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "TOPLAM GELİR",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = uiState.incomeText,
            onValueChange = onIncomeChange,
            leadingIcon = { Text("₺", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary) },
            textStyle = MaterialTheme.typography.headlineMedium,
            placeholder = { Text("0") },
            singleLine = true,
            visualTransformation = ThousandsSeparatorTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Maaş, yan gelirler ve düzenli tüm kazançlar.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }

    Spacer(Modifier.height(32.dp))
    PrimaryButton(text = "Devam Et", onClick = onContinue, enabled = uiState.canContinue)
}

@Composable
private fun LimitStep(
    uiState: OnboardingUiState,
    onLimitTextChange: (String) -> Unit,
    onContinue: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            "Harcama Limitiniz",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Aylık gelirinin ne kadarını harcamak istiyorsun? Kendine bir limit belirle.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.size(160.dp).padding(bottom = 24.dp),
        ) {
            MascotWaveVideo(rawResId = R.raw.karinca_talk)
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "AYLIK HARCAMA LİMİTİ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = uiState.limitText,
                onValueChange = onLimitTextChange,
                leadingIcon = { Text("₺", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary) },
                textStyle = MaterialTheme.typography.headlineMedium,
                singleLine = true,
                visualTransformation = ThousandsSeparatorTransformation,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Harcama", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Birikim", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(percent = 50)),
            ) {
                Box(
                    modifier = Modifier
                        .weight(uiState.spendRatioPercent.coerceAtLeast(1).toFloat())
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(percent = 50)),
                )
                Box(
                    modifier = Modifier
                        .weight((100 - uiState.spendRatioPercent).coerceAtLeast(1).toFloat())
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(percent = 50)),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Tahmini Aylık Gelir: ${turkishCurrencyFormat.format(uiState.income)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }

    if (uiState.isSpendRatioTooHigh) {
        Spacer(Modifier.height(16.dp))
        SpendRatioWarning(
            "Harcama limitin gelirinin %80'inden (${turkishCurrencyFormat.format(uiState.maxAllowedLimit)}) fazla olamaz.",
        )
    }

    Spacer(Modifier.height(32.dp))
    PrimaryButton(text = "Devam Et", onClick = onContinue, enabled = uiState.canContinue)
    Spacer(Modifier.height(8.dp))
    Text(
        "Daha sonra ayarlardan değiştirebilirsin.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun AlreadySpentStep(
    uiState: OnboardingUiState,
    onAlreadySpentTextChange: (String) -> Unit,
    onContinue: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.size(160.dp).padding(bottom = 24.dp),
        ) {
            MascotWaveVideo(rawResId = R.raw.karinca_talk)
        }
        Text(
            "Bu Ay Ne Kadar Harcadın?",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Maaşını aldığından bu yana, Karınca'yı kullanmadan önce yaklaşık ne kadar harcadın? Bilmiyorsan boş bırakabilirsin.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "YAKLAŞIK HARCAMA",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = uiState.alreadySpentText,
            onValueChange = onAlreadySpentTextChange,
            leadingIcon = { Text("₺", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary) },
            textStyle = MaterialTheme.typography.headlineMedium,
            placeholder = { Text("0") },
            singleLine = true,
            visualTransformation = ThousandsSeparatorTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (uiState.isAlreadySpentTooHigh) {
        Spacer(Modifier.height(16.dp))
        SpendRatioWarning("Bu tutar gelirinden fazla olamaz.")
    } else if (uiState.isAlreadySpentOverLimit) {
        Spacer(Modifier.height(16.dp))
        SpendRatioWarning("Bu tutar limitini aştı, bu ay Kumbara'na ekleme yapılmayacak.")
    }

    Spacer(Modifier.height(32.dp))
    // Final step while the coach-tone step is switched off (see COACH_TONE_ENABLED),
    // so it takes over CoachToneStep's finishing button.
    if (uiState.isLastStep) {
        PrimaryButton(
            text = "Kurulumu Tamamla",
            onClick = onContinue,
            enabled = uiState.canContinue,
            loading = uiState.isSaving,
            trailingIcon = Icons.Filled.CheckCircle,
        )
    } else {
        PrimaryButton(text = "Devam Et", onClick = onContinue, enabled = uiState.canContinue)
    }
}

private data class CoachToneOption(val tone: CoachTone, val title: String, val description: String, val icon: ImageVector)

private val coachToneOptions = listOf(
    CoachToneOption(CoachTone.GENTLE, "Yumuşak", "Seni kırmadan, nazikçe hatırlatmalar yapan tatlı dilli bir dost.", Icons.Filled.Favorite),
    CoachToneOption(CoachTone.BALANCED, "Dengeli", "Yeri geldiğinde tebrik eden, gerektiğinde profesyonel uyarılar yapan koç.", Icons.Filled.Balance),
    CoachToneOption(CoachTone.BRUTAL, "Sert", "Disipline önem veren, bütçeni aşmanı engelleyen kararlı bir otorite.", Icons.Filled.Gavel),
)

private fun coachToneImageRes(tone: CoachTone): Int = when (tone) {
    CoachTone.GENTLE -> R.drawable.yumusak_karinca
    CoachTone.BALANCED -> R.drawable.dengeli_karinca
    CoachTone.BRUTAL -> R.drawable.sert_karinca
}

@Composable
private fun CoachToneStep(uiState: OnboardingUiState, onToneChange: (CoachTone) -> Unit, onContinue: () -> Unit) {
    Image(
        painter = painterResource(coachToneImageRes(uiState.coachTone)),
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(bottom = 16.dp),
    )
    Text(
        "Koçluk Tarzı",
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "Karınca Koçu sana nasıl hitap etsin? Senin için en uygun tonu seç.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(24.dp))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        coachToneOptions.forEach { option ->
            val selected = uiState.coachTone == option.tone
            // Same white card for every state — the alpha-blended tint previously used for
            // "selected" computed to a color almost indistinguishable from white/the icon
            // circle's own backdrop, which is exactly the flat, wrong-looking background
            // being reported. Selection is conveyed by the border + filled radio alone.
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    // Clip before border/clickable so the press ripple is cropped to the
                    // rounded card instead of drawing as a sharp-cornered rectangle over it.
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = if (selected) 2.dp else 0.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clickable { onToneChange(option.tone) },
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(option.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(option.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(option.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    RadioButton(selected = selected, onClick = { onToneChange(option.tone) })
                }
            }
        }
    }

    Spacer(Modifier.height(32.dp))
    PrimaryButton(
        text = "Kurulumu Tamamla",
        onClick = onContinue,
        enabled = uiState.canContinue,
        loading = uiState.isSaving,
        trailingIcon = Icons.Filled.CheckCircle,
    )
}
