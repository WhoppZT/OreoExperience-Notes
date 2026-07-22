package com.oreoexperience.notes.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.ui.theme.OreoPalette
import androidx.compose.animation.core.Animatable
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Fila con swipe revelado estilo iOS Mail / Gmail.
 *
 * - Deslizar la card a la izquierda revela un botón "Eliminar" rojo
 *   detrás de ella.
 * - Soltar antes del 30% del ancho del botón → snap a 0 (se cierra).
 * - Soltar después del 30% → snap a posición revelada.
 * - El botón "Eliminar" nunca elimina directo; dispara [onRequestDelete].
 * - Notas fijadas ([pinned] = true) no registran el gesto de swipe.
 *
 * @param pinned si es true, no hay gesto de swipe y se muestra candado.
 * @param cardId ID de esta nota.
 * @param revealedId ID de la nota actualmente revelada (global).
 * @param onRevealChanged callback para notificar cambio de estado revelado.
 * @param onRequestDelete dispara el flujo de confirmación de borrado.
 * @param content la card que se mueve con el swipe.
 */
@Composable
fun SwipeRevealRow(
    pinned: Boolean,
    cardId: Long,
    revealedId: Long?,
    onRevealChanged: (Long?) -> Unit,
    onRequestDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val buttonWidthPx = with(density) { 88.dp.toPx() }
    val thresholdPx = buttonWidthPx * 0.30f

    val animatedOffset = remember { Animatable(0f) }
    val currentOffset by animatedOffset.asState()

    // Animación reactiva al cambio de revealedId.
    // El drag handler solo llama onRevealChanged; la animación corre acá.
    // Usamos spring bouncy para que el botón "entre" con un rebote pronunciado.
    val snapSpec = spring<Float>(
        dampingRatio = 0.35f,
        stiffness = Spring.StiffnessMediumLow,
    )
    LaunchedEffect(revealedId) {
        if (revealedId == cardId) {
            animatedOffset.animateTo(-buttonWidthPx, snapSpec)
        } else if (currentOffset != 0f) {
            animatedOffset.animateTo(0f, snapSpec)
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(22.dp))
                .background(OreoPalette.DangerFill.copy(alpha = (currentOffset / -buttonWidthPx).coerceIn(0f, 1f))),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Column(
                modifier = Modifier
                    .width(88.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (revealedId == cardId) {
                            onRequestDelete()
                        }
                    }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Eliminar",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Card que se mueve con el drag
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(currentOffset.roundToInt(), 0) }
                .then(
                    if (pinned) {
                        Modifier
                    } else {
                        Modifier.pointerInput(cardId) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (currentOffset < -thresholdPx) {
                                        onRevealChanged(cardId)
                                    } else {
                                        onRevealChanged(null)
                                    }
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    val newOffset = (currentOffset + dragAmount)
                                        .coerceIn(-buttonWidthPx, 0f)
                                    scope.launch {
                                        animatedOffset.snapTo(newOffset)
                                    }
                                },
                            )
                        }
                    }
                ),
        ) {
            content()

            if (pinned) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Fijada",
                        tint = OreoPalette.OnSurfaceFaint,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Overlay que intercepta taps cuando la card está revelada
            if (revealedId == cardId) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* bloquea el click para que la NoteCard no navegue */ },
                        ),
                )
            }
        }
    }
}
