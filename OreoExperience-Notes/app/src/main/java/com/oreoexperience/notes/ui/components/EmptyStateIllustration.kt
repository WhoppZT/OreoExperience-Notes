package com.oreoexperience.notes.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.oreoexperience.notes.R
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlin.math.sin

@Composable
fun EmptyStateIllustration(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "emptyCrumbs")
    val crumb1 by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_000, easing = LinearEasing, delayMillis = 0),
            repeatMode = RepeatMode.Restart,
        ),
        label = "crumb1",
    )
    val crumb2 by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_000, easing = LinearEasing, delayMillis = 600),
            repeatMode = RepeatMode.Restart,
        ),
        label = "crumb2",
    )
    val crumb3 by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_000, easing = LinearEasing, delayMillis = 1_200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "crumb3",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_empty_cafeteria),
            contentDescription = "Estado vacío",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(96.dp),
        )
        Canvas(
            modifier = Modifier
                .width(48.dp)
                .height(24.dp),
        ) {
            val cx = size.width / 2f
            val fallEnd = size.height
            val crumbColor = OreoPalette.OnSurface.copy(alpha = 0.85f)

            fun drawCrumb(progress: Float, dx: Float, baseSize: Float) {
                val alpha = (1f - progress).coerceIn(0f, 1f) * (progress.coerceAtMost(0.25f) / 0.25f)
                val bounceY = if (progress > 0.85f) {
                    fallEnd * 0.85f + sin((progress - 0.85f) * 20f) * 4.dp.toPx() * (1f - progress)
                } else {
                    fallEnd * progress
                }
                drawCircle(
                    color = crumbColor.copy(alpha = alpha * 0.85f),
                    radius = baseSize,
                    center = Offset(cx + dx, bounceY),
                )
            }

            drawCrumb(crumb1, dx = -8.dp.toPx(), baseSize = 2.4.dp.toPx())
            drawCrumb(crumb2, dx = 5.dp.toPx(), baseSize = 2.dp.toPx())
            drawCrumb(crumb3, dx = -1.dp.toPx(), baseSize = 1.6.dp.toPx())
        }
    }
}
