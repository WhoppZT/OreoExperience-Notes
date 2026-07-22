@file:OptIn(ExperimentalFoundationApi::class)

package com.oreoexperience.notes.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.oreoexperience.notes.ui.LocalOreoWindowSizeClass
import com.oreoexperience.notes.ui.OreoWidthSizeClass
import com.oreoexperience.notes.ui.theme.OreoPalette

// ─── Variant ────────────────────────────────────────────────────────

sealed interface OreoTimerVariant {
    data object Editor : OreoTimerVariant
    data object Discourse : OreoTimerVariant
}

// ─── OreoTimer ──────────────────────────────────────────────────────

@Composable
fun OreoTimer(
    targetSec: Int,
    elapsedSec: Int,
    running: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onClose: (() -> Unit)? = null,
    variant: OreoTimerVariant,
    modifier: Modifier = Modifier,
) {
    if (targetSec <= 0) return

    val haptics = LocalHapticFeedback.current
    val isCompact = LocalOreoWindowSizeClass.current.widthSizeClass == OreoWidthSizeClass.Compact

    val maxWidth = when {
        variant == OreoTimerVariant.Editor && !isCompact    -> 720.dp
        variant == OreoTimerVariant.Discourse && !isCompact -> 880.dp
        else                                                -> Dp.Infinity
    }

    val isOver = elapsedSec >= targetSec
    val ratio = (elapsedSec.toFloat() / targetSec.toFloat()).coerceIn(0f, 1f)
    val animRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = tween(600),
        label = "progress",
    )

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(20))
                .background(OreoPalette.SurfaceCard)
                .border(1.dp, OreoPalette.Outline, RoundedCornerShape(20)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // play/pause — círculo con fondo accent
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(OreoPalette.Accent)
                        .combinedClickable(
                            onClick = onToggle,
                            onLongClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onReset()
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (running) "Pausar" else "Iniciar",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                // línea de progreso — track sutil OutlineFaint + fill gradiente Accent
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                OreoPalette.OutlineFaint,
                                RoundedCornerShape(3.dp),
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animRatio.coerceIn(0f, 1f))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(OreoPalette.Accent, OreoPalette.AccentSub),
                                ),
                                RoundedCornerShape(3.dp),
                            ),
                    )
                }
                Spacer(Modifier.width(12.dp))
                // objetivo + separador
                Row(
                    modifier = Modifier.clickable(onClick = onToggle),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatTargetCompact(targetSec),
                        color = OreoPalette.OnSurfaceMuted,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "\u00B7",
                        color = OreoPalette.OnSurfaceFaint,
                        fontSize = 16.sp,
                    )
                }
                Spacer(Modifier.width(6.dp))
                // tiempo
                Text(
                    text = if (isOver) "+${formatHms(elapsedSec - targetSec)}"
                           else formatHms(elapsedSec),
                    color = if (isOver) OreoPalette.DangerFill
                            else OreoPalette.OnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable(onClick = onToggle),
                )
                // cerrar
                if (onClose != null) {
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Quitar cronómetro",
                            tint = OreoPalette.OnSurfaceMuted,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

// ─── Helpers ────────────────────────────────────────────────────────

private fun formatHms(seconds: Int): String {
    val h = seconds / 3_600
    val m = (seconds % 3_600) / 60
    val s = seconds % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private fun formatTargetCompact(seconds: Int): String {
    val totalMin = seconds / 60
    return when {
        totalMin >= 60 -> {
            val h = totalMin / 60
            val mm = totalMin % 60
            if (mm == 0) "${h} h" else "${h} h ${mm} min"
        }
        else -> "${totalMin} min"
    }
}
