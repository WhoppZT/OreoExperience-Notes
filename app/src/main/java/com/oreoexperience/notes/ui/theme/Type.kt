package com.oreoexperience.notes.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Tipografía del tema. Por simplicidad usamos la familia Sans-serif del
 * sistema (en Android moderno suele resolverse a Roboto / Roboto Flex,
 * que tiene un look limpio similar al "Inter" usado en el desktop). Si
 * más adelante queremos bundlear Inter como `res/font`, sólo hay que
 * cambiar [DefaultFamily] sin tocar nada más.
 */
private val DefaultFamily = FontFamily.SansSerif

val OreoTypography = Typography(
    displayLarge = TextStyle(fontFamily = DefaultFamily, fontWeight = FontWeight.Light,    fontSize = 57.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = DefaultFamily, fontWeight = FontWeight.Light,   fontSize = 45.sp),
    displaySmall = TextStyle(fontFamily = DefaultFamily, fontWeight = FontWeight.Normal,   fontSize = 36.sp),

    headlineLarge = TextStyle(fontFamily = DefaultFamily, fontWeight = FontWeight.SemiBold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = DefaultFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = DefaultFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),

    titleLarge = TextStyle(fontFamily = DefaultFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = DefaultFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = DefaultFamily, fontWeight = FontWeight.Medium,    fontSize = 14.sp, letterSpacing = 0.1.sp),

    bodyLarge = TextStyle(fontFamily = DefaultFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = DefaultFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = DefaultFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.4.sp),

    labelLarge = TextStyle(fontFamily = DefaultFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = DefaultFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = DefaultFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.5.sp),
)
