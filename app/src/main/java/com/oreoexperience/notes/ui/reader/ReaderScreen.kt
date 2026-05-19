@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi::class,
)

package com.oreoexperience.notes.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.oreoexperience.notes.data.NoteBlock
import com.oreoexperience.notes.ui.LocalAppContainer
import com.oreoexperience.notes.ui.components.BottomTimerBar
import com.oreoexperience.notes.ui.components.MediaPreview
import com.oreoexperience.notes.ui.theme.OreoPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SWIPE_BACK_THRESHOLD_DP = 80f
private const val SWIPE_BACK_EDGE_DP = 24f

/**
 * Modo **lectura / presentación**: muestra la nota en solo-lectura,
 * sin toolbar de formato, sin botón "Eliminar", sin botón de insertar
 * media. El cronómetro aparece si la nota tiene `targetDurationSec`
 * configurado.
 *
 * Pensado para predicar/presentar: minimizar lo que distrae y
 * maximizar el contenido. El usuario puede ir a editar con el botón
 * "Editar" arriba a la derecha (que abre el editor sobre esta misma
 * stack), o salir con el botón "Atrás" / swipe desde el borde
 * izquierdo (gesto iOS).
 */
@Composable
fun ReaderScreen(
    discursoId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val container = LocalAppContainer.current
    val mediaStorage = container.mediaStorage
    val vm: ReaderViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ReaderViewModel(container.repository) }
        },
    )
    LaunchedEffect(discursoId) { vm.load(discursoId) }
    val state by vm.state.collectAsStateWithLifecycle()

    BackHandler { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OreoPalette.Bg0)
            .pointerInput(Unit) {
                val edgePx = SWIPE_BACK_EDGE_DP.dp.toPx()
                val thresholdPx = SWIPE_BACK_THRESHOLD_DP.dp.toPx()
                var startX = 0f
                var totalDelta = 0f
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        startX = offset.x
                        totalDelta = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        if (startX <= edgePx) {
                            totalDelta += dragAmount
                        }
                    },
                    onDragEnd = {
                        if (startX <= edgePx && totalDelta >= thresholdPx) {
                            onBack()
                        }
                    },
                    onDragCancel = { },
                )
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            ReaderTopBar(
                onBack = onBack,
                onEdit = { onEdit(discursoId) },
                canEdit = state.note != null,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
            ) {
                Spacer(Modifier.height(4.dp))
                val note = state.note
                if (note != null) {
                    Text(
                        text = formatDate(note.updatedAt),
                        color = OreoPalette.OnSurfaceFaint,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    )
                    Text(
                        text = note.title.ifBlank { "Sin título" },
                        color = OreoPalette.OnSurface,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    state.blocks.forEach { block ->
                        when (block) {
                            is NoteBlock.Text -> ReaderTextBlock(markdown = block.markdown)
                            is NoteBlock.Image -> MediaPreview(
                                fileName = block.fileName,
                                isVideo = false,
                                storage = mediaStorage,
                                onDelete = { /* sin edición en lectura */ },
                            )
                            is NoteBlock.Video -> MediaPreview(
                                fileName = block.fileName,
                                isVideo = true,
                                storage = mediaStorage,
                                onDelete = { /* sin edición en lectura */ },
                            )
                        }
                    }
                    Spacer(Modifier.height(160.dp))
                }
            }

            // Cronómetro inferior idéntico al del editor: visible sólo
            // si la nota tiene objetivo configurado.
            AnimatedVisibility(
                visible = (state.note?.targetDurationSec ?: 0) > 0,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    initialOffsetY = { it },
                ) + fadeIn(),
                exit = slideOutVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    targetOffsetY = { it },
                ) + fadeOut(),
            ) {
                BottomTimerBar(
                    targetSec = state.note?.targetDurationSec ?: 0,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                )
            }
        }
    }
}

@Composable
private fun ReaderTopBar(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    canEdit: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = null,
                tint = OreoPalette.Accent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = "Notas",
                color = OreoPalette.Accent,
                fontSize = 17.sp,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Lectura",
                color = OreoPalette.OnSurfaceMuted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onEdit,
                enabled = canEdit,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Editar",
                    tint = if (canEdit) OreoPalette.Accent else OreoPalette.OnSurfaceFaint,
                )
            }
        }
    }
}

/**
 * Renderiza un bloque de texto en solo-lectura. Convertimos el
 * markdown a AnnotatedString usando el mismo motor (RichTextState)
 * para que la tipografía y los estilos coincidan con el editor.
 *
 * No hacemos `RichTextEditor(enabled = false)` porque eso sigue
 * mostrando un campo de texto enfocable; en su lugar usamos el
 * `Text` nativo, que es ligero y no captura foco.
 */
@Composable
private fun ReaderTextBlock(markdown: String) {
    val state = rememberRichTextState()
    LaunchedEffect(markdown) {
        if (state.toMarkdown() != markdown) state.setMarkdown(markdown)
    }
    if (markdown.isBlank()) return
    Text(
        text = state.annotatedString,
        color = OreoPalette.OnSurface,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

private fun formatDate(ts: Long): String {
    val fmt = SimpleDateFormat("d MMM yyyy 'a las' HH:mm", Locale("es"))
    return fmt.format(Date(ts))
}
