package com.oreoexperience.notes.ui.splash

import android.provider.Settings
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.BuildConfig
import com.oreoexperience.notes.R
import com.oreoexperience.notes.ui.theme.OreoMotion
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

private const val SPLASH_DURATION_MS = 1800L
private const val FADE_OUT_MS = 380

private data class Particle(
    val xFrac: Float,
    val yFrac: Float,
    val radiusDp: Dp,
    val minAlpha: Float,
    val maxAlpha: Float,
    val phase: Float,
    val orbitRadius: Float,
    val orbitSpeed: Float,
)

private fun seedParticles(count: Int = 25, seed: Long = 42L): List<Particle> {
    val r = Random(seed)
    return List(count) {
        Particle(
            xFrac = r.nextFloat(),
            yFrac = r.nextFloat(),
            radiusDp = (1.2f + r.nextFloat() * 1.8f).dp,
            minAlpha = 0.10f + r.nextFloat() * 0.12f,
            maxAlpha = 0.35f + r.nextFloat() * 0.20f,
            phase = r.nextFloat() * 2f * PI.toFloat(),
            orbitRadius = 8f + r.nextFloat() * 20f,
            orbitSpeed = 0.3f + r.nextFloat() * 0.7f,
        )
    }
}

/**
 * Pantalla de carga estilo iOS con identidad **OreoExperience Aurora**:
 * fondo negro con un "spot" radial violeta que respira lentamente,
 * partículas blancas que titilan, el ícono "N" sobre un cuadrado con
 * gradiente Aurora, halo violeta concéntrico pulsante, el título
 * "OreoExperience · Notas", la firma "Creado por Elihu Rueda" y la
 * versión actual con su canal.
 *
 * Secuencia de entrada (ms):
 *   0     → fondo y partículas visibles; logo con spring low-bouncy.
 *   400   → título fade-up 24 dp.
 *   600   → firma fade-in.
 *   800   → versión fade-in.
 *   1800  → morph de salida: el logo se encoge y se desplaza hacia el
 *           FAB del Home; todo se desvanece en 380 ms.
 *
 * @param isWarmStart true si la app vuelve de background hace menos de
 *   ~30 s. En ese caso se salta toda la secuencia y se llama
 *   [onFinished] tras un frame, dejando solo el splash nativo.
 */
@Composable
fun SplashScreen(
    isWarmStart: Boolean = false,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val reduceMotion = remember {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        } catch (_: Settings.SettingNotFoundException) {
            false
        }
    }
    val skipComposeSplash = isWarmStart || reduceMotion

    val startAnimated = remember { !skipComposeSplash }
    var visible by remember { mutableStateOf(true) }
    var entered by remember { mutableStateOf(startAnimated) }
    var titleVisible by remember { mutableStateOf(startAnimated) }
    var authorVisible by remember { mutableStateOf(startAnimated) }
    var versionVisible by remember { mutableStateOf(startAnimated) }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(FADE_OUT_MS, easing = FastOutSlowInEasing),
        label = "splashAlpha",
    )

    val iconScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = 0.25f,
            stiffness = 180f,
        ),
        label = "iconScale",
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(400, easing = EaseOutCubic),
        label = "iconAlpha",
    )

    val titleAlpha by animateFloatAsState(
        targetValue = if (titleVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.40f,
            stiffness = 200f,
        ),
        label = "titleAlpha",
    )
    val titleOffsetY by animateFloatAsState(
        targetValue = if (titleVisible) 0f else 35f,
        animationSpec = spring(
            dampingRatio = 0.40f,
            stiffness = 200f,
        ),
        label = "titleOffsetY",
    )

    val authorAlpha by animateFloatAsState(
        targetValue = if (authorVisible) 1f else 0f,
        animationSpec = tween(280, easing = EaseOutCubic),
        label = "authorAlpha",
    )
    val versionAlpha by animateFloatAsState(
        targetValue = if (versionVisible) 1f else 0f,
        animationSpec = tween(280, easing = EaseOutCubic),
        label = "versionAlpha",
    )

    val exitScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.18f,
        animationSpec = tween(420, easing = OreoMotion.EaseEmphasized),
        label = "exitScale",
    )
    val exitSquash by animateFloatAsState(
        targetValue = if (visible) 1f else 0.6f,
        animationSpec = tween(280, easing = OreoMotion.EaseEmphasized),
        label = "exitSquash",
    )
    val exitTranslateX by animateFloatAsState(
        targetValue = if (visible) 0f else 1f,
        animationSpec = tween(420, easing = OreoMotion.EaseEmphasized),
        label = "exitTx",
    )
    val exitTranslateY by animateFloatAsState(
        targetValue = if (visible) 0f else 1f,
        animationSpec = tween(420, easing = OreoMotion.EaseEmphasized),
        label = "exitTy",
    )

    val halo = rememberInfiniteTransition(label = "splashHalo")
    val haloPulse by halo.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "haloPulse",
    )

    val bg = rememberInfiniteTransition(label = "splashBg")
    val bgRotation by bg.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bgRotation",
    )
    val bgCenterX by bg.animateFloat(
        initialValue = 0.32f,
        targetValue = 0.68f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bgCenterX",
    )
    val bgCenterY by bg.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.58f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bgCenterY",
    )

    val twinkle = rememberInfiniteTransition(label = "splashTwinkle")
    val twinkleT by twinkle.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
        ),
        label = "twinkleT",
    )

    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (skipComposeSplash) {
            delay(32)
            finished = true
            return@LaunchedEffect
        }
        entered = true
        delay(400)
        titleVisible = true
        delay(200)
        authorVisible = true
        delay(200)
        versionVisible = true
        delay(SPLASH_DURATION_MS - 1000)
        visible = false
        delay(FADE_OUT_MS.toLong() + 150)
        finished = true
    }

    LaunchedEffect(finished) {
        if (finished) onFinished()
    }

    if (alpha < 0.01f) return

    val particles = remember { seedParticles() }
    val density = LocalDensity.current
    var rootWidthPx by remember { mutableStateOf(0f) }
    var rootHeightPx by remember { mutableStateOf(0f) }
    val fabTargetX = with(density) { (rootWidthPx / 2f) - 36.dp.toPx() - 16.dp.toPx() }
    val fabTargetY = with(density) { (rootHeightPx / 2f) - 64.dp.toPx() - 24.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(OreoPalette.Bg0)
            .onSizeChanged {
                rootWidthPx = it.width.toFloat()
                rootHeightPx = it.height.toFloat()
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val brush = Brush.radialGradient(
                colors = listOf(
                    OreoPalette.AccentDeep.copy(alpha = 0.30f),
                    OreoPalette.Accent.copy(alpha = 0.10f),
                    Color.Transparent,
                ),
                center = Offset(w * bgCenterX, h * bgCenterY),
                radius = max(w, h) * 0.75f,
            )
            drawRect(brush = brush)
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            particles.forEach { p ->
                val f = sin(twinkleT + p.phase) * 0.5f + 0.5f
                val a = p.minAlpha + (p.maxAlpha - p.minAlpha) * f
                val orbitX = cos(twinkleT * p.orbitSpeed + p.phase) * p.orbitRadius
                val orbitY = sin(twinkleT * p.orbitSpeed + p.phase) * p.orbitRadius
                drawCircle(
                    color = Color.White.copy(alpha = a),
                    radius = with(density) { p.radiusDp.toPx() },
                    center = Offset(
                        w * p.xFrac + orbitX,
                        h * p.yFrac + orbitY,
                    ),
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.graphicsLayer {
                    translationX = fabTargetX * exitTranslateX
                    translationY = fabTargetY * exitTranslateY
                    scaleX = exitScale * exitSquash
                    scaleY = exitScale / exitSquash
                },
            ) {
                Canvas(
                    modifier = Modifier
                        .size(200.dp)
                        .scale(haloPulse * iconScale)
                        .alpha(iconAlpha)
                ) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val s = size.width / 200f

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                OreoPalette.AccentDeep.copy(alpha = 0.30f),
                                OreoPalette.AccentDeep.copy(alpha = 0.08f),
                                Color.Transparent,
                            ),
                            center = Offset(cx, cy),
                            radius = size.width * 0.48f,
                        ),
                        radius = size.width * 0.48f,
                        center = Offset(cx, cy),
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                OreoPalette.AccentGlow.copy(alpha = 0.22f),
                                Color.Transparent,
                            ),
                            center = Offset(cx, cy),
                            radius = size.width * 0.26f,
                        ),
                        radius = size.width * 0.26f,
                        center = Offset(cx, cy),
                    )

                    val cR = size.width * 0.33f
                    val pts = listOf(
                        Offset(cx, cy - cR),
                        Offset(cx + cR * 0.95f, cy - cR * 0.31f),
                        Offset(cx + cR * 0.59f, cy + cR * 0.81f),
                        Offset(cx - cR * 0.59f, cy + cR * 0.81f),
                        Offset(cx - cR * 0.95f, cy - cR * 0.31f),
                    )
                    for (pt in pts) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.12f),
                            start = Offset(cx, cy),
                            end = pt,
                            strokeWidth = 1.2f * s,
                            cap = StrokeCap.Round,
                        )
                    }
                    val pR = listOf(4f, 3f, 5f, 3.5f, 3f)
                    val pA = listOf(0.75f, 0.55f, 0.85f, 0.65f, 0.6f)
                    pts.forEachIndexed { i, pt ->
                        drawCircle(
                            color = Color.White.copy(alpha = pA[i]),
                            radius = pR[i] * s,
                            center = pt,
                        )
                    }
                }
                Text(
                    text = "N",
                    fontWeight = FontWeight.Black,
                    fontSize = 56.sp,
                    color = Color.White,
                    modifier = Modifier
                        .scale(iconScale)
                        .alpha(iconAlpha),
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = stringResource(R.string.splash_app_title),
                style = MaterialTheme.typography.headlineSmall,
                color = OreoPalette.OnSurface,
                modifier = Modifier
                    .alpha(titleAlpha)
                    .graphicsLayer { translationY = titleOffsetY },
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.splash_author),
                style = MaterialTheme.typography.titleSmall,
                color = OreoPalette.OnSurfaceMuted,
                modifier = Modifier.alpha(authorAlpha),
            )
            Spacer(Modifier.height(6.dp))
            Box(modifier = Modifier.alpha(versionAlpha)) {
                VersionBadge(version = BuildConfig.VERSION_NAME)
            }
        }
    }
}

@Composable
private fun VersionBadge(version: String) {
    val parts = version.trim().split(" ", limit = 2)
    val core = parts.firstOrNull().orEmpty()
    val channel = parts.getOrNull(1)
    Row(
        modifier = Modifier
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        OreoPalette.AccentDeep.copy(alpha = 0.32f),
                        OreoPalette.Accent.copy(alpha = 0.28f),
                    ),
                ),
                shape = RoundedCornerShape(999.dp),
            )
            .border(
                width = 1.dp,
                color = OreoPalette.Accent.copy(alpha = 0.45f),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${stringResource(R.string.splash_version_prefix)}$core",
            style = MaterialTheme.typography.labelSmall,
            color = OreoPalette.OnSurface,
        )
        if (!channel.isNullOrBlank()) {
            Spacer(Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .background(
                        color = OreoPalette.Accent,
                        shape = RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            ) {
                Text(
                    text = channel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}
