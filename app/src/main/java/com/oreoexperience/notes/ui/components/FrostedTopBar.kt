package com.oreoexperience.notes.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.oreoexperience.notes.ui.theme.OreoPalette

/**
 * Topbar tipo *frosted glass* estilo iOS Notes.
 *
 *  - Cuando la pantalla está al tope (no scrolleada), el fondo es
 *    transparente — el contenido respira contra el fondo de la app.
 *  - Cuando el usuario scrollea, el topbar gana un fondo translúcido
 *    sobre el contenido + un degradado suave hacia abajo + una línea
 *    fina inferior. Esto crea la sensación de cristal sin obstruir.
 *  - En Android 12+ (API 31) se aplica además un blur real con
 *    [RenderEffect.createBlurEffect] sobre el contenido del propio
 *    topbar, lo que suaviza los bordes de los íconos contra cualquier
 *    backdrop que aparezca debajo (status bar coloreada, contenido,
 *    etc).
 *  - En versiones anteriores se usa solo la transparencia + la
 *    superposición vertical, que también queda elegante sin necesidad
 *    de blur nativo.
 *
 * Se usa como una *Row* normal: el contenido se compone dentro del
 * lambda [content], que recibe un [RowScope].
 *
 * Si [statusBarPadding] es true, se reserva además el espacio del
 * status bar (status bar transparente con el fondo del topbar pintado
 * por debajo). Usar `false` cuando el topbar vive dentro de otro
 * Scaffold que ya aplicó el inset.
 */
@Composable
fun FrostedTopBar(
    scrolled: Boolean,
    modifier: Modifier = Modifier,
    statusBarPadding: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    // Tinte del fondo cuando hay contenido detrás. Lo dejamos un poco
    // más opaco en oscuro para que el cristal "pegue" sobre fondos muy
    // negros.
    val solidBg = OreoPalette.Bg0
    val targetBg = if (scrolled) solidBg.copy(alpha = 0.72f) else Color.Transparent
    val targetBorder = if (scrolled) OreoPalette.OutlineFaint else Color.Transparent

    val bg by animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(durationMillis = 220),
        label = "frostedBg",
    )
    val border by animateColorAsState(
        targetValue = targetBorder,
        animationSpec = tween(durationMillis = 220),
        label = "frostedBorder",
    )

    // Blur real solo en Android 12+ (API 31). El blur se aplica sobre
    // el propio topbar y suaviza ligeramente sus bordes y el degradado
    // inferior, lo que vende mucho la idea de cristal.
    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && scrolled) {
        Modifier.graphicsLayer {
            renderEffect = android.graphics
                .RenderEffect
                .createBlurEffect(18f, 18f, android.graphics.Shader.TileMode.CLAMP)
                .asComposeRenderEffect()
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(blurModifier)
            .background(bg)
            .drawWithCache {
                // Degradado vertical muy sutil para que la transición
                // entre el topbar y el contenido se vea como cristal.
                val grad = Brush.verticalGradient(
                    colors = listOf(
                        bg,
                        bg.copy(alpha = bg.alpha * 0.35f),
                    ),
                    startY = 0f,
                    endY = size.height,
                )
                onDrawBehind {
                    drawRect(brush = grad, topLeft = Offset.Zero, size = size)
                }
            },
    ) {
        Column(
            modifier = if (statusBarPadding) Modifier.statusBarsPadding() else Modifier,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                content = content,
            )
            // Hairline inferior — sólo visible cuando el topbar está
            // "encendido" (scrolled). Anima el alpha en conjunto con
            // el fondo.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(border),
            )
        }
    }
}
