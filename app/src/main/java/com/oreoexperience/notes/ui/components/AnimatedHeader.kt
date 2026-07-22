package com.oreoexperience.notes.ui.components

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.R
import com.oreoexperience.notes.ui.theme.OreoMotion
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlin.math.cos
import kotlin.math.sin

private data class DripData(
    val x: Float,
    val speed: Float,
    val maxWidth: Float,
    val color: Color,
    val delay: Float,
)

/**
 * Encabezado con gradiente animado y partículas flotantes.
 */
@Composable
fun AnimatedGradientHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onIconClick: (() -> Unit)? = null,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "headerGradient")
    
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )
    
    val starRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starRotation"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // Canvas de fondo que cubre todo el header
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val width = size.width
            val height = size.height
            
            // Color drip effect
            val drips = listOf(
                DripData(0.08f, 1.2f, 12f, OreoPalette.Accent, 0f),
                DripData(0.18f, 0.9f, 16f, OreoPalette.AccentSub, 0.3f),
                DripData(0.32f, 1.5f, 10f, OreoPalette.AccentLight, 0.1f),
                DripData(0.45f, 0.8f, 18f, OreoPalette.Accent, 0.5f),
                DripData(0.58f, 1.3f, 14f, OreoPalette.AccentSub, 0.2f),
                DripData(0.72f, 1.0f, 11f, OreoPalette.AccentSub, 0.4f),
                DripData(0.85f, 1.4f, 15f, OreoPalette.Accent, 0.15f),
                DripData(0.95f, 0.7f, 13f, OreoPalette.AccentLight, 0.35f),
            )
            
            drips.forEach { drip ->
                val cycleProgress = ((gradientOffset * drip.speed + drip.delay) % 1.2f)
                val dripHeight = cycleProgress * height * 1.5f
                val dripX = width * drip.x
                
                if (dripHeight > 0f) {
                    // Cuerpo del drip
                    drawRect(
                        color = drip.color.copy(alpha = 0.3f),
                        topLeft = Offset(dripX - drip.maxWidth / 2f, 0f),
                        size = Size(drip.maxWidth, dripHeight.coerceAtMost(height)),
                    )
                    // Punta redondeada del drip
                    if (dripHeight < height) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    drip.color.copy(alpha = 0.4f),
                                    drip.color.copy(alpha = 0.15f),
                                    Color.Transparent,
                                ),
                                center = Offset(dripX, dripHeight),
                                radius = drip.maxWidth * 1.2f,
                            ),
                            radius = drip.maxWidth * 1.2f,
                            center = Offset(dripX, dripHeight),
                        )
                    }
                }
            }
            
            // Aurora bands encima de los drips
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        OreoPalette.Accent.copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                    start = Offset(gradientOffset * 600f - 300f, 0f),
                    end = Offset(gradientOffset * 600f + 200f, height),
                )
            )
            
            // Partículas (reducidas de 15 a 8 para mejor rendimiento)
            val particles = listOf(
                Triple(0.1f, 0.25f, 2.5f),
                Triple(0.3f, 0.7f, 2f),
                Triple(0.5f, 0.15f, 2.8f),
                Triple(0.7f, 0.6f, 2f),
                Triple(0.9f, 0.35f, 2.5f),
                Triple(0.2f, 0.85f, 1.8f),
                Triple(0.6f, 0.45f, 2.2f),
                Triple(0.8f, 0.9f, 1.5f),
            )
            
            particles.forEachIndexed { index, (x, y, radius) ->
                val spiralAngle = gradientOffset * 4f + index * 0.8f
                val spiralRadius = 15f + index * 2f
                val offsetX = cos(spiralAngle) * spiralRadius
                val offsetY = sin(spiralAngle) * spiralRadius
                
                val particleX = width * x + offsetX
                val particleY = height * y + offsetY
                
                val alpha = 0.3f + 0.45f * sin(gradientOffset * 4f + index)
                
                drawCircle(
                    color = OreoPalette.Accent.copy(alpha = alpha),
                    radius = radius.dp.toPx(),
                    center = Offset(particleX, particleY)
                )
            }
        }
        
        // Contenido con padding
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 16.dp)
        ) {
            if (onIconClick != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(OreoPalette.Accent.copy(alpha = 0.15f))
                        .clickable(onClick = onIconClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_oreo_cookie),
                        contentDescription = "Oreo Logo",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.size(16.dp))
            }
            
            Column(horizontalAlignment = Alignment.Start) {
                var titleWidthPx by remember { mutableIntStateOf(0) }
                var titleHeightPx by remember { mutableIntStateOf(0) }
                val density = LocalDensity.current
                Box {
                    // Título principal
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            onTextLayout = {
                                titleWidthPx = it.size.width
                                titleHeightPx = it.size.height
                            },
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
                                fontFamily = FontFamily(Font(R.font.misans_bold)),
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                letterSpacing = (-0.5).sp,
                            ),
                        )
                    }
                    if (titleWidthPx > 0) {
                        Canvas(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = with(density) { titleWidthPx + 16.dp.roundToPx() },
                                        y = with(density) { (titleHeightPx * 0.2f).toInt() },
                                    )
                                }
                                .size(
                                    width = with(density) { (titleHeightPx * 2.5f).toDp() },
                                    height = with(density) { titleHeightPx.toDp() },
                                )
                        ) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val scale = size.height / 65f
                        val starPurple = OreoPalette.AccentGlow
                        val starLight = OreoPalette.StarLight
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(starPurple.copy(alpha = 0.15f), Color.Transparent),
                                center = Offset(cx, cy),
                                radius = size.height * 0.95f,
                            ),
                            radius = size.height * 0.95f,
                            center = Offset(cx, cy),
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(starPurple.copy(alpha = 0.35f), Color.Transparent),
                                center = Offset(cx, cy),
                                radius = size.height * 0.55f,
                            ),
                            radius = size.height * 0.55f,
                            center = Offset(cx, cy),
                        )
                        val outerR = 30f * scale; val innerR = 13f * scale
                        drawPath(
                            path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(cx, cy - outerR)
                                for (i in 0 until 5) {
                                    val oa = Math.toRadians((i * 72.0 - 90.0))
                                    lineTo(cx + (outerR * kotlin.math.cos(oa)).toFloat(), cy + (outerR * kotlin.math.sin(oa)).toFloat())
                                    val ia = Math.toRadians((i * 72.0 + 36.0 - 90.0))
                                    lineTo(cx + (innerR * kotlin.math.cos(ia)).toFloat(), cy + (innerR * kotlin.math.sin(ia)).toFloat())
                                }
                                close()
                            },
                            brush = Brush.radialGradient(
                                colors = listOf(starLight, starPurple),
                                center = Offset(cx, cy),
                                radius = outerR,
                            ),
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.7f), Color.Transparent),
                                center = Offset(cx, cy),
                                radius = outerR * 0.35f,
                            ),
                            radius = outerR * 0.35f,
                            center = Offset(cx, cy),
                        )
                        val dotX = cx + outerR * 1.1f; val dotY = cy - outerR * 0.7f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(starPurple.copy(alpha = 0.3f), Color.Transparent),
                                center = Offset(dotX, dotY),
                                radius = 8f * scale,
                            ),
                            radius = 8f * scale,
                            center = Offset(dotX, dotY),
                        )
                        drawCircle(OreoPalette.StarDot, 3.2f * scale, Offset(dotX, dotY))
                    }
                }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(2.5.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        OreoPalette.Accent,
                                        OreoPalette.AccentSub.copy(alpha = 0.25f),
                                    ),
                                ),
                                RoundedCornerShape(1.5.dp),
                            ),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = subtitle,
                        color = OreoPalette.OnSurface.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        }
    }
}

/**
 * Botón flotante con lápiz inclinado y animaciones sutiles.
 */
@Composable
fun MorphingFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = OreoMotion.SpringPress(),
        label = "fabScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "fabGlow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = OreoMotion.EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fabGlowScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = OreoMotion.EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fabGlowAlpha"
    )

    // Lápiz: colores
    val eraserColor = Color(0xFFF472B6)
    val bandColor = Color(0xFFD1D5DB)
    val bodyTop = Color(0xFFC4A8FF)
    val bodyBottom = Color(0xFFA78BFA)
    val accentBand = Color(0xFF7C3AED)
    val woodColor = Color(0xFFF5E6D3)
    val tipColor = Color(0xFF2D2A3E)

    Box(
        modifier = modifier
            .padding(bottom = 0.dp)
            .size(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Glow sutil
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(glowScale)
                .alpha(glowAlpha)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            OreoPalette.Accent.copy(alpha = 0.3f),
                            OreoPalette.AccentSub.copy(alpha = 0.1f),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
        )

        // Contenedor del lápiz
        Box(
            modifier = Modifier
                .size(64.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        listOf(OreoPalette.Accent, OreoPalette.AccentSub)
                    )
                )
                .pointerInput(Unit) {
                    detectTapGestures(onPress = { onClick() })
                },
            contentAlignment = Alignment.Center,
        ) {
            // Lápiz inclinado +30° (sentido contrario)
            Canvas(
                modifier = Modifier
                    .size(36.dp, 56.dp)
                    .graphicsLayer {
                        rotationZ = 30f
                    }
            ) {
            val w = size.width
            val h = size.height
            val pencilW = w * 0.5f
            val pencilX = (w - pencilW) / 2f

            // Goma
            val eraserH = h * 0.15f
            drawRect(eraserColor, Offset(pencilX, 0f), Size(pencilW, eraserH))

            // Anillo
            val bandH = h * 0.05f
            drawRect(bandColor, Offset(pencilX, eraserH), Size(pencilW, bandH))

            // Cuerpo
            val bodyH = h * 0.48f
            val bodyY = eraserH + bandH
            drawRect(bodyTop, Offset(pencilX, bodyY), Size(pencilW, bodyH))
            drawRect(bodyBottom.copy(alpha = 0.25f), Offset(pencilX, bodyY + bodyH * 0.6f), Size(pencilW, bodyH * 0.4f))

            // Banda accent
            val accentH = h * 0.05f
            drawRect(accentBand, Offset(pencilX, bodyY + bodyH), Size(pencilW, accentH))

            // Punta
            val tipY = bodyY + bodyH + accentH
            val tipH = h - tipY
            drawPath(
                androidx.compose.ui.graphics.Path().apply {
                    moveTo(pencilX, tipY)
                    lineTo(pencilX + pencilW, tipY)
                    lineTo(pencilX + pencilW / 2f, tipY + tipH)
                    close()
                },
                woodColor,
            )
            // Grafito
            val graphitH = tipH * 0.35f
            drawPath(
                androidx.compose.ui.graphics.Path().apply {
                    val gTop = tipY + tipH - graphitH
                    val gW = pencilW * (graphitH / tipH) / 2f
                    moveTo(pencilX + pencilW / 2f - gW, gTop)
                    lineTo(pencilX + pencilW / 2f + gW, gTop)
                    lineTo(pencilX + pencilW / 2f, tipY + tipH)
                    close()
                },
                tipColor,
            )
        }
        }
    }
}

// ─── Edge Panel: pestaña inferior-derecha con lápiz ───────────
@Composable
fun EdgePanel(
    onNewNote: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    // Pencil colors
    val eraserColor = Color(0xFFF472B6)
    val bandColor = Color(0xFFD1D5DB)
    val bodyTop = Color(0xFFC4A8FF)
    val bodyBottom = Color(0xFFA78BFA)
    val accentBand = Color(0xFF7C3AED)
    val woodColor = Color(0xFFF5E6D3)
    val tipColor = Color(0xFF2D2A3E)

    val fabInteraction = remember { MutableInteractionSource() }
    val fabPressed by fabInteraction.collectIsPressedAsState()
    val fabScale by animateFloatAsState(
        targetValue = if (fabPressed) 0.92f else 1f,
        animationSpec = OreoMotion.SpringPress(),
        label = "edgeFabScale",
    )

    // Scrim animation
    val scrimAlpha by animateFloatAsState(
        targetValue = if (expanded) 0.6f else 0f,
        animationSpec = tween(250),
        label = "scrimAlpha",
    )
    // Card scale-in
    val cardScale by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.8f,
        animationSpec = OreoMotion.SpringCard(),
        label = "edgeCardScale",
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(200),
        label = "edgeCardAlpha",
    )

    // Collapsed: small pencil tab at bottom-end
    if (!expanded) {
        Box(
            modifier = modifier
                .size(width = 28.dp, height = 56.dp)
                .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                .background(OreoPalette.Accent.copy(alpha = 0.2f))
                .border(
                    0.5.dp,
                    OreoPalette.Accent.copy(alpha = 0.2f),
                    RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp),
                )
                .clickable { expanded = true },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "\u2039",
                color = OreoPalette.AccentSub,
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
            )
        }
    }

    // Expanded: full-screen scrim + centered card
    if (expanded || scrimAlpha > 0.01f) {
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = scrimAlpha }
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { expanded = false },
        )

        // Card with pencil + close
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = cardAlpha
                    scaleX = cardScale
                    scaleY = cardScale
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Close button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(OreoPalette.SurfaceCardHi.copy(alpha = 0.6f))
                        .clickable { expanded = false },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "\u2715",
                        color = OreoPalette.OnSurfaceMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                // Pencil FAB
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(fabScale)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(
                                    OreoPalette.Accent,
                                    OreoPalette.AccentSub,
                                )
                            )
                        )
                        .clickable(
                            interactionSource = fabInteraction,
                            indication = null,
                        ) {
                            onNewNote()
                            expanded = false
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(40.dp, 64.dp)
                            .graphicsLayer { rotationZ = 30f }
                    ) {
                        val w = size.width
                        val h = size.height
                        val pencilW = w * 0.5f
                        val pencilX = (w - pencilW) / 2f

                        val eraserH = h * 0.15f
                        drawRect(eraserColor, Offset(pencilX, 0f), Size(pencilW, eraserH))

                        val bandH = h * 0.05f
                        drawRect(bandColor, Offset(pencilX, eraserH), Size(pencilW, bandH))

                        val bodyH = h * 0.48f
                        val bodyY = eraserH + bandH
                        drawRect(bodyTop, Offset(pencilX, bodyY), Size(pencilW, bodyH))
                        drawRect(
                            bodyBottom.copy(alpha = 0.25f),
                            Offset(pencilX, bodyY + bodyH * 0.6f),
                            Size(pencilW, bodyH * 0.4f),
                        )

                        val accentH = h * 0.05f
                        drawRect(accentBand, Offset(pencilX, bodyY + bodyH), Size(pencilW, accentH))

                        val tipY = bodyY + bodyH + accentH
                        val tipH = h - tipY
                        drawPath(
                            androidx.compose.ui.graphics.Path().apply {
                                moveTo(pencilX, tipY)
                                lineTo(pencilX + pencilW, tipY)
                                lineTo(pencilX + pencilW / 2f, tipY + tipH)
                                close()
                            },
                            woodColor,
                        )
                        val graphitH = tipH * 0.35f
                        drawPath(
                            androidx.compose.ui.graphics.Path().apply {
                                val gTop = tipY + tipH - graphitH
                                val gW = pencilW * (graphitH / tipH) / 2f
                                moveTo(pencilX + pencilW / 2f - gW, gTop)
                                lineTo(pencilX + pencilW / 2f + gW, gTop)
                                lineTo(pencilX + pencilW / 2f, tipY + tipH)
                                close()
                            },
                            tipColor,
                        )
                    }
                }

                // Label
                Text(
                    "Nueva nota",
                    color = OreoPalette.OnSurfaceMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
