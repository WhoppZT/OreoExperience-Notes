package com.oreoexperience.notes.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.R

/**
 * Tipografía del tema. Usamos **MiSans** empaquetada en `res/font`
 * para garantizar consistencia visual en todos los dispositivos.
 *
 * Jerarquía de pesos:
 *   - Ligeros (Thin → Light): MiSans Regular — elegant, display use
 *   - Normales (Normal → Medium): MiSans Regular — body text, UI
 *   - SemiBold: MiSans Demibold — títulos, labels
 *   - Bold+ (Bold → Black): MiSans Heavy — impacto visual, hero text
 */
private val MiSansFontFamily = FontFamily(
    // Pesos ligeros: Regular para look más limpio en display
    Font(R.font.misans_regular, FontWeight.Thin),
    Font(R.font.misans_regular, FontWeight.ExtraLight),
    Font(R.font.misans_regular, FontWeight.Light),
    // Pesos normales: Regular para body text
    Font(R.font.misans_regular, FontWeight.Normal),
    Font(R.font.misans_regular, FontWeight.Medium),
    // SemiBold: Demibold para títulos y labels
    Font(R.font.misans_demibold, FontWeight.SemiBold),
    // Pesos fuertes: Heavy para impacto visual
    Font(R.font.misans_heavy, FontWeight.Bold),
    Font(R.font.misans_heavy, FontWeight.ExtraBold),
    Font(R.font.misans_heavy, FontWeight.Black),
)

val OreoTypography = Typography(
    // Display: pesos ligeros ahora usan Regular font (más limpio)
    displayLarge = TextStyle(fontFamily = MiSansFontFamily, fontWeight = FontWeight.Light,    fontSize = 57.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = MiSansFontFamily, fontWeight = FontWeight.Light,   fontSize = 45.sp),
    displaySmall = TextStyle(fontFamily = MiSansFontFamily, fontWeight = FontWeight.Normal,   fontSize = 36.sp),

    // Headline: SemiBold para jerarquía clara
    headlineLarge = TextStyle(fontFamily = MiSansFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = MiSansFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = MiSansFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),

    // Title: SemiBold para títulos de sección
    titleLarge = TextStyle(fontFamily = MiSansFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = MiSansFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = MiSansFontFamily, fontWeight = FontWeight.Medium,    fontSize = 14.sp, letterSpacing = 0.1.sp),

    // Body: Regular para lectura cómoda
    bodyLarge = TextStyle(fontFamily = MiSansFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = MiSansFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = MiSansFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.4.sp),

    // Label: Medium para UI elements
    labelLarge = TextStyle(fontFamily = MiSansFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = MiSansFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = MiSansFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.5.sp),
)
