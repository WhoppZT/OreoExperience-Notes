@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.oreoexperience.notes.ui.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.ui.theme.OreoMotion
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlin.math.cos
import kotlin.math.sin

/**
 * Panel de Find & Replace con identidad Aurora.
 * Se monta sobre la top bar del editor con animación slide-in.
 */
@Composable
fun FindReplacePanel(
    findQuery: String,
    replaceText: String,
    caseSensitive: Boolean,
    useRegex: Boolean,
    matchCount: Int,
    currentMatchIndex: Int,
    regexError: Boolean,
    onFindQueryChange: (String) -> Unit,
    onReplaceTextChange: (String) -> Unit,
    onToggleCaseSensitive: () -> Unit,
    onToggleRegex: () -> Unit,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onReplaceCurrent: () -> Unit,
    onReplaceAll: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    // Gradiente animado para partículas de fondo
    val infiniteTransition = rememberInfiniteTransition(label = "findReplaceParticles")
    val particleOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "findReplaceParticleOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
    Surface(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        OreoPalette.SurfaceCard.copy(alpha = 0.95f),
                        OreoPalette.Bg1.copy(alpha = 0.98f),
                    ),
                ),
            )
            .border(
                width = 0.5.dp,
                color = OreoPalette.Outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(22.dp),
            ),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        shadowElevation = 12.dp,
        tonalElevation = 6.dp,
    ) {
        Box {
            // Partículas sutiles de fondo
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .alpha(0.15f)
            ) {
                val width = size.width
                val height = size.height

                val particles = listOf(
                    Triple(0.1f, 0.5f, 1.5f),
                    Triple(0.3f, 0.3f, 1.2f),
                    Triple(0.5f, 0.7f, 1.8f),
                    Triple(0.7f, 0.4f, 1.4f),
                    Triple(0.9f, 0.6f, 1.6f),
                )

                particles.forEachIndexed { index, (x, y, radius) ->
                    val offsetX = cos(particleOffset * 2f + index) * 12f
                    val offsetY = sin(particleOffset * 1.5f + index * 0.7f) * 8f

                    val particleX = width * x + offsetX
                    val particleY = height * y + offsetY

                    val alpha = 0.3f + 0.2f * sin(particleOffset * 3f + index)

                    drawCircle(
                        color = OreoPalette.Accent.copy(alpha = alpha),
                        radius = radius.dp.toPx(),
                        center = Offset(particleX, particleY)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
            // Top bar 48dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Buscar en la nota",
                    color = OreoPalette.OnSurfaceMuted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )

                Spacer(Modifier.width(6.dp))

                TogglePill(
                    label = "Aa",
                    active = caseSensitive,
                    onClick = onToggleCaseSensitive,
                    contentDescription = "Distinguir mayúsculas y minúsculas",
                )

                Spacer(Modifier.width(4.dp))

                TogglePill(
                    label = ".*",
                    active = useRegex,
                    onClick = onToggleRegex,
                    contentDescription = "Usar expresión regular",
                )

                Spacer(Modifier.weight(1f))

                IconButton(
                    onClick = {
                        keyboardController?.hide()
                        onClose()
                    },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Cerrar búsqueda",
                        tint = OreoPalette.OnSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Card unificada de inputs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(OreoPalette.SurfaceCard)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                // Campo de búsqueda
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = OreoPalette.OnSurfaceMuted,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 10.dp),
                    )

                    BasicTextField(
                        value = findQuery,
                        onValueChange = onFindQueryChange,
                        textStyle = LocalTextStyle.current.copy(
                            color = OreoPalette.OnSurface,
                            fontSize = 16.sp,
                        ),
                        cursorBrush = SolidColor(OreoPalette.Accent),
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onNext() }),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.CenterVertically),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                if (findQuery.isEmpty()) {
                                    Text(
                                        text = "Buscar en la nota…",
                                        color = OreoPalette.OnSurfaceFaint,
                                        fontSize = 16.sp,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )

                    AnimatedVisibility(
                        visible = findQuery.isNotEmpty(),
                        enter = fadeIn(tween(150)) + androidx.compose.animation.scaleIn(tween(150)),
                        exit = fadeOut(tween(100)) + androidx.compose.animation.scaleOut(tween(100)),
                    ) {
                        IconButton(
                            onClick = { onFindQueryChange("") },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Limpiar búsqueda",
                                tint = OreoPalette.OnSurfaceMuted,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                // Divisor
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(OreoPalette.OutlineFaint)
                        .padding(vertical = 6.dp),
                )

                // Campo de reemplazo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BorderColor,
                        contentDescription = null,
                        tint = OreoPalette.OnSurfaceMuted,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 10.dp),
                    )

                    BasicTextField(
                        value = replaceText,
                        onValueChange = onReplaceTextChange,
                        textStyle = LocalTextStyle.current.copy(
                            color = OreoPalette.OnSurface,
                            fontSize = 16.sp,
                        ),
                        cursorBrush = SolidColor(OreoPalette.Accent),
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.CenterVertically),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                if (replaceText.isEmpty()) {
                                    Text(
                                        text = "Reemplazar con…",
                                        color = OreoPalette.OnSurfaceFaint,
                                        fontSize = 16.sp,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                }

                if (regexError) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Expresión inválida",
                        color = OreoPalette.DangerFill,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 28.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Fila inferior: contador + navegación + acciones
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val hasQuery = findQuery.isNotBlank()
                if (hasQuery) {
                    val counterText = if (matchCount == 0) {
                        "Sin coincidencias"
                    } else {
                        "${currentMatchIndex + 1} de $matchCount"
                    }
                    val counterColor = if (matchCount == 0) {
                        OreoPalette.OnSurfaceFaint
                    } else {
                        OreoPalette.OnSurfaceMuted
                    }

                    AnimatedContent(
                        targetState = counterText,
                        transitionSpec = {
                            slideInVertically(
                                animationSpec = tween(180, easing = OreoMotion.EaseOut),
                                initialOffsetY = { it / 2 },
                            ) togetherWith slideOutVertically(
                                animationSpec = tween(180, easing = OreoMotion.EaseInOut),
                                targetOffsetY = { -it / 2 },
                            )
                        },
                        label = "matchCounter",
                        modifier = Modifier
                            .semantics { liveRegion = LiveRegionMode.Polite }
                            .weight(1f, fill = false),
                    ) { text ->
                        Text(
                            text = text,
                            color = counterColor,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    Spacer(Modifier.weight(1f, fill = false))
                }

                val hasMatches = matchCount > 0
                val canReplace = hasMatches && replaceText.isNotBlank() && !regexError

                IconButton(
                    onClick = onPrev,
                    enabled = hasMatches,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowUp,
                        contentDescription = "Coincidencia anterior",
                        tint = if (hasMatches) OreoPalette.Accent
                        else OreoPalette.OnSurfaceFaint.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp),
                    )
                }

                IconButton(
                    onClick = onNext,
                    enabled = hasMatches,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Siguiente coincidencia",
                        tint = if (hasMatches) OreoPalette.Accent
                        else OreoPalette.OnSurfaceFaint.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(Modifier.weight(1f))

                OutlinedButton(
                    onClick = onReplaceCurrent,
                    enabled = canReplace,
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (canReplace) OreoPalette.Accent else OreoPalette.OutlineFaint,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = "Reemplazar",
                        color = if (canReplace) OreoPalette.Accent else OreoPalette.OnSurfaceFaint.copy(alpha = 0.4f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                FilledTonalButton(
                    onClick = onReplaceAll,
                    enabled = hasMatches && replaceText.isNotBlank() && !regexError,
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = "Todo",
                        color = OreoPalette.Accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        } // Cierre del Box
    } // cierra Surface
    } // cierra Box centrador
}

@Composable
private fun TogglePill(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
) {
    val bgColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (active) OreoPalette.Accent.copy(alpha = 0.18f) else OreoPalette.SurfaceCardHi,
        animationSpec = tween(200, easing = OreoMotion.EaseOut),
        label = "togglePillBg",
    )
    val textColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (active) OreoPalette.Accent else OreoPalette.OnSurfaceMuted,
        animationSpec = tween(200, easing = OreoMotion.EaseOut),
        label = "togglePillText",
    )
    val borderColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (active) OreoPalette.Accent else Color.Transparent,
        animationSpec = tween(200, easing = OreoMotion.EaseOut),
        label = "togglePillBorder",
    )

    Box(
        modifier = Modifier
            .size(width = 36.dp, height = 32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .then(
                if (active) Modifier.border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(10.dp),
                ) else Modifier,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
