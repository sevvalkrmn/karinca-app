@file:OptIn(ExperimentalMaterial3Api::class)

package com.karinca.app.ui.coach

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karinca.app.R
import com.karinca.app.data.local.entity.WeeklyReportEntity
import com.karinca.app.ui.components.KarincaBottomBar
import com.karinca.app.ui.components.KarincaTopBar
import com.karinca.app.ui.components.MascotWaveVideo
import java.time.format.DateTimeFormatter
import java.util.Locale

private val reportDateFormat: DateTimeFormatter by lazy {
    DateTimeFormatter.ofPattern("d MMMM", Locale("tr", "TR"))
}

@Composable
fun WeeklyReportScreen(
    onNavigateHome: () -> Unit,
    onNavigateGoals: () -> Unit,
    onNavigateReports: () -> Unit,
    onNavigateProfile: () -> Unit,
    onAddTransaction: () -> Unit,
    viewModel: WeeklyReportViewModel = hiltViewModel(),
) {
    val report by viewModel.latestReport.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { KarincaTopBar(onProfileClick = onNavigateProfile) },
        bottomBar = {
            KarincaBottomBar(
                activeTab = KarincaBottomBar.Tab.KOC,
                onNavigateHome = onNavigateHome,
                onNavigateGoals = onNavigateGoals,
                onNavigateReports = onNavigateReports,
                onOpenCoach = {},
                onAddTransaction = onAddTransaction,
            )
        },
    ) { padding ->
        if (report == null) {
            EmptyReportState(modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            ReportContent(report = report!!, modifier = Modifier.fillMaxSize().padding(padding))
        }
    }
}

@Composable
private fun EmptyReportState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shadowElevation = 4.dp,
            modifier = Modifier.size(140.dp),
        ) {
            MascotWaveVideo(rawResId = R.raw.karinca_talk)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Henüz hazır bir raporum yok",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Sana gerçek bir haftalık değerlendirme sunabilmem için en az bir haftalık harcama/gelir kaydın olması lazım. O hazır olunca burada seni bekliyor olacak!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ReportContent(report: WeeklyReportEntity, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(4.dp))
        Text(
            "${report.weekStart.format(reportDateFormat)} – ${report.weekEnd.format(reportDateFormat)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(64.dp),
                ) {
                    MascotWaveVideo(rawResId = R.raw.karinca_talk)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    report.narration ?: "Bu haftaki yorumum hazırlanıyor, birazdan burada olacak.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
