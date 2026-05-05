package com.oreoexperience.notes.ui.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.BuildConfig
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val SPLASH_DURATION_MS = 2200L
private const val FADE_OUT_MS = 540

/**
 * Pantalla de carga con estética Aurora: fondo nocturno oscuro, estrellas
 * titilantes, halo violeta pulsante, logo circular "O" con anillo orbital
 * giratorio, título "OreoExperience" + subtítulo "NOTAS", barra de progreso
 * animada.
 *
 * Dura ~2.2 s, hace fade-out de 540 ms, y emite [onFinished] al terminar.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(true) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(FADE_OUT_MS, easing = FastOutSlowInEasing),
        label = "splashAlpha",
        finishedListener = { v -> if (v == 0f) onFinished() },
    )

    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MS)
        visible = false
    }

    if (alpha < 0.01f) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0820),
                        Color(0xFF0F0828),
                        Color(0xFF1B0A3A),
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Estrellas titilantes
        TwinklingStars()

        // Halo violeta pulsante detrás del logo
        PulsingHalo(modifier = Modifier.offset(y = (-28).dp))

        // Contenido central
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LogoWithRing()
            Spacer(Modifier.height(18.dp))
            AppTitle()
            AccentSubtitle()
            Spacer(Modifier.height(4.dp))
            LoadingLabel()
            Spacer(Modifier.height(6.dp))
            ProgressBar()
        }

        // Firma discreta abajo: crédito + versión.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp),
        ) {
            Text(
                text = "Creado por Elihu Rueda",
                color = OreoPalette.Accent.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                color = Color.White.copy(alpha = 0.30f),
                fontSize = 11.sp,
                letterSpacing = 1.sp,
            )
        }
    }
}

/* ──────────────────────────── Estrellas ─────────────────────────── */

@Composable
private fun TwinklingStars() {
    val inf = rememberInfiniteTransition(label = "stars")
    val phase by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "starPhase",
    )
    val stars = remember {
        val rng = Random(0xDEADBEEFL)
        List(45) {
            StarData(
                ux = rng.nextFloat(),
                uy = rng.nextFloat(),
                r = 0.5f + rng.nextFloat() * 1.8f,
                speed = 0.4f + rng.nextFloat() * 1.2f,
                offset = rng.nextFloat(),
            )
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val twoPi = (2.0 * PI).toFloat()
        for (s in stars) {
            val a = ((sin((phase * s.speed + s.offset) * twoPi).toFloat() + 1f) / 2f)
                .coerceIn(0.15f, 0.9f)
            drawCircle(
                color = Color.White.copy(alpha = a),
                radius = s.r.dp.toPx(),
                center = Offset(s.ux * size.width, s.uy * size.height),
            )
        }
    }
}

private data class StarData(
    val ux: Float, val uy: Float, val r: Float,
    val speed: Float, val offset: Float,
)

/* ───────────────────────────── Halo ─────────────────────────────── */

@Composable
private fun PulsingHalo(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "halo")
    val haloScale by inf.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "haloScale",
    )
    Canvas(modifier = modifier.size(260.dp).scale(haloScale)) {
        val center = this.center
        drawCircle(
            color = Color(0x2E8C66F3),
            radius = size.width / 2f,
            center = center,
        )
        drawCircle(
            color = Color(0x24C78DFA),
            radius = size.width * 0.36f,
            center = center,
        )
        drawCircle(
            color = Color(0x1AF2D9FF),
            radius = size.width * 0.22f,
            center = center,
        )
    }
}

/* ──────────────────────────── Logo ──────────────────────────────── */

@Composable
private fun LogoWithRing() {
    // Escala con bounce al aparecer
    var targetScale by remember { mutableFloatStateOf(0.5f) }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "logoScale",
    )
    // Opacidad
    var targetAlpha by remember { mutableFloatStateOf(0f) }
    val logoAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "logoAlpha",
    )
    LaunchedEffect(Unit) {
        delay(100)
        targetScale = 1f
        targetAlpha = 1f
    }

    // Rotación del anillo orbital
    val inf = rememberInfiniteTransition(label = "ring")
    val ringRotation by inf.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_400, easing = LinearEasing),
        ),
        label = "ringRotation",
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(scale)
            .alpha(logoAlpha),
        contentAlignment = Alignment.Center,
    ) {
        // Anillo exterior orbital
        Box(
            modifier = Modifier
                .size(148.dp)
                .rotate(ringRotation)
                .drawBehind {
                    val ringRadius = size.width / 2f
                    drawCircle(
                        color = Color(0x73C78DFA),
                        radius = ringRadius,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                    val dotAngle = 0f // el ring rota; el dot queda arriba y gira con él
                    val dotX = center.x + ringRadius * cos(dotAngle)
                    val dotY = center.y + ringRadius * sin(dotAngle)
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(dotX, dotY),
                    )
                },
        ) { /* solo drawBehind */ }

        // Disco del logo
        Canvas(Modifier.size(120.dp)) {
            drawCircle(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFA78BFA), Color(0xFF5B21B6)),
                ),
                radius = size.width / 2f,
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.18f),
                radius = size.width / 2f,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
        // "O" centrada
        Text(
            text = "O",
            color = Color.White,
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/* ─────────────────────── Textos ─────────────────────────────────── */

@Composable
private fun AppTitle() {
    var alpha by remember { mutableFloatStateOf(0f) }
    val animAlpha by animateFloatAsState(alpha, tween(600), label = "titleAlpha")
    LaunchedEffect(Unit) { delay(200); alpha = 1f }
    Text(
        text = "OreoExperience",
        color = Color.White.copy(alpha = animAlpha),
        fontSize = 32.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = 1.sp,
    )
}

@Composable
private fun AccentSubtitle() {
    var alpha by remember { mutableFloatStateOf(0f) }
    val animAlpha by animateFloatAsState(alpha, tween(700), label = "accentAlpha")
    LaunchedEffect(Unit) { delay(350); alpha = 0.95f }
    Text(
        text = "NOTAS",
        color = OreoPalette.Accent.copy(alpha = animAlpha),
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 8.sp,
    )
}

@Composable
private fun LoadingLabel() {
    var alpha by remember { mutableFloatStateOf(0f) }
    val animAlpha by animateFloatAsState(alpha, tween(800), label = "loadAlpha")
    LaunchedEffect(Unit) { delay(500); alpha = 1f }
    Text(
        text = "Cargando experiencia…",
        color = Color.White.copy(alpha = animAlpha * 0.65f),
        fontSize = 13.sp,
    )
}

/* ──────────────── Barra de progreso ──────────────────────────────── */

@Composable
private fun ProgressBar() {
    var progress by remember { mutableFloatStateOf(0f) }
    val animProg by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = (SPLASH_DURATION_MS - 300).toInt(),
            easing = FastOutSlowInEasing,
        ),
        label = "splashProgress",
    )
    LaunchedEffect(Unit) { delay(200); progress = 1f }

    val barW = 240.dp
    val barH = 4.dp
    Canvas(
        modifier = Modifier
            .width(barW)
            .height(barH),
    ) {
        // Track
        drawRoundRect(
            color = Color.White.copy(alpha = 0.10f),
            size = size,
            cornerRadius = CornerRadius(size.height / 2),
        )
        // Fill
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF7C3AED), Color(0xFFC4B5FD)),
            ),
            size = Size(width = size.width * animProg, height = size.height),
            cornerRadius = CornerRadius(size.height / 2),
        )
    }
}
