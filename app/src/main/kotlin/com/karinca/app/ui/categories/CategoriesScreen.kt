package com.karinca.app.ui.categories

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karinca.app.R
import com.karinca.app.data.local.entity.CategoryEntity
import com.karinca.app.ui.components.KarincaBottomBar
import com.karinca.app.ui.theme.toComposeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    onOpenCategoryDetail: (String) -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateGoals: () -> Unit,
    onNavigateReports: () -> Unit,
    onNavigateProfile: () -> Unit,
    onAddTransaction: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Kategoriler",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateProfile) {
                        Icon(
                            Icons.Filled.AccountCircle,
                            contentDescription = "Profilim",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                },
            )
        },
        bottomBar = {
            KarincaBottomBar(
                activeTab = KarincaBottomBar.Tab.NONE,
                onNavigateHome = onNavigateHome,
                onNavigateGoals = onNavigateGoals,
                onNavigateReports = onNavigateReports,
                onNavigateProfile = onNavigateProfile,
                onAddTransaction = onAddTransaction,
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item(span = { GridItemSpan(2) }) {
                BudgetBanner()
            }
            items(categories, key = { it.id }) { category ->
                CategoryCard(category, onClick = { onOpenCategoryDetail(category.id) })
            }
            item(span = { GridItemSpan(2) }) {
                EncouragementFooter()
            }
        }
    }
}

@Composable
private fun BudgetBanner() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Bütçeni yönet",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Yeni Harcama",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Image(
                painter = painterResource(R.drawable.ic_karinca_logo),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                alpha = 0.8f,
            )
        }
    }
}

@Composable
private fun CategoryCard(category: CategoryEntity, onClick: () -> Unit) {
    val tint = category.color.toComposeColor()
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 2.dp,
        modifier = Modifier.aspectRatio(1f).clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(tint.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(categoryIcon(category.icon), contentDescription = null, tint = tint)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                category.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun EncouragementFooter() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_karinca_logo),
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            alpha = 0.4f,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Her kuruşun hesabını bilen karıncalar gibi büyüyoruz!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
