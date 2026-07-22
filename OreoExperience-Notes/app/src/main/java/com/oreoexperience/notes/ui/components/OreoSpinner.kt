package com.oreoexperience.notes.ui.components

import androidx.compose.animation.core.EaseInOut
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oreoexperience.notes.ui.theme.OreoPalette

/**
 * Galletita Oreo dibujada con Canvas, con animación expresiva:
 * velocidad variable (ease-in-out) y pulso mientras gira.
 */
@Composable
fun OreoSpinner(
    size: Dp = 36.dp,
    spinning: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val inf = rememberInfiniteTransition(label = "oreoSpin")
    val angle by inf.animateFloat(
        initialValue = 0f,
        targetValue = if (spinning) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseInOut),
            repeatMode = RepeatMode.Restart,
        ),
        label = "oreoSpinAngle",
    )
    val pulse by inf.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "oreoPulse",
    )

    Canvas(modifier = modifier.size(size)) {
        val scale = if (spinning) pulse else 1f
        rotate(angle) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val radius = this.size.width * 0.46f * scale
            val cookieColor = OreoPalette.OnSurface.copy(alpha = 0.92f)
            val creamColor = OreoPalette.OnSurfaceMuted.copy(alpha = 0.55f)
            drawCircle(
                color = cookieColor,
                radius = radius,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = creamColor,
                radius = radius * 0.62f,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = cookieColor,
                radius = radius * 0.18f,
                center = Offset(cx, cy),
            )
            val detailRadius = radius * 0.08f
            val orbit = radius * 0.78f
            repeat(6) { i ->
                val theta = (i * 60.0 * Math.PI / 180.0).toFloat()
                val x = cx + kotlin.math.cos(theta) * orbit
                val y = cy + kotlin.math.sin(theta) * orbit
                drawCircle(
                    color = Color.Black.copy(alpha = 0.55f),
                    radius = detailRadius,
                    center = Offset(x, y),
                )
            }
        }
    }
}
