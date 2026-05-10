package com.oreoexperience.notes.ui.theme

import android.app.Activity
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Tema "iOS Notes Dark": fondo negro puro, texto blanco, acento
 * **violeta Aurora**. Forzamos siempre el esquema oscuro
 * independientemente del setting del sistema — la app no tiene tema
 * claro por decisión del diseño.
 */
private val IosDarkScheme = darkColorScheme(
    primary = OreoPalette.Accent,
    onPrimary = Color.White,
    primaryContainer = OreoPalette.SurfaceCardHi,
    onPrimaryContainer = OreoPalette.OnSurface,

    secondary = OreoPalette.Accent,
    onSecondary = Color.White,

    tertiary = OreoPalette.AccentSub,
    onTertiary = Color.White,

    background = OreoPalette.Bg0,
    onBackground = OreoPalette.OnSurface,

    surface = OreoPalette.Bg0,
    onSurface = OreoPalette.OnSurface,
    surfaceVariant = OreoPalette.SurfaceCard,
    onSurfaceVariant = OreoPalette.OnSurfaceMuted,

    outline = OreoPalette.Outline,
    outlineVariant = OreoPalette.OutlineFaint,

    error = OreoPalette.DangerFill,
    onError = Color.White,
)

@Composable
fun OreoExperienceTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar y nav bar transparentes; los iconos en blanco
            // sobre el fondo negro de la app.
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    // Selección de texto en violeta Aurora.
    val iosSelectionColors = TextSelectionColors(
        handleColor = OreoPalette.Accent,
        backgroundColor = OreoPalette.AccentSub.copy(alpha = 0.35f),
    )

    MaterialTheme(
        colorScheme = IosDarkScheme,
        typography = OreoTypography,
    ) {
        CompositionLocalProvider(
            LocalTextSelectionColors provides iosSelectionColors,
            content = content,
        )
    }
}
