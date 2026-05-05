package com.oreoexperience.notes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oreoexperience.notes.ui.theme.OreoPalette

/**
 * Card "glassmorphism" del tema Aurora. No usamos un blur real (caro en
 * Compose y con regresiones por API level); simulamos el efecto con una
 * capa semi-transparente sobre el fondo Aurora más un borde violeta tenue.
 * El resultado es muy parecido al estilo glass del desktop sin overhead.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    borderAlpha: Float = 0.45f,
    fill: Color = OreoPalette.GlassFill,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Surface(
        modifier = modifier,
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            fill,
                            fill.copy(alpha = (fill.alpha * 0.55f)),
                        )
                    ),
                    shape = shape,
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            OreoPalette.Accent.copy(alpha = borderAlpha),
                            OreoPalette.AccentSub.copy(alpha = borderAlpha * 0.4f),
                        )
                    ),
                    shape = shape,
                )
                .padding(0.dp),
        ) {
            content()
        }
    }
}
