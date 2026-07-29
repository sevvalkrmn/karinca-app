package com.karinca.app.ui.consent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karinca.app.ui.components.PrimaryButton

// Real KVKK disclosure texts. "**bold**" markers are rendered bold by parseBoldMarkdown()
// below; a leading "## " line is rendered as a document sub-title. Shown in full via a
// dialog (see ConsentItem) rather than inline — only a short "I read this" sentence sits
// in the checkbox row itself.
private val DATA_STORAGE_DISCLOSURE = """
## Finansal Verilerin Saklanması Aydınlatma Metni

Gelir-gider kayıtlarınız, bütçe limitleriniz ve finansal hedefleriniz; bütçe takip hizmetinin sunulması, kayıtlarınıza tekrar erişebilmeniz ve kişiselleştirilmiş analizlerin oluşturulabilmesi amacıyla **Şevval Karaman** tarafından işlenir.

Verileriniz, veri tabanı ve barındırma hizmeti sağlayıcımız **Supabase** altyapısında güvenli şekilde saklanır. Verileriniz reklam amacıyla kullanılmaz ve yetkisiz üçüncü kişilerle paylaşılmaz.

KVKK'nın 11. maddesi kapsamındaki haklarınız için sevvalkrmn14@gmail.com üzerinden bizimle iletişime geçebilirsiniz.
""".trimIndent()

private val AI_PROCESSING_DISCLOSURE = """
## Yapay Zekâ Destekli Bütçe Analizi Aydınlatma Metni

**Şevval Karaman**, veri sorumlusu sıfatıyla; gelir-gider özetlerinizi, bütçe ve kategori limitlerinizi ve finansal hedeflerinizi, yapay zekâ destekli bütçe analizi sunmak amacıyla işlemektedir.

Bu veriler yalnızca bütçe durumunuzu analiz etmek ve size kişiselleştirilmiş öneriler oluşturmak için kullanılır. Adınız, iletişim bilgileriniz, banka hesap bilgileriniz ve kart bilgileriniz yapay zekâ sistemine gönderilmez.

Verileriniz, analiz hizmetinin sunulması amacıyla **OpenAI** ile paylaşılabilir. Analiz için gönderilen veriler yalnızca işlem süresince kullanılır ve model eğitimi amacıyla kullanılmaz.

KVKK'nın 11. maddesi kapsamında kişisel verileriniz hakkında bilgi talep etme, düzeltilmesini, silinmesini veya yok edilmesini isteme haklarına sahipsiniz. Taleplerinizi sevvalkrmn14@gmail.com üzerinden iletebilirsiniz.
""".trimIndent()

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
                label = "Finansal verilerimin bütçe takip hizmetinin sunulması amacıyla Supabase altyapısında saklanmasına ilişkin aydınlatma metnini okudum.",
                disclosureText = DATA_STORAGE_DISCLOSURE,
            )
            Spacer(Modifier.height(16.dp))
            ConsentItem(
                checked = uiState.llmProcessingChecked,
                onCheckedChange = viewModel::onLlmProcessingCheckedChange,
                label = "Özet verilerimin yapay zekâ destekli bütçe analizi için işlenmesine ilişkin aydınlatma metnini okudum.",
                disclosureText = AI_PROCESSING_DISCLOSURE,
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
    label: String,
    disclosureText: String,
) {
    var showDisclosure by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .clickable { onCheckedChange(!checked) }
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // onCheckedChange = null: the checkbox is purely a visual indicator here —
            // the surrounding Row's clickable is the single source of truth for toggling,
            // so tapping anywhere on the card (not just the tiny checkbox) works, without
            // double-toggling from two independent click handlers firing on one tap.
            Checkbox(checked = checked, onCheckedChange = null)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                TextButton(
                    onClick = { showDisclosure = true },
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    Text("Aydınlatma metnini oku", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    if (showDisclosure) {
        val (header, rest) = splitLeadingHeader(disclosureText)
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            confirmButton = {
                TextButton(onClick = { showDisclosure = false }) { Text("Kapat") }
            },
            title = header?.let { { Text(it, style = MaterialTheme.typography.titleMedium) } },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(parseBoldMarkdown(rest), style = MaterialTheme.typography.bodyMedium)
                }
            },
        )
    }
}

// A leading "## " line (this project's only markdown need) is a document sub-title,
// not a plain paragraph — split it off so it can be styled distinctly.
private fun splitLeadingHeader(text: String): Pair<String?, String> {
    val firstLine = text.lineSequence().firstOrNull() ?: return null to text
    return if (firstLine.startsWith("## ")) {
        firstLine.removePrefix("## ") to text.removePrefix(firstLine).trimStart('\n')
    } else {
        null to text
    }
}

private fun parseBoldMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var remaining = text
    while (true) {
        val start = remaining.indexOf("**")
        if (start == -1) {
            append(remaining)
            break
        }
        append(remaining.substring(0, start))
        val end = remaining.indexOf("**", start + 2)
        if (end == -1) {
            append(remaining.substring(start))
            break
        }
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(remaining.substring(start + 2, end))
        }
        remaining = remaining.substring(end + 2)
    }
}
