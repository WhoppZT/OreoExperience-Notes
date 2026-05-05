package com.oreoexperience.notes.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.OpenInFull
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.R
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlinx.coroutines.delay

/**
 * Mini ventana flotante con cronómetro, encima del contenido de la pantalla
 * de lectura. Es arrastrable: el usuario la puede mover a cualquier esquina
 * sin perder el progreso. Tiene dos modos:
 *
 *   - **Compacto**: pill chico con tiempo + play/pause. No estorba la
 *     lectura.
 *   - **Expandido**: card completa con objetivo, barra de progreso y
 *     botones de iniciar / pausar / reiniciar.
 *
 * El estado vive interno al composable: si te vas y volvés a la pantalla,
 * el cronómetro arranca de cero (intencional — un nuevo discurso es un
 * nuevo conteo).
 *
 * Uso (debe ir como hijo de un `Box` que ocupe el área disponible):
 * ```
 * Box(Modifier.fillMaxSize()) {
 *     LazyColumn(...) { ... }
 *     FloatingTimerWindow(targetSec = d.targetDurationSec)
 * }
 * ```
 */
@Composable
fun FloatingTimerWindow(
    targetSec: Int,
) {
    // [BoxWithConstraints] con fillMaxSize() para conocer el ancho y alto
    // disponibles. No agrega `pointerInput` propio: cuando tocás afuera
    // del chip, el evento pasa transparente al LazyColumn que está
    // debajo, así no se bloquea el scroll del contenido.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val containerW = constraints.maxWidth.toFloat()
        val containerH = constraints.maxHeight.toFloat()

        var elapsed by remember { mutableIntStateOf(0) }
        var running by remember { mutableStateOf(false) }
        var expanded by remember { mutableStateOf(false) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var windowSize by remember { mutableStateOf(IntSize.Zero) }
        var initialized by remember { mutableStateOf(false) }

        // Posición inicial: arriba-derecha, separado del top app bar.
        LaunchedEffect(windowSize, containerW) {
            if (!initialized && windowSize.width > 0 && containerW > 0f) {
                val marginPx = with(density) { 12.dp.toPx() }
                val topPx = with(density) { 12.dp.toPx() }
                offset = Offset(
                    x = (containerW - windowSize.width - marginPx).coerceAtLeast(0f),
                    y = topPx,
                )
                initialized = true
            }
        }

        // Tick cada segundo mientras corre.
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
        val color by animateColorAsState(targetColor, label = "floatingTimerColor")
        val animatedRatio by animateFloatAsState(
            targetValue = ratio.coerceIn(0f, 1f),
            label = "floatingTimerRatio",
        )

        // Pulso "respiración" cuando te pasaste del objetivo. Amplitud chica
        // (1.04) + duración larga (1100ms) + EaseInOutCubic = late orgánico,
        // no taquicárdico.
        val pulseTransition = rememberInfiniteTransition(label = "timerPulse")
        val pulseScale by pulseTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (ratio >= 1f && running) 1.04f else 1.0001f,
            animationSpec = infiniteRepeatable(
                animation = tween(1_100, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "timerPulseScale",
        )

        // Press scale al play/pause: spring sin rebote para que se sienta
        // un "settle" suave en vez de un golpe.
        val pressScale by animateFloatAsState(
            targetValue = if (running) 1.0f else 0.985f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "timerPressScale",
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
                .scale(pulseScale * pressScale)
                .onSizeChanged { windowSize = it }
                .pointerInput(containerW, containerH, windowSize) {
                    if (containerW <= 0f || containerH <= 0f) return@pointerInput
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val maxX = (containerW - windowSize.width).coerceAtLeast(0f)
                        val maxY = (containerH - windowSize.height).coerceAtLeast(0f)
                        offset = Offset(
                            x = (offset.x + dragAmount.x).coerceIn(0f, maxX),
                            y = (offset.y + dragAmount.y).coerceIn(0f, maxY),
                        )
                    }
                },
        ) {
            AnimatedContent(
                targetState = expanded,
                transitionSpec = {
                    (fadeIn(tween(280, easing = EaseInOutCubic)) +
                        scaleIn(tween(280, easing = EaseInOutCubic), initialScale = 0.94f))
                        .togetherWith(
                            fadeOut(tween(180, easing = EaseInOutCubic)) +
                                scaleOut(tween(180, easing = EaseInOutCubic), targetScale = 0.94f)
                        )
                },
                label = "timerModeSwitch",
            ) { isExpanded ->
                if (isExpanded) {
                    ExpandedTimer(
                        elapsed = elapsed,
                        targetSec = targetSec,
                        running = running,
                        ratio = animatedRatio,
                        color = color,
                        onToggle = { running = !running },
                        onReset = { running = false; elapsed = 0 },
                        onCollapse = { expanded = false },
                    )
                } else {
                    CompactTimer(
                        elapsed = elapsed,
                        targetSec = targetSec,
                        running = running,
                        color = color,
                        onToggle = { running = !running },
                        onExpand = { expanded = true },
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactTimer(
    elapsed: Int,
    targetSec: Int,
    running: Boolean,
    color: Color,
    onToggle: () -> Unit,
    onExpand: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.padding(2.dp),
        cornerRadius = 18.dp,
        fill = OreoPalette.GlassFillFrosted,
        borderAlpha = 0.7f,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = formatHMS(elapsed),
                    color = color,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
                if (targetSec > 0) {
                    Text(
                        text = "/ ${formatHMS(targetSec)}",
                        color = OreoPalette.OnSurfaceMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
            IconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = stringResource(
                        if (running) R.string.timer_pause else R.string.timer_start
                    ),
                    tint = color,
                )
            }
            IconButton(onClick = onExpand, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Outlined.OpenInFull,
                    contentDescription = "Expandir",
                    tint = OreoPalette.OnSurfaceMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ExpandedTimer(
    elapsed: Int,
    targetSec: Int,
    running: Boolean,
    ratio: Float,
    color: Color,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onCollapse: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.width(260.dp),
        cornerRadius = 22.dp,
        fill = OreoPalette.GlassFillFrosted,
        borderAlpha = 0.7f,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatHMS(elapsed),
                        color = color,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = if (targetSec > 0)
                            "Objetivo: ${formatHMS(targetSec)}"
                        else stringResource(R.string.timer_no_target),
                        color = OreoPalette.OnSurfaceMuted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                IconButton(onClick = onCollapse, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Compactar",
                        tint = OreoPalette.OnSurfaceMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            if (targetSec > 0) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp),
                    color = color,
                    trackColor = Color.White.copy(alpha = 0.10f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = stringResource(
                            if (running) R.string.timer_pause else R.string.timer_start
                        ),
                        tint = color,
                    )
                }
                IconButton(onClick = onReset) {
                    Icon(
                        imageVector = Icons.Outlined.RestartAlt,
                        contentDescription = stringResource(R.string.timer_reset),
                        tint = OreoPalette.OnSurfaceMuted,
                    )
                }
            }
        }
    }
}

private fun formatHMS(totalSec: Int): String {
    val s = totalSec.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}
