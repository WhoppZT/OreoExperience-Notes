package com.oreoexperience.notes.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.oreoexperience.notes.ui.theme.OreoPalette

/**
 * Ilustración animada para el estado vacío: una galletita Oreo grande
 * con tres migajas chiquitas que caen por debajo en bucle suave. La
 * Oreo no rota; solo las migajas tienen movimiento, así no es
 * agresiva ni distrae.
 */
@Composable
fun EmptyStateIllustration(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "emptyCrumbs")
    val crumb1 by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_400, easing = LinearEasing, delayMillis = 0),
            repeatMode = RepeatMode.Restart,
        ),
        label = "crumb1",
    )
    val crumb2 by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_400, easing = LinearEasing, delayMillis = 800),
            repeatMode = RepeatMode.Restart,
        ),
        label = "crumb2",
    )
    val crumb3 by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_400, easing = LinearEasing, delayMillis = 1_600),
            repeatMode = RepeatMode.Restart,
        ),
        label = "crumb3",
    )

    Canvas(modifier = modifier.size(96.dp)) {
        val cx = size.width / 2f
        val cy = size.height * 0.42f
        val radius = size.width * 0.28f
        val cookieColor = OreoPalette.OnSurface.copy(alpha = 0.85f)
        val creamColor = OreoPalette.OnSurfaceMuted.copy(alpha = 0.55f)

        // Galleta principal
        drawCircle(
            color = cookieColor,
            radius = radius,
            center = Offset(cx, cy),
        )
        drawCircle(
            color = creamColor,
            radius = radius * 0.60f,
            center = Offset(cx, cy),
        )
        drawCircle(
            color = cookieColor,
            radius = radius * 0.16f,
            center = Offset(cx, cy),
        )
        // Detalles alrededor
        val detailRadius = radius * 0.075f
        val orbit = radius * 0.78f
        repeat(6) { i ->
            val theta = (i * 60.0 * Math.PI / 180.0).toFloat()
            val x = cx + kotlin.math.cos(theta) * orbit
            val y = cy + kotlin.math.sin(theta) * orbit
            drawCircle(
                color = Color.Black.copy(alpha = 0.45f),
                radius = detailRadius,
                center = Offset(x, y),
            )
        }

        // Migajas que caen
        val fallStart = cy + radius
        val fallEnd = size.height * 0.95f
        val fallDist = fallEnd - fallStart

        fun drawCrumb(progress: Float, dx: Float, baseSize: Float) {
            val alpha = (1f - progress).coerceIn(0f, 1f) * (progress.coerceAtMost(0.25f) / 0.25f)
            drawCircle(
                color = cookieColor.copy(alpha = alpha * 0.85f),
                radius = baseSize,
                center = Offset(cx + dx, fallStart + fallDist * progress),
            )
        }

        drawCrumb(crumb1, dx = -10.dp.toPx(), baseSize = 2.6.dp.toPx())
        drawCrumb(crumb2, dx = 6.dp.toPx(), baseSize = 2.1.dp.toPx())
        drawCrumb(crumb3, dx = -2.dp.toPx(), baseSize = 1.8.dp.toPx())
    }
}
