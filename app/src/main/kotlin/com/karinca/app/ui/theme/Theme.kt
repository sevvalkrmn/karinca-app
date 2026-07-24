package com.karinca.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Light-only for now — DESIGN.md defines no dark-theme tokens.
private val KarincaColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    inversePrimary = InversePrimary,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    surfaceTint = SurfaceTint,
    surfaceDim = SurfaceDim,
    surfaceBright = SurfaceBright,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    // "Fixed" tone roles (primaryFixed, secondaryFixed, etc.) from DESIGN.md are NOT
    // wired in here — Material3 1.3.2 (this project's compose-bom 2024.09.00) doesn't
    // expose them as lightColorScheme() parameters. The Color.kt constants are kept as
    // the design-system source of truth; revisit if/when Material3 is upgraded.
)

// Shapes are left at Material3's own defaults, on purpose — only color and typography
// carry brand identity; component shape/sizing/layout follows standard M3.
@Composable
fun KarincaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KarincaColorScheme,
        typography = KarincaTypography,
        content = content,
    )
}
