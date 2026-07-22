package com.oreoexperience.notes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlin.math.cos
import kotlin.math.sin

/**
 * Icono "galleta Oreo" — usado en el editor de notas como activador del
 * menú desplegable (en reemplazo del clásico "···").
 *
 * Se dibuja vectorialmente sobre [Canvas] para que escale a cualquier
 * tamaño y respete el tint dinámico de la paleta Aurora (oscuro/claro).
 * Top-down view: un disco oscuro (la galleta) con un anillo de "crema"
 * en el centro y pequeños puntos decorativos alrededor que evocan el
 * relieve característico de la Oreo.
 */
@Composable
fun OreoCookieIcon(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    tint: Color = OreoPalette.Accent,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w / 2f, h / 2f)
        val outerRadius = (minOf(w, h) / 2f) * 0.94f
        val ringThickness = outerRadius * 0.28f

        // Anillo principal — la galleta.
        drawCircle(
            color = tint,
            radius = outerRadius - ringThickness / 2f,
            center = center,
            style = Stroke(width = ringThickness),
        )

        // Crema central.
        drawCircle(
            color = tint.copy(alpha = 0.55f),
            radius = outerRadius * 0.34f,
            center = center,
        )

        // Pequeños "relieves" decorativos: 8 puntos alrededor, simulando
        // el patrón embossed de la galleta real.
        val dotRadius = outerRadius * 0.07f
        val dotOrbit = outerRadius * 0.72f
        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45.0) + 22.5)
            val x = center.x + (dotOrbit * cos(angle)).toFloat()
            val y = center.y + (dotOrbit * sin(angle)).toFloat()
            drawCircle(
                color = tint.copy(alpha = 0.9f),
                radius = dotRadius,
                center = Offset(x, y),
            )
        }
    }
}
