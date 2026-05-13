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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oreoexperience.notes.ui.theme.OreoPalette

/**
 * Galletita Oreo dibujada con Canvas, lista para usar como indicador
 * de carga: dos tapas oscuras separadas, el relleno claro en el medio
 * y unos puntitos en las tapas. Gira sobre su eje continuamente.
 *
 * Usada por el indicador de pull-to-refresh.
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
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "oreoSpinAngle",
    )

    Canvas(modifier = modifier.size(size)) {
        rotate(angle) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val radius = this.size.width * 0.46f
            val cookieColor = OreoPalette.OnSurface.copy(alpha = 0.92f)
            val creamColor = OreoPalette.OnSurfaceMuted.copy(alpha = 0.55f)
            // Galleta de fondo (tapa)
            drawCircle(
                color = cookieColor,
                radius = radius,
                center = Offset(cx, cy),
            )
            // Crema central
            drawCircle(
                color = creamColor,
                radius = radius * 0.62f,
                center = Offset(cx, cy),
            )
            // Punto central crema
            drawCircle(
                color = cookieColor,
                radius = radius * 0.18f,
                center = Offset(cx, cy),
            )
            // 6 detalles de la galleta alrededor
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
