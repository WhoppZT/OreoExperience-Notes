package com.oreoexperience.notes.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlinx.coroutines.delay

/**
 * Barra inferior fija con cronómetro, anclada al fondo del editor. Se
 * muestra solamente si la nota tiene `targetSec > 0`. Diseño plano sin
 * arrastrar — siempre visible en su sitio.
 *
 * Estructura de la barra:
 *
 *   [⏵/⏸]   00:00  ─────────────────────────  10 min   [↺]
 *
 *   - Botón circular play/pause a la izquierda (acento amarillo).
 *   - Tiempo transcurrido en mono.
 *   - Línea de progreso (0% → 100%) que avanza con el tiempo y cambia
 *     de color al cruzar 85% (ámbar) y 100% (rojo, pulsando).
 *   - Tiempo objetivo a la derecha.
 *   - Botón secundario para reiniciar.
 *
 * El estado vive en memoria, interno al composable. Si la pantalla se
 * destruye y se vuelve a abrir, el cronómetro arranca de cero (intencional
 * — un nuevo evento es un nuevo conteo).
 */
@Composable
fun BottomTimerBar(
    targetSec: Int,
    modifier: Modifier = Modifier,
) {
    if (targetSec <= 0) return

    var elapsed by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        while (running) {
            delay(1_000)
            elapsed += 1
        }
    }

    val ratio = (elapsed.toFloat() / targetSec.toFloat()).coerceAtLeast(0f)

    // Pulso suave cuando se pasa del objetivo: subimos la opacidad del
    // borde rojo arriba/abajo. Sutil, no distrae la lectura.
    val pulse = rememberInfiniteTransition(label = "timerOverPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.55f,
        targetValue = if (ratio >= 1f && running) 1f else 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_300, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "timerPulseAlpha",
    )

    val progressColor by animateColorAsState(
        targetValue = when {
            ratio >= 1f -> OreoPalette.DangerFill
            ratio >= 0.85f -> OreoPalette.WarnFill
            else -> OreoPalette.Accent
        },
        animationSpec = tween(durationMillis = 600),
        label = "timerProgressColor",
    )

    val borderColor = when {
        ratio >= 1f -> OreoPalette.DangerFill.copy(alpha = pulseAlpha)
        else -> OreoPalette.Outline
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .background(
                color = OreoPalette.SurfaceCard,
                shape = RoundedCornerShape(22.dp),
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(22.dp),
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Play / Pause círculo
            CircleAccentButton(
                onClick = { running = !running },
                tint = if (running) OreoPalette.Accent else OreoPalette.Accent,
            ) {
                Icon(
                    imageVector = if (running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (running) "Pausar" else "Iniciar",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            // Tiempo transcurrido (mono)
            Text(
                text = formatHms(elapsed),
                color = OreoPalette.OnSurface,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
            )

            Spacer(Modifier.width(12.dp))

            // Espacio flexible: la barra de progreso vive aquí
            Box(modifier = Modifier.weight(1f)) {
                LinearProgressIndicator(
                    progress = { ratio.coerceAtMost(1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = progressColor,
                    trackColor = OreoPalette.OutlineFaint,
                )
            }

            Spacer(Modifier.width(12.dp))

            // Tiempo objetivo
            Text(
                text = formatHmsShort(targetSec),
                color = OreoPalette.OnSurfaceMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                modifier = Modifier.alpha(0.85f),
            )

            Spacer(Modifier.width(8.dp))

            // Reiniciar
            IconButton(
                onClick = { elapsed = 0; running = false },
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.RestartAlt,
                    contentDescription = "Reiniciar",
                    tint = OreoPalette.OnSurfaceMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun CircleAccentButton(
    onClick: () -> Unit,
    tint: Color,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(color = tint, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
            content()
        }
    }
}

private fun formatHms(seconds: Int): String {
    val h = seconds / 3_600
    val m = (seconds % 3_600) / 60
    val s = seconds % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private fun formatHmsShort(seconds: Int): String {
    val m = seconds / 60
    return if (m >= 60) {
        val h = m / 60
        val mm = m % 60
        if (mm == 0) "${h}h" else "${h}h ${mm}m"
    } else "${m} min"
}
