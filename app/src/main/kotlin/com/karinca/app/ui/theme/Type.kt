package com.karinca.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.karinca.app.R

// No font files were bundled with the design hand-off — Inter (chosen as the closest
// widely-available match to iOS's SF Pro, which is Apple-proprietary and not
// distributable here) is loaded via Android's Downloadable Fonts API (Google Play
// Services provider) instead of guessing at binary font files. font_certs.xml is the
// standard, unmodified Google-published certificate set for this provider.
private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val interQuery = GoogleFont("Inter")

private fun inter(weight: FontWeight): FontFamily = FontFamily(
    Font(googleFont = interQuery, fontProvider = googleFontProvider, weight = weight)
)

private val ExtraBold = inter(FontWeight.ExtraBold) // 800 — display-lg
private val Bold = inter(FontWeight.Bold) // 700 — headline-*, label-sm
private val SemiBold = inter(FontWeight.SemiBold) // 600 — label-md
private val Medium = inter(FontWeight.Medium) // 500 — body-lg
private val Regular = inter(FontWeight.Normal) // 400 — body-md, default fallback

// M3 defines 15 text styles; DESIGN.md only specifies 7. Every style below at least gets
// Inter; only the 7 named tokens get their exact size/weight/line-height/tracking from
// DESIGN.md. The rest keep M3's default scale with the brand font applied.
private val base = Typography()

val KarincaTypography = Typography(
    displayLarge = base.displayLarge.copy(
        fontFamily = ExtraBold,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.02).em,
    ),
    displayMedium = base.displayMedium.copy(fontFamily = Regular),
    displaySmall = base.displaySmall.copy(fontFamily = Regular),
    // headline-lg-mobile used here, not the 32sp desktop headline-lg — DESIGN.md names
    // mobile as the primary target ("Mobile (Default)" under Layout & Spacing).
    headlineLarge = base.headlineLarge.copy(
        fontFamily = Bold,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineMedium = base.headlineMedium.copy(
        fontFamily = Bold,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    headlineSmall = base.headlineSmall.copy(fontFamily = Regular),
    titleLarge = base.titleLarge.copy(fontFamily = Regular),
    titleMedium = base.titleMedium.copy(fontFamily = Regular),
    titleSmall = base.titleSmall.copy(fontFamily = Regular),
    bodyLarge = base.bodyLarge.copy(
        fontFamily = Medium,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 28.sp,
    ),
    bodyMedium = base.bodyMedium.copy(
        fontFamily = Regular,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodySmall = base.bodySmall.copy(fontFamily = Regular),
    labelLarge = base.labelLarge.copy(fontFamily = SemiBold),
    labelMedium = base.labelMedium.copy(
        fontFamily = SemiBold,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.01.em,
    ),
    labelSmall = base.labelSmall.copy(
        fontFamily = Bold,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.05.em,
    ),
)
