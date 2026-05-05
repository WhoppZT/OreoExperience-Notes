package com.oreoexperience.notes.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
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

private val DarkScheme = darkColorScheme(
    primary = OreoPalette.Accent,
    onPrimary = Color(0xFF1A0B33),
    primaryContainer = OreoPalette.Mesh2,
    onPrimaryContainer = OreoPalette.OnSurface,

    secondary = OreoPalette.AccentSub,
    onSecondary = Color(0xFF1A0B33),

    tertiary = Color(0xFFD0B8FF),
    onTertiary = Color(0xFF1A0B33),

    background = OreoPalette.Bg0,
    onBackground = OreoPalette.OnSurface,

    surface = OreoPalette.Bg0,
    onSurface = OreoPalette.OnSurface,
    surfaceVariant = OreoPalette.Mesh1,
    onSurfaceVariant = OreoPalette.OnSurfaceMuted,

    outline = OreoPalette.Outline,
    outlineVariant = Color(0x22B68CFF),

    error = OreoPalette.DangerFill,
    onError = Color(0xFF330007),
)

/**
 * Forzamos siempre la paleta `starry` (oscura + violeta), independientemente
 * del setting del sistema. Esto refleja la decisión de diseño de la
 * Edición Aurora del desktop. Si en el futuro se quiere modo claro,
 * hay que añadir un [lightColorScheme] y cambiar este selector.
 */
@Composable
fun OreoExperienceTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = DarkScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    // Pintamos la selección de texto en violeta Aurora para que combine
    // con el resto del tema (los handles redondos también heredan estos
    // colores). El `backgroundColor` queda con alpha bajo para no tapar
    // el texto y el `handleColor` opaco para que sean visibles sobre
    // cualquier card del fondo.
    val auroraSelectionColors = TextSelectionColors(
        handleColor = OreoPalette.Accent,
        backgroundColor = OreoPalette.Accent.copy(alpha = 0.32f),
    )

    MaterialTheme(
        colorScheme = scheme,
        typography = OreoTypography,
    ) {
        CompositionLocalProvider(
            LocalTextSelectionColors provides auroraSelectionColors,
            content = content,
        )
    }
}
