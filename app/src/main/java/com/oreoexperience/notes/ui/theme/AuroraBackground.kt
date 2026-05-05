package com.oreoexperience.notes.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Fondo "Aurora": pinta tres "blobs" radiales violetas suavemente animados
 * sobre la base [OreoPalette.Bg0], más una capa de partículas estelares
 * estáticas (sembradas con un seed fijo para que no parpadeen entre
 * recomposiciones).
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "auroraT",
    )
    // Segunda fase con período distinto para que el resultado no se sienta
    // periódico. La superposición de [t] y [t2] hace que las constelaciones
    // de blobs no se repitan en el mismo lugar.
    val t2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "auroraT2",
    )

    val stars = remember {
        val r = Random(0xCAFE0AC1L)
        List(80) {
            StarSpec(
                ux = r.nextFloat(),
                uy = r.nextFloat(),
                radiusDp = 0.6f + r.nextFloat() * 1.6f,
                alpha = 0.25f + r.nextFloat() * 0.55f,
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w: Float = size.width
        val h: Float = size.height

        // 1) Base
        drawRect(color = OreoPalette.Bg0)

        // 2) Tres blobs radiales animados. Convertimos cos/sin a Float
        //    explícitamente para evitar que Kotlin promueva a Double.
        val twoPi = (2.0 * PI).toFloat()
        val a = cos(t * twoPi).toFloat()
        val b = sin(t * twoPi * 0.7f).toFloat()
        val c = cos(t * twoPi * 0.4f).toFloat()
        val d = sin(t2 * twoPi * 0.55f).toFloat()
        val e = cos(t2 * twoPi * 0.9f).toFloat()

        drawBlob(
            center = Offset(w * (0.20f + 0.09f * a), h * (0.25f + 0.05f * e)),
            radius = w * 0.85f,
            color = OreoPalette.Mesh1,
            innerAlpha = 0.85f,
        )
        drawBlob(
            center = Offset(w * (0.85f + 0.08f * b), h * (0.65f + 0.06f * c)),
            radius = w * 0.95f,
            color = OreoPalette.Mesh2,
            innerAlpha = 0.7f,
        )
        drawBlob(
            center = Offset(w * (0.5f + 0.06f * e), h * (1.05f + 0.05f * d)),
            radius = w * 1.1f,
            color = OreoPalette.Mesh3,
            innerAlpha = 0.55f,
        )

        // 3) Estrellas
        val starPx = 1.dp.toPx()
        for (s in stars) {
            drawCircle(
                color = Color.White.copy(alpha = s.alpha),
                radius = s.radiusDp * starPx,
                center = Offset(s.ux * w, s.uy * h),
            )
        }

        // 4) Viñeta sutil para asentar el contenido
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                startY = h * 0.55f,
                endY = h,
            )
        )
    }
}

private fun DrawScope.drawBlob(
    center: Offset,
    radius: Float,
    color: Color,
    innerAlpha: Float,
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = innerAlpha),
                color.copy(alpha = innerAlpha * 0.5f),
                Color.Transparent,
            ),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

private data class StarSpec(
    val ux: Float,
    val uy: Float,
    val radiusDp: Float,
    val alpha: Float,
)
