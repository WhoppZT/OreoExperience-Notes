package com.oreoexperience.notes.ui.components

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SettingsGradientHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "settingsHeader")

    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )

    val iconRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = EaseInOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "iconRotation"
    )

    val iconPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconPulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 16.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.2f)
        ) {
            val width = size.width
            val height = size.height

            val particles = listOf(
                Triple(0.15f, 0.3f, 1.5f),
                Triple(0.4f, 0.7f, 1f),
                Triple(0.6f, 0.25f, 1.8f),
                Triple(0.8f, 0.65f, 1.2f),
                Triple(0.25f, 0.5f, 1.4f),
            )

            particles.forEachIndexed { index, (x, y, radius) ->
                val offsetX = cos(gradientOffset * 2f + index) * 12f
                val offsetY = sin(gradientOffset * 1.5f + index * 0.7f) * 10f

                val particleX = width * x + offsetX
                val particleY = height * y + offsetY

                val alpha = 0.2f + 0.12f * sin(gradientOffset * 3f + index)

                drawCircle(
                    color = OreoPalette.Accent.copy(alpha = alpha),
                    radius = radius.dp.toPx(),
                    center = Offset(particleX, particleY)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                OreoPalette.Accent.copy(alpha = 0.22f),
                                OreoPalette.Accent.copy(alpha = 0.06f),
                            ),
                        ),
                    )
                    .scale(iconPulse),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Ajustes",
                    tint = OreoPalette.Accent,
                    modifier = Modifier
                        .size(26.dp)
                        .rotate(iconRotation),
                )
            }
            Spacer(Modifier.size(14.dp))

            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = title,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                OreoPalette.AccentSub,
                                OreoPalette.Accent,
                                OreoPalette.AccentLight,
                                OreoPalette.AccentSub,
                            ),
                            start = Offset(gradientOffset * 200f, 0f),
                            end = Offset(gradientOffset * 200f + 300f, 0f),
                        ),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                    ),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(2.5.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        OreoPalette.Accent,
                                        OreoPalette.AccentSub.copy(alpha = 0.2f),
                                    ),
                                ),
                                RoundedCornerShape(1.5.dp),
                            ),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = subtitle,
                        color = OreoPalette.OnSurface.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.3.sp,
                    )
                }
            }
        }
    }
}
