package com.karinca.app.ui.theme

import androidx.compose.ui.graphics.Color

// Category colors are stored as hex strings (CategoryEntity.color) so they're data-driven
// rather than tied to fixed M3 roles — this parses them for use as a Compose Color.
fun String.toComposeColor(): Color = Color(android.graphics.Color.parseColor(this))
