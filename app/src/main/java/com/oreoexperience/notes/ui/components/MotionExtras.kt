package com.oreoexperience.notes.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.oreoexperience.notes.ui.theme.OreoMotion
import com.oreoexperience.notes.ui.theme.OreoPalette

/**
 * Modifier que hace entrar el contenido con un pequeño fade +
 * desplazamiento vertical, retrasado según el índice del elemento en la
 * lista. Pensado para listas que renderizan items de a uno — cada fila
 * aparece un poco después de la anterior, generando una cascada suave.
 *
 * El offset Y se aplica como translation visual (graphicsLayer) sin
 * reservar espacio, así que LazyColumn / Column siguen funcionando bien.
 */
fun Modifier.staggeredEntry(
    index: Int,
    triggerKey: Any? = null,
    perItemDelayMs: Int = 45,
): Modifier = composed {
    val progress = remember(index, triggerKey) { Animatable(0f) }
    LaunchedEffect(index, triggerKey) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 380,
                delayMillis = index.coerceAtMost(20) * perItemDelayMs,
                easing = EaseOutCubic,
            ),
        )
    }
    val p = progress.value
    val translationYpx = (1f - p) * with(LocalDensity.current) { 14.dp.toPx() }
    val scaleFactor = 0.96f + 0.04f * p
    this.graphicsLayer {
        alpha = p
        translationY = translationYpx
        scaleX = scaleFactor
        scaleY = scaleFactor
    }
}

/**
 * Halo pulsante violeta detrás de un elemento. Pensado para diferenciar
 * notas fijadas sin recargar la lista — el ciclo es lento (3s) y la
 * opacidad baja (15%) para que solo se note de reojo.
 */
fun Modifier.pinnedGlow(
    color: Color = OreoPalette.Accent,
): Modifier = composed {
    val inf = rememberInfiniteTransition(label = "pinnedGlow")
    val alphaFactor by inf.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(3_000, easing = OreoMotion.EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pinnedGlowAlpha",
    )
    this.drawBehind {
        val pad = 6.dp.toPx()
        val cornerPx = 22.dp.toPx()
        drawRoundRect(
            color = color.copy(alpha = alphaFactor),
            topLeft = Offset(-pad, -pad),
            size = Size(size.width + pad * 2, size.height + pad * 2),
            cornerRadius = CornerRadius(cornerPx, cornerPx),
        )
    }
}
