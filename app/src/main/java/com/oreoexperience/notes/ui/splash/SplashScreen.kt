package com.oreoexperience.notes.ui.splash

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.BuildConfig
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlinx.coroutines.delay

private const val SPLASH_DURATION_MS = 1400L
private const val FADE_OUT_MS = 380

/**
 * Pantalla de carga estilo iOS: fondo negro puro, ícono cuadrado amarillo
 * con esquinas redondeadas (mock del icon iOS), nombre de la app debajo,
 * y al fondo la firma "Creado por Elihu Rueda" + versión.
 *
 * Animación mucho más sutil que la versión Aurora: el ícono entra con un
 * fade + scale spring (1.0 → 1.04 → 1.0), nada más. No hay halo, ni
 * estrellas, ni anillo orbital. Total: ~1.4 s + 380 ms de fade-out.
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
    val iconScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.86f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "iconScale",
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(540, easing = EaseOutCubic),
        label = "iconAlpha",
    )

    LaunchedEffect(Unit) {
        entered = true
        delay(SPLASH_DURATION_MS)
        visible = false
    }

    if (alpha < 0.01f) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(OreoPalette.Bg0),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Ícono cuadrado amarillo con la "N" estilizada en negro.
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(iconScale)
                    .alpha(iconAlpha)
                    .background(
                        color = OreoPalette.Accent,
                        shape = RoundedCornerShape(22.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "N",
                    fontWeight = FontWeight.Black,
                    fontSize = 56.sp,
                    color = androidx.compose.ui.graphics.Color.Black,
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Notas",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = OreoPalette.OnSurface,
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
            Spacer(Modifier.height(2.dp))
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                color = OreoPalette.OnSurfaceFaint,
                fontSize = 11.sp,
            )
        }
    }
}


