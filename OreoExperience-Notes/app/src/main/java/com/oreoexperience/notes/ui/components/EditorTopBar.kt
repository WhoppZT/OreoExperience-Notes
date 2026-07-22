@file:OptIn(ExperimentalMaterial3Api::class)

package com.oreoexperience.notes.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.ui.LocalOreoWindowSizeClass
import com.oreoexperience.notes.ui.OreoWidthSizeClass
import com.oreoexperience.notes.ui.editor.SaveStatus
import com.oreoexperience.notes.ui.theme.OreoPalette

/**
 * Top bar premium para el editor de notas, alineada con la identidad Aurora.
 *
 * Diseño minimalista sin pipas separadoras ni glassmorphism:
 *  - Botón back neutro (OnSurface, no Accent) con tap target 44dp.
 *  - Título de la nota en 17sp SemiBold, con subtítulo opcional debajo
 *    (conteo de palabras · tiempo relativo). Toda la zona es clickeable
 *    para enfocar el campo de título.
 *  - Cluster de acciones a la derecha, sin separadores, cada una en
 *    IconButton 40dp con tint OnSurface (Accent cuando está activa).
 *  - Indicador sutil de guardado que aparece/desaparece con slide
 *    horizontal.
 *  - Línea divisoria inferior de 1dp en OutlineFaint que se desvanece
 *    cuando el scroll está al tope.
 */
@Composable
fun EditorTopBar(
    title: String,
    subtitle: String,
    isEmpty: Boolean,
    saveStatus: SaveStatus,
    findOpen: Boolean,
    scrollFraction: Float,
    onReaderMode: (() -> Unit)? = null,
    onBack: () -> Unit,
    onTitleClick: () -> Unit,
    onToggleFind: () -> Unit,
    onTogglePin: () -> Unit,
    onShare: () -> Unit,
    onTimer: () -> Unit,
    onExportPdf: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }

    val oreoWc = LocalOreoWindowSizeClass.current
    val topBarMaxWidth = when (oreoWc.widthSizeClass) {
        OreoWidthSizeClass.Compact  -> Dp.Infinity
        OreoWidthSizeClass.Medium   -> 720.dp
        OreoWidthSizeClass.Expanded -> 820.dp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(OreoPalette.Bg0),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = topBarMaxWidth)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ── Back ────────────────────────────────────────────
            // Icono solo, sin label pegado. Tint OnSurface: el back
            // es navegación neutra, no acción primaria.
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Volver",
                    tint = OreoPalette.OnSurface,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.width(4.dp))

            // ── Botón "▷ Lectura" ──────────────────────────────
            // Solo visible si la nota tiene contenido textual.
            if (onReaderMode != null) {
                IconButton(
                    onClick = onReaderMode!!,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "Modo lectura",
                        tint = OreoPalette.Accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(2.dp))
            }

            // ── Título + subtítulo ──────────────────────────────
            // Columna clickeable: al tocar enfoca el campo de título
            // del editor. Sin ripple visible — feedback solo por focus.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        onClick = onTitleClick,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    )
                    .semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Text(
                    text = when {
                        isEmpty -> "Nota vacía"
                        title.isBlank() -> "Nota sin título"
                        else -> title
                    },
                    color = if (title.isBlank() || isEmpty)
                        OreoPalette.OnSurfaceFaint
                    else
                        OreoPalette.OnSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = OreoPalette.OnSurfaceMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // ── Indicador de guardado ───────────────────────────
            SaveStatusIndicator(saveStatus = saveStatus)
            Spacer(Modifier.width(4.dp))

            // ── Buscar ──────────────────────────────────────────
            // AnimatedContent intercambia lupa ↔ close con scaleIn+fadeIn.
            // Cuando está abierto, el icono se tiñe de Accent para
            // señalizar visualmente el estado activo del panel.
            IconButton(
                onClick = onToggleFind,
                modifier = Modifier.size(40.dp),
            ) {
                AnimatedContent(
                    targetState = findOpen,
                    transitionSpec = {
                        (fadeIn(tween(180)) +
                            scaleIn(initialScale = 0.7f, animationSpec = tween(180))) togetherWith
                        (fadeOut(tween(180)) +
                            scaleOut(targetScale = 0.7f, animationSpec = tween(180)))
                    },
                    label = "searchToggle",
                ) { open ->
                    Icon(
                        imageVector = if (open) Icons.Outlined.Close else Icons.Outlined.Search,
                        contentDescription = if (open)
                            "Cerrar búsqueda"
                        else
                            "Buscar en la nota",
                        tint = if (open) OreoPalette.Accent else OreoPalette.OnSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            // ── Menú Oreo ───────────────────────────────────────
            EditorOreoMenu(
                expanded = menuOpen,
                onExpand = { menuOpen = true },
                onDismiss = { menuOpen = false },
                onReaderMode = onReaderMode,
                onTogglePin = {
                    menuOpen = false
                    onTogglePin()
                },
                onShare = {
                    menuOpen = false
                    onShare()
                },
                onTimer = {
                    menuOpen = false
                    onTimer()
                },
                onExportPdf = {
                    menuOpen = false
                    onExportPdf()
                },
                onDelete = {
                    menuOpen = false
                    onDelete()
                },
            )
        }
        } // cierra Box centrador

        // ── Línea divisoria inferior (scroll-aware) ────────────
        // Sólo visible cuando el contenido scrolleó al tope.
        // La transición de alpha va suave por animateFloatAsState
        // que controla `scrollFraction` desde EditorScreen.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .alpha(scrollFraction)
                .background(OreoPalette.OutlineFaint),
        )
    }
}

/**
 * Indicador sutil del estado de guardado que aparece a la izquierda
 * del cluster de acciones. Sólo visible mientras se está guardando
 * o durante 1.5s después de guardado (el ViewModel hace el fade-out
 * automático transicionando a Idle).
 *
 * - Saving: spinner chiquito (12dp CircularProgressIndicator) + "Guardando…".
 * - Saved:  check verde (OkFill) + "Guardado".
 */
@Composable
private fun SaveStatusIndicator(saveStatus: SaveStatus) {
    AnimatedVisibility(
        visible = saveStatus == SaveStatus.Saving || saveStatus == SaveStatus.Saved,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
    ) {
        // AnimatedContent interno para la transición suave entre
        // "Guardando…" (spinner) y "Guardado" (check verde).
        // La visibilidad general la controla AnimatedVisibility:
        // aparece con fadeIn cuando empieza a guardar, desaparece
        // con fadeOut cuando el ViewModel transiciona a Idle (1.5s).
        AnimatedContent(
            targetState = saveStatus,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn(tween(200))) togetherWith
                (slideOutHorizontally { -it } + fadeOut(tween(200)))
            },
            label = "saveIndicator",
        ) { status ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (status) {
                    SaveStatus.Saving -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = OreoPalette.Accent,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Guardando\u2026",
                            color = OreoPalette.Accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    SaveStatus.Saved -> {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = OreoPalette.OkFill,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Guardado",
                            color = OreoPalette.OkFill,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    else -> {} // Idle y Dirty no se muestran acá
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Menú Oreo (mismas opciones de siempre, ahora con icono en
// OnSurface para no competir con el acento del back).
// ═══════════════════════════════════════════════════════════════

@Composable
private fun EditorOreoMenu(
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    onReaderMode: (() -> Unit)? = null,
    onTogglePin: () -> Unit,
    onShare: () -> Unit,
    onTimer: () -> Unit,
    onExportPdf: () -> Unit,
    onDelete: () -> Unit,
) {
    Box {
        IconButton(onClick = onExpand) {
            OreoCookieIcon(
                size = 22.dp,
                tint = OreoPalette.OnSurface,
            )
        }
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme.copy(
                surface = OreoPalette.SurfaceCard,
                onSurface = OreoPalette.OnSurface,
            ),
            shapes = MaterialTheme.shapes.copy(
                extraSmall = RoundedCornerShape(18.dp),
            ),
        ) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = onDismiss,
                offset = DpOffset(x = (-4).dp, y = 4.dp),
                modifier = Modifier
                    .background(
                        color = OreoPalette.SurfaceCard,
                        shape = RoundedCornerShape(18.dp),
                    )
                    .padding(vertical = 4.dp),
            ) {
                if (onReaderMode != null) {
                    OreoMenuItem(
                        icon = Icons.Outlined.PlayArrow,
                        label = "Modo lectura",
                        onClick = { onReaderMode() },
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(OreoPalette.Outline.copy(alpha = 0.35f)),
                    )
                }
                OreoMenuItem(
                    icon = Icons.Outlined.PushPin,
                    label = "Fijar al tope",
                    onClick = onTogglePin,
                )
                OreoMenuItem(
                    icon = Icons.Outlined.Share,
                    label = "Compartir",
                    onClick = onShare,
                )
                OreoMenuItem(
                    icon = Icons.Outlined.Timer,
                    label = "Cronómetro",
                    onClick = onTimer,
                )
                OreoMenuItem(
                    icon = Icons.Outlined.PictureAsPdf,
                    label = "Exportar a PDF",
                    onClick = onExportPdf,
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(OreoPalette.Outline.copy(alpha = 0.35f)),
                )
                OreoMenuItem(
                    icon = Icons.Outlined.Delete,
                    label = "Eliminar nota",
                    onClick = onDelete,
                    destructive = true,
                )
            }
        }
    }
}

@Composable
private fun OreoMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val tint = if (destructive) OreoPalette.DangerFill else OreoPalette.Accent
    val textColor = if (destructive) OreoPalette.DangerFill else OreoPalette.OnSurface
    DropdownMenuItem(
        modifier = Modifier.padding(horizontal = 6.dp),
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        },
        text = {
            Text(
                text = label,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        },
        onClick = onClick,
    )
}
