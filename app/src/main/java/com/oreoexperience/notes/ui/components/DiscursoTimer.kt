package com.oreoexperience.notes.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.R
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlinx.coroutines.delay

/**
 * Cronómetro para un discurso. Cuenta hacia arriba en segundos y compara
 * contra [targetSec] (duración objetivo). El color del display y de la
 * barra de progreso transiciona violeta → ámbar → rojo según el ratio
 * elapsed/target.
 *
 * Estado puramente in-memory: si salís de la pantalla y volvés, arranca
 * de cero. Es intencional — no tiene sentido "recordar" un cronómetro
 * de un discurso que ya diste.
 */
@Composable
fun DiscursoTimer(
    targetSec: Int,
    modifier: Modifier = Modifier,
) {
    var elapsed by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        while (running) {
            delay(1_000)
            elapsed += 1
        }
    }

    val ratio = if (targetSec > 0) elapsed.toFloat() / targetSec else 0f
    val targetColor = when {
        targetSec <= 0 -> OreoPalette.Accent
        ratio < 0.85f -> OreoPalette.OkFill
        ratio < 1.0f -> OreoPalette.WarnFill
        else -> OreoPalette.DangerFill
    }
    val color by animateColorAsState(targetColor, label = "timerColor")
    val animatedRatio by animateFloatAsState(
        targetValue = ratio.coerceIn(0f, 1f),
        label = "timerRatio",
    )

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        fill = OreoPalette.GlassFillFrosted,
        borderAlpha = 0.55f,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatHMS(elapsed),
                        color = color,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = if (targetSec > 0)
                            "Objetivo: ${formatHMS(targetSec)}"
                        else stringResource(R.string.timer_no_target),
                        color = OreoPalette.OnSurfaceMuted,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                IconButton(onClick = { running = !running }) {
                    Icon(
                        imageVector = if (running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = stringResource(
                            if (running) R.string.timer_pause else R.string.timer_start
                        ),
                        tint = color,
                    )
                }
                IconButton(onClick = {
                    running = false
                    elapsed = 0
                }) {
                    Icon(
                        imageVector = Icons.Outlined.RestartAlt,
                        contentDescription = stringResource(R.string.timer_reset),
                        tint = OreoPalette.OnSurfaceMuted,
                    )
                }
            }
            if (targetSec > 0) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { animatedRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = color,
                    trackColor = Color.White.copy(alpha = 0.10f),
                )
            }
        }
    }
}

private fun formatHMS(totalSec: Int): String {
    val s = totalSec.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0)
        "%d:%02d:%02d".format(h, m, sec)
    else
        "%02d:%02d".format(m, sec)
}
