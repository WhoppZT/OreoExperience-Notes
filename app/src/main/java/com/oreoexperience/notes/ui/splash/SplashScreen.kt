package com.oreoexperience.notes.ui.splash

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.BuildConfig
import com.oreoexperience.notes.ui.theme.OreoMotion
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlinx.coroutines.delay

private const val SPLASH_DURATION_MS = 1600L
private const val FADE_OUT_MS = 380

/**
 * Pantalla de carga estilo iOS con identidad **OreoExperience Aurora**:
 * fondo negro puro, ícono cuadrado con la "N" estilizada sobre un
 * gradient violeta (deep → lavanda) y un halo violeta concéntrico que
 * pulsa suavemente. Debajo el título "OreoExperience · Notas", la
 * firma "Creado por Elihu Rueda" y la versión.
 *
 * Animaciones:
 *   - El ícono entra con un fade + spring low-bouncy (0.78× → 1×).
 *   - El halo pulsa entre 1× y 1.12× con curva cosenoidal infinita.
 *   - Total: ~1.6 s + 380 ms de fade-out.
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

    var entered by remember { mutableStateOf(false) }
    // Spring bouncy: el ícono entra con un "pop" elástico, no con
    // un tween rígido — sensación tipo iOS al desbloquear el iPhone.
    val iconScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.62f,
        animationSpec = OreoMotion.SpringHero(),
        label = "iconScale",
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(560, easing = EaseOutCubic),
        label = "iconAlpha",
    )

    // Morph hacia el FAB del Home: cuando empieza la salida, el ícono
    // se achica hacia la esquina inferior derecha (donde vive el botón
    // de nueva nota). Da continuidad visual entre el splash y la
    // pantalla principal.
    val exitScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.18f,
        animationSpec = tween(420, easing = OreoMotion.EaseEmphasized),
        label = "exitScale",
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

    // Halo pulsante atrás del ícono.
    val haloTransition = rememberInfiniteTransition(label = "splashHalo")
    val haloPulse by haloTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "haloPulse",
    )

    LaunchedEffect(Unit) {
        entered = true
        delay(SPLASH_DURATION_MS)
        visible = false
    }

    // Necesitamos importar mutableStateOf para los root size states
    // (queda arriba; este comentario es solo para anclar referencias)

    if (alpha < 0.01f) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(OreoPalette.Bg0),
        contentAlignment = Alignment.Center,
    ) {
        // Vector hacia el FAB: aproximamos su posición (esquina inferior
        // derecha con padding). Cuando exitTranslate avanza de 0 a 1, el
        // ícono se desplaza desde el centro de la pantalla hasta donde
        // está el botón flotante del Home.
        val density = LocalDensity.current
        var rootWidthPx by remember { mutableStateOf(0f) }
        var rootHeightPx by remember { mutableStateOf(0f) }
        val fabTargetX = with(density) {
            (rootWidthPx / 2f) - 36.dp.toPx() - 16.dp.toPx()
        }
        val fabTargetY = with(density) {
            (rootHeightPx / 2f) - 64.dp.toPx() - 24.dp.toPx()
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .onGloballyPositioned { coords ->
                    rootWidthPx = coords.size.width.toFloat()
                    rootHeightPx = coords.size.height.toFloat()
                }
                .graphicsLayer {
                    translationX = fabTargetX * exitTranslateX
                    translationY = fabTargetY * exitTranslateY
                    scaleX = exitScale
                    scaleY = exitScale
                },
        ) {
            // Logo con halo violeta atrás.
            Box(contentAlignment = Alignment.Center) {
                // Halo violeta exterior — círculo difuso que pulsa.
                Box(
                    modifier = Modifier
                        .size(168.dp)
                        .scale(haloPulse * iconScale)
                        .alpha(0.18f * iconAlpha)
                        .background(
                            color = OreoPalette.AccentSub,
                            shape = CircleShape,
                        ),
                )
                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .scale(haloPulse * iconScale)
                        .alpha(0.28f * iconAlpha)
                        .background(
                            color = OreoPalette.Accent,
                            shape = CircleShape,
                        ),
                )
                // Cuadrado del ícono con gradient violeta.
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .scale(iconScale)
                        .alpha(iconAlpha)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    OreoPalette.AccentDeep,
                                    OreoPalette.Accent,
                                    OreoPalette.AccentSub,
                                ),
                            ),
                            shape = RoundedCornerShape(28.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "N",
                        fontWeight = FontWeight.Black,
                        fontSize = 56.sp,
                        color = Color.White,
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = "OreoExperience · Notas",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = OreoPalette.OnSurface,
                modifier = Modifier.alpha(iconAlpha),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Aurora Edition",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = OreoPalette.AccentSub,
                modifier = Modifier.alpha(iconAlpha),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 40.dp)
                .alpha(iconAlpha),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Creado por Elihu Rueda",
                color = OreoPalette.OnSurfaceMuted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(6.dp))
            VersionBadge(version = BuildConfig.VERSION_NAME)
        }
    }
}

/**
 * Pill compacto con la versión actual. Si el versionName tiene un
 * sufijo como "ORBETA" (ej. "1.0.0 ORBETA"), separa el número del
 * canal y dibuja un mini-tag violeta a la derecha. Si no hay sufijo,
 * muestra solamente la versión.
 */
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
            text = "v$core",
            color = OreoPalette.OnSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
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
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}
