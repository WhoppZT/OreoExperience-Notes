@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.oreoexperience.notes.ui.editor

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.data.ai.AiChunk
import com.oreoexperience.notes.ui.theme.OreoMotion
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Sheet que muestra el resultado de una acción de IA en streaming.
 * Mientras genera, el texto aparece palabra por palabra con un cursor
 * parpadeante. Cuando termina, ofrece reemplazar, insertar abajo,
 * copiar o descartar.
 */
@Composable
fun AiSuggestionSheet(
    actionLabel: String,
    originalText: String,
    createStreamFlow: () -> Flow<AiChunk>,
    onDismiss: () -> Unit,
    onReplace: (String) -> Unit,
    onInsertBelow: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var generatedText by remember { mutableStateOf("") }
    var isStreaming by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var streamJob by remember { mutableStateOf<Job?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }
    var cooldownSeconds by remember { mutableLongStateOf(0L) }

    // Countdown ticker: cuando hay cooldown, baja cada segundo.
    LaunchedEffect(cooldownSeconds) {
        while (cooldownSeconds > 0) {
            delay(1000)
            cooldownSeconds -= 1
        }
    }

    LaunchedEffect(retryKey) {
        generatedText = ""
        isStreaming = true
        errorMessage = null
        cooldownSeconds = 0
        streamJob = scope.launch {
            createStreamFlow().collect { chunk ->
                when (chunk) {
                    is AiChunk.Delta -> generatedText += chunk.text
                    is AiChunk.Done -> {
                        isStreaming = false
                        streamJob = null
                    }
                    is AiChunk.Error -> {
                        errorMessage = chunk.message
                        isStreaming = false
                        streamJob = null
                        // Detectar rate-limit y mostrar countdown
                        if (chunk.message.contains("Esperá")) {
                            val regex = Regex("Esperá\\s+(\\d+)")
                            val match = regex.find(chunk.message)
                            cooldownSeconds = match?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 60L
                        }
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            streamJob?.cancel()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
        sheetMaxWidth = 640.dp,
    ) {
        // Fondo con gradiente sutil
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            OreoPalette.SurfaceCard,
                            OreoPalette.Bg1,
                        ),
                    ),
                ),
        ) {
            // Partículas decorativas en el header
            val particleTransition = rememberInfiniteTransition(label = "aiParticles")
            val particleOffset by particleTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "aiParticleOffset"
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .alpha(0.2f)
            ) {
                val width = size.width
                val height = size.height

                val particles = listOf(
                    Triple(0.1f, 0.3f, 2f),
                    Triple(0.25f, 0.7f, 1.5f),
                    Triple(0.4f, 0.4f, 2.5f),
                    Triple(0.6f, 0.6f, 1.8f),
                    Triple(0.75f, 0.2f, 2.2f),
                    Triple(0.9f, 0.5f, 1.6f),
                )

                particles.forEachIndexed { index, (x, y, radius) ->
                    val offsetX = cos(particleOffset * 2f + index) * 15f
                    val offsetY = sin(particleOffset * 1.5f + index * 0.7f) * 10f

                    val particleX = width * x + offsetX
                    val particleY = height * y + offsetY

                    val alpha = 0.3f + 0.2f * sin(particleOffset * 3f + index)

                    drawCircle(
                        color = OreoPalette.AccentSub.copy(alpha = alpha),
                        radius = radius.dp.toPx(),
                        center = Offset(particleX, particleY)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                // Header: icono IA + label + botón cerrar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Icono de AutoAwesome con halo
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(OreoPalette.Accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = OreoPalette.Accent,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    // Texto con gradiente
                    Text(
                        text = actionLabel,
                        style = androidx.compose.ui.text.TextStyle(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    OreoPalette.Accent,
                                    OreoPalette.AccentSub,
                                ),
                            ),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        modifier = Modifier.weight(1f),
                    )

                    IconButton(onClick = {
                        streamJob?.cancel()
                        onDismiss()
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Cerrar",
                            tint = OreoPalette.OnSurfaceMuted,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

            Spacer(Modifier.height(16.dp))

            // Área de texto generado con scroll
            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(max = 240.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(OreoPalette.Bg1)
                    .verticalScroll(scrollState)
                    .padding(14.dp),
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = OreoPalette.DangerFill,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else if (generatedText.isEmpty() && isStreaming) {
                    Text(
                        text = "Generando…",
                        color = OreoPalette.OnSurfaceFaint,
                        fontSize = 14.sp,
                    )
                } else {
                    Column {
                        Text(
                            text = generatedText,
                            color = OreoPalette.OnSurface,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                        )
                        // Cursor parpadeante mientras genera
                        if (isStreaming) {
                            BlinkingCursor()
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Botones de acción
            if (errorMessage != null) {
                // Estado de error: Reintentar + Descartar
                val isRateLimited = cooldownSeconds > 0
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Badge de rate-limit con countdown
                    if (isRateLimited) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(OreoPalette.Accent.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "⏳",
                                fontSize = 16.sp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Límite alcanzado. Reintentá en ${cooldownSeconds}s",
                                color = OreoPalette.Accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                streamJob?.cancel()
                                errorMessage = null
                                generatedText = ""
                                isStreaming = true
                                retryKey += 1
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isRateLimited,
                        ) {
                            Text(
                                "Reintentar",
                                color = if (isRateLimited) OreoPalette.OnSurfaceFaint else OreoPalette.Accent,
                            )
                        }
                        TextButton(
                            onClick = {
                                streamJob?.cancel()
                                onDismiss()
                            },
                        ) {
                            Text("Descartar", color = OreoPalette.OnSurfaceMuted)
                        }
                    }
                }
            } else if (!isStreaming && generatedText.isNotBlank()) {
                // Estado terminado: Reemplazar, Insertar abajo, Copiar, Descartar
                val clipboard = LocalClipboardManager.current
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Reemplazar (primario)
                        TextButton(
                            onClick = {
                                onReplace(generatedText)
                                streamJob?.cancel()
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(OreoPalette.Accent)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Text(
                                "Reemplazar",
                                color = OreoPalette.Bg0,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        // Insertar abajo
                        TextButton(
                            onClick = {
                                onInsertBelow(generatedText)
                                streamJob?.cancel()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Insertar abajo", color = OreoPalette.Accent, fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Copiar
                        TextButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(generatedText))
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = null,
                                tint = OreoPalette.OnSurfaceMuted,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Copiar", color = OreoPalette.OnSurfaceMuted, fontSize = 13.sp)
                        }
                        // Descartar
                        TextButton(
                            onClick = {
                                streamJob?.cancel()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Descartar", color = OreoPalette.OnSurfaceMuted, fontSize = 13.sp)
                        }
                    }
                }
            } else if (isStreaming) {
                // Mientras genera: botón Detener
                TextButton(
                    onClick = {
                        streamJob?.cancel()
                        isStreaming = false
                        streamJob = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Detener", color = OreoPalette.DangerFill, fontSize = 14.sp)
                }
            }
        }
        } // Cierre del Box
    }
}

/** Cursor parpadeante que aparece mientras se genera texto. */
@Composable
private fun BlinkingCursor() {
    val infinite = rememberInfiniteTransition(label = "cursorBlink")
    val alpha by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(530, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cursorAlpha",
    )
    Text(
        text = "▎",
        color = OreoPalette.Accent.copy(alpha = alpha),
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
    )
}
