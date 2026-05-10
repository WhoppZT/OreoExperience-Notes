package com.oreoexperience.notes.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.oreoexperience.notes.data.ThemeMode

/** Esquema Material3 derivado de la paleta oscura. */
private fun iosDarkScheme() = darkColorScheme(
    primary = DarkPalette.Accent,
    onPrimary = Color.White,
    primaryContainer = DarkPalette.SurfaceCardHi,
    onPrimaryContainer = DarkPalette.OnSurface,

    secondary = DarkPalette.Accent,
    onSecondary = Color.White,

    tertiary = DarkPalette.AccentSub,
    onTertiary = Color.White,

    background = DarkPalette.Bg0,
    onBackground = DarkPalette.OnSurface,

    surface = DarkPalette.Bg0,
    onSurface = DarkPalette.OnSurface,
    surfaceVariant = DarkPalette.SurfaceCard,
    onSurfaceVariant = DarkPalette.OnSurfaceMuted,

    outline = DarkPalette.Outline,
    outlineVariant = DarkPalette.OutlineFaint,

    error = DarkPalette.DangerFill,
    onError = Color.White,
)

/** Esquema Material3 derivado de la paleta clara. */
private fun iosLightScheme() = lightColorScheme(
    primary = LightPalette.Accent,
    onPrimary = Color.White,
    primaryContainer = LightPalette.SurfaceCardHi,
    onPrimaryContainer = LightPalette.OnSurface,

    secondary = LightPalette.Accent,
    onSecondary = Color.White,

    tertiary = LightPalette.AccentSub,
    onTertiary = Color.White,

    background = LightPalette.Bg0,
    onBackground = LightPalette.OnSurface,

    surface = LightPalette.Bg0,
    onSurface = LightPalette.OnSurface,
    surfaceVariant = LightPalette.SurfaceCard,
    onSurfaceVariant = LightPalette.OnSurfaceMuted,

    outline = LightPalette.Outline,
    outlineVariant = LightPalette.OutlineFaint,

    error = LightPalette.DangerFill,
    onError = Color.White,
)

/**
 * Tema "iOS Notes" con soporte light + dark + system. La paleta
 * activa se determina con [themeMode]; si es [ThemeMode.SYSTEM] se
 * sigue al sistema operativo en tiempo real.
 */
@Composable
fun OreoExperienceTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
    }

    // Sincroniza la paleta global usada por OreoPalette.X con el modo
    // resuelto. Es seguro hacerlo aquí porque el tema envuelve todo el
    // árbol y los hijos siempre recomponen al cambiar el modo.
    ActivePalette = if (isDark) DarkPalette else LightPalette

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar y nav bar transparentes; los iconos se invierten
            // según el fondo (oscuros sobre claro, claros sobre oscuro).
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    val palette = if (isDark) DarkPalette else LightPalette
    val selection = TextSelectionColors(
        handleColor = palette.Accent,
        backgroundColor = palette.AccentSub.copy(alpha = 0.35f),
    )

    MaterialTheme(
        colorScheme = if (isDark) iosDarkScheme() else iosLightScheme(),
        typography = OreoTypography,
    ) {
        CompositionLocalProvider(
            LocalTextSelectionColors provides selection,
            content = content,
        )
    }
}
