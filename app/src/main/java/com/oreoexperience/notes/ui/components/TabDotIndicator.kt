package com.oreoexperience.notes.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.oreoexperience.notes.ui.theme.OreoMotion
import com.oreoexperience.notes.ui.theme.OreoPalette

/**
 * Hilera de puntos que indica la cantidad de tabs (categorías) y cuál
 * está activa. El punto activo es más ancho y violeta sólido; los
 * inactivos son círculos chiquitos translúcidos. Pensado para
 * acompañar el gesto de swipe horizontal entre tabs.
 */
@Composable
fun TabDotIndicator(
    count: Int,
    activeIndex: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(count) { i ->
            val active = i == activeIndex
            val width by animateFloatAsState(
                targetValue = if (active) 20f else 6f,
                animationSpec = OreoMotion.SpringCard(),
                label = "dotW$i",
            )
            val color by animateColorAsState(
                targetValue = if (active) OreoPalette.Accent
                else OreoPalette.OnSurfaceFaint.copy(alpha = 0.40f),
                label = "dotC$i",
            )
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width.dp)
                    .background(color = color, shape = CircleShape),
            )
        }
    }
}
