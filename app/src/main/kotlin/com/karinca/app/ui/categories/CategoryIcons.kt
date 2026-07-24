package com.karinca.app.ui.categories

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

// Maps CategoryEntity.icon (the string identifier stored per the seed migration) to an
// actual Compose icon. Covers exactly the icons used in
// supabase/migrations/20260714010000_seed_default_categories.sql — add a case here
// whenever a new category icon identifier is introduced.
fun categoryIcon(iconName: String): ImageVector = when (iconName) {
    "shopping_cart" -> Icons.Filled.ShoppingCart
    "coffee" -> Icons.Filled.Coffee
    "directions_bus" -> Icons.Filled.DirectionsBus
    "receipt_long" -> Icons.Filled.ReceiptLong
    "home" -> Icons.Filled.Home
    "sports_esports" -> Icons.Filled.SportsEsports
    "shopping_bag" -> Icons.Filled.ShoppingBag
    "medical_services" -> Icons.Filled.MedicalServices
    "autorenew" -> Icons.Filled.Autorenew
    "school" -> Icons.Filled.School
    "payments" -> Icons.Filled.Payments
    "trending_up" -> Icons.Filled.TrendingUp
    "more_horiz" -> Icons.Filled.MoreHoriz
    else -> Icons.Filled.MoreHoriz
}
