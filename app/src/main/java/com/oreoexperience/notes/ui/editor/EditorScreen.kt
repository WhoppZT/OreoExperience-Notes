@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi::class,
)

package com.oreoexperience.notes.ui.editor

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import com.oreoexperience.notes.data.NoteBlock
import com.oreoexperience.notes.ui.LocalAppContainer
import com.oreoexperience.notes.ui.components.BottomTimerBar
import com.oreoexperience.notes.ui.components.MediaPreview
import com.oreoexperience.notes.ui.theme.OreoMotion
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SWIPE_BACK_THRESHOLD_DP = 80f
private const val SWIPE_BACK_EDGE_DP = 24f

/**
 * Editor estilo iOS Notes con identidad **OreoExperience Aurora**.
 *
 * Modelo basado en bloques: la nota es una columna de bloques de
 * texto y media intercalados. Cada bloque de texto tiene su propio
 * RichTextEditor; los bloques de media se renderizan inline con
 * preview a pantalla completa.
 *
 * Toolbar inferior: formato (B/I/U/S, encabezado, listas) + insertar
 * imagen / video desde el sistema. La inserción se hace en la posición
 * exacta del cursor.
 *
 * Swipe-back: arrastrar desde el borde izquierdo (>= 80 dp) dispara el
 * back, igual al gesto pull-to-back de iOS.
 */
@Composable
fun EditorScreen(
    discursoId: Long,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
) {
    val container = LocalAppContainer.current
    val mediaStorage = container.mediaStorage
    val vm: EditorViewModel = viewModel(
        factory = viewModelFactory {
            initializer { EditorViewModel(container.repository, mediaStorage) }
        }
    )
    LaunchedEffect(discursoId) { vm.load(discursoId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val ctxLocal = LocalContext.current
    var showDelete by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    // Track del bloque de texto enfocado para insertar media después.
    var focusedTextBlockId by remember { mutableStateOf<String?>(null) }
    var focusedCursorOffset by remember { mutableStateOf(-1) }

    // Pickers de media.
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val name = mediaStorage.importUri(uri, fallbackExt = "jpg") ?: return@launch
                vm.insertMediaAfter(
                    afterTextBlockId = focusedTextBlockId,
                    splitOffset = focusedCursorOffset,
                    media = NoteBlock.Image(fileName = name),
                )
            }
        }
    }
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val name = mediaStorage.importUri(uri, fallbackExt = "mp4") ?: return@launch
                vm.insertMediaAfter(
                    afterTextBlockId = focusedTextBlockId,
                    splitOffset = focusedCursorOffset,
                    media = NoteBlock.Video(fileName = name),
                )
            }
        }
    }

    suspend fun saveAndBack() {
        val newId = vm.save()
        onSaved(newId)
    }
    BackHandler {
        scope.launch { saveAndBack() }
    }

    // Track activo para que el botón "B/I/U..." opere sobre el bloque
    // enfocado.
    val activeStateRef = remember { mutableStateOf<RichTextState?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OreoPalette.Bg0)
            // Swipe-back desde el borde izquierdo (gesto iOS).
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
                            scope.launch { saveAndBack() }
                        }
                    },
                    onDragCancel = { /* nothing */ },
                )
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // Top bar estilo iOS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { scope.launch { saveAndBack() } }) {
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
                Spacer(Modifier.weight(1f))

                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreHoriz,
                            contentDescription = "Opciones",
                            tint = OreoPalette.Accent,
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        if (!state.isNew) {
                            DropdownMenuItem(
                                text = {
                                    Text(if (state.pinned) "Quitar fijado" else "Fijar al tope")
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.PushPin, null)
                                },
                                onClick = {
                                    menuOpen = false
                                    vm.togglePin()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Compartir") },
                                leadingIcon = { Icon(Icons.Outlined.Share, null) },
                                onClick = {
                                    menuOpen = false
                                    shareEditorState(context = ctxLocal, vmState = state)
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Establecer cronómetro") },
                            onClick = {
                                menuOpen = false
                                showTimerDialog = true
                            },
                        )
                        if (!state.isNew) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Eliminar nota",
                                        color = OreoPalette.DangerFill,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        null,
                                        tint = OreoPalette.DangerFill,
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    showDelete = true
                                },
                            )
                        }
                    }
                }

                TextButton(onClick = { scope.launch { saveAndBack() } }) {
                    Text(
                        text = "Listo",
                        color = OreoPalette.Accent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                    )
                }
            }

            // Cuerpo scrolleable: título + bloques.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
            ) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatNowDate(),
                    color = OreoPalette.OnSurfaceFaint,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
                BasicTextField(
                    value = state.title,
                    onValueChange = vm::setTitle,
                    textStyle = LocalTextStyle.current.copy(
                        color = OreoPalette.OnSurface,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp,
                    ),
                    cursorBrush = SolidColor(OreoPalette.Accent),
                    keyboardOptions = KeyboardOptions(),
                    keyboardActions = KeyboardActions(),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box {
                            if (state.title.isEmpty()) {
                                Text(
                                    text = "Título",
                                    color = OreoPalette.OnSurfaceFaint,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            inner()
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))

                // Render de cada bloque
                state.blocks.forEach { block ->
                    when (block) {
                        is NoteBlock.Text -> TextBlockEditor(
                            block = block,
                            initialMarkdown = block.markdown,
                            onMarkdownChange = { md -> vm.updateTextBlock(block.id, md) },
                            onFocused = { rts, cursor ->
                                focusedTextBlockId = block.id
                                focusedCursorOffset = cursor
                                activeStateRef.value = rts
                            },
                        )
                        is NoteBlock.Image -> MediaPreview(
                            fileName = block.fileName,
                            isVideo = false,
                            storage = mediaStorage,
                            onDelete = { vm.removeBlock(block.id) },
                        )
                        is NoteBlock.Video -> MediaPreview(
                            fileName = block.fileName,
                            isVideo = true,
                            storage = mediaStorage,
                            onDelete = { vm.removeBlock(block.id) },
                        )
                    }
                }
                Spacer(Modifier.height(120.dp))
            }

            AnimatedVisibility(
                visible = state.loaded && state.targetDurationSec > 0,
                enter = slideInVertically(
                    animationSpec = tween(320, easing = OreoMotion.EaseOut),
                    initialOffsetY = { it / 2 },
                ) + fadeIn(tween(180, easing = OreoMotion.EaseOut)),
                exit = slideOutVertically(
                    animationSpec = tween(240, easing = OreoMotion.EaseInOut),
                    targetOffsetY = { it },
                ) + fadeOut(tween(140, easing = OreoMotion.EaseInOut)),
            ) {
                BottomTimerBar(
                    targetSec = state.targetDurationSec,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Toolbar de formato + media
            FormatToolbar(
                activeState = activeStateRef.value,
                onPickImage = {
                    imagePicker.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        ),
                    )
                },
                onPickVideo = {
                    videoPicker.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.VideoOnly
                        ),
                    )
                },
            )
        }

        if (showTimerDialog) {
            TimerDurationDialog(
                initialMinutes = state.targetDurationSec / 60,
                onDismiss = { showTimerDialog = false },
                onConfirm = { minutes ->
                    vm.setTargetMinutes(minutes)
                    showTimerDialog = false
                },
            )
        }

        if (showDelete) {
            AlertDialog(
                onDismissRequest = { showDelete = false },
                title = { Text("Eliminar nota") },
                text = { Text("Esta acción no se puede deshacer.") },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            vm.deleteCurrent()
                            showDelete = false
                            onSaved(0L)
                        }
                    }) {
                        Text("Eliminar", color = OreoPalette.DangerFill)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDelete = false }) {
                        Text("Cancelar", color = OreoPalette.Accent)
                    }
                },
                containerColor = OreoPalette.SurfaceCard,
                titleContentColor = OreoPalette.OnSurface,
                textContentColor = OreoPalette.OnSurfaceMuted,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            )
        }
    }
}

/**
 * Editor de bloque de texto: un RichTextEditor que se sincroniza con
 * el ViewModel. Cada bloque tiene su propio state para evitar que se
 * peleen. El callback onFocused se dispara cuando el bloque toma el
 * foco — usamos eso para que el botón "Insertar imagen" sepa dónde
 * cortar.
 */
@Composable
private fun TextBlockEditor(
    block: NoteBlock.Text,
    initialMarkdown: String,
    onMarkdownChange: (String) -> Unit,
    onFocused: (RichTextState, Int) -> Unit,
) {
    val richState = rememberRichTextState()
    LaunchedEffect(block.id) {
        if (richState.toMarkdown() != initialMarkdown) {
            richState.setMarkdown(initialMarkdown)
        }
    }
    LaunchedEffect(richState) {
        snapshotFlow { richState.annotatedString }
            .drop(1)
            .distinctUntilChanged()
            .collect { onMarkdownChange(richState.toMarkdown()) }
    }
    // Re-emitir foco cuando cambia el cursor para que el insertor de
    // media tenga la posición actualizada.
    val cursor by remember(richState) {
        derivedStateOf { richState.selection.start }
    }
    LaunchedEffect(cursor) {
        onFocused(richState, cursor)
    }

    RichTextEditor(
        state = richState,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { fs ->
                if (fs.isFocused) onFocused(richState, richState.selection.start)
            },
        textStyle = LocalTextStyle.current.copy(
            color = OreoPalette.OnSurface,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Start,
        ),
        colors = RichTextEditorDefaults.richTextEditorColors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            cursorColor = OreoPalette.Accent,
            textColor = OreoPalette.OnSurface,
            containerColor = Color.Transparent,
        ),
        placeholder = {
            Text(
                text = "Empezá a escribir…",
                color = OreoPalette.OnSurfaceFaint,
                fontSize = 17.sp,
                textAlign = TextAlign.Start,
            )
        },
        contentPadding = PaddingValues(0.dp),
    )
}

@Composable
private fun FormatToolbar(
    activeState: RichTextState?,
    onPickImage: () -> Unit,
    onPickVideo: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(OreoPalette.Bg1)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // Insertar imagen / video — siempre disponibles.
            ToolbarButton(
                icon = Icons.Outlined.Image,
                description = "Insertar imagen",
                active = false,
                tintActive = OreoPalette.AccentSub,
                onClick = onPickImage,
            )
            ToolbarButton(
                icon = Icons.Outlined.Videocam,
                description = "Insertar video",
                active = false,
                tintActive = OreoPalette.AccentSub,
                onClick = onPickVideo,
            )
            VerticalDivider()
            ToolbarButton(
                icon = Icons.Outlined.FormatBold,
                description = "Negrita",
                active = activeState?.currentSpanStyle?.fontWeight == FontWeight.Bold,
                onClick = { activeState?.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
            )
            ToolbarButton(
                icon = Icons.Outlined.FormatItalic,
                description = "Cursiva",
                active = activeState?.currentSpanStyle?.fontStyle == FontStyle.Italic,
                onClick = { activeState?.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
            )
            ToolbarButton(
                icon = Icons.Outlined.FormatUnderlined,
                description = "Subrayado",
                active = activeState?.currentSpanStyle?.textDecoration?.contains(TextDecoration.Underline) == true,
                onClick = { activeState?.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) },
            )
            ToolbarButton(
                icon = Icons.Outlined.FormatStrikethrough,
                description = "Tachado",
                active = activeState?.currentSpanStyle?.textDecoration?.contains(TextDecoration.LineThrough) == true,
                onClick = { activeState?.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) },
            )
            VerticalDivider()
            ToolbarButton(
                icon = Icons.Outlined.Title,
                description = "Encabezado",
                active = activeState?.currentSpanStyle?.fontSize == 22.sp,
                onClick = {
                    activeState?.toggleSpanStyle(
                        SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                    )
                },
            )
            ToolbarButton(
                icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                description = "Lista",
                active = activeState?.isUnorderedList == true,
                onClick = { activeState?.toggleUnorderedList() },
            )
            ToolbarButton(
                icon = Icons.Outlined.FormatListNumbered,
                description = "Lista numerada",
                active = activeState?.isOrderedList == true,
                onClick = { activeState?.toggleOrderedList() },
            )
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit,
    tintActive: Color = OreoPalette.Accent,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(38.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (active) tintActive else OreoPalette.OnSurfaceMuted,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .height(20.dp)
            .width(1.dp)
            .background(OreoPalette.Outline),
    )
}

@Composable
private fun TimerDurationDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var text by remember { mutableStateOf(if (initialMinutes > 0) initialMinutes.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cronómetro de la nota") },
        text = {
            Column {
                Text(
                    text = "Duración objetivo en minutos. Dejá vacío o 0 para ocultar la barra.",
                    color = OreoPalette.OnSurfaceMuted,
                )
                Spacer(Modifier.height(12.dp))
                BasicTextField(
                    value = text,
                    onValueChange = { v -> text = v.filter { it.isDigit() }.take(4) },
                    textStyle = LocalTextStyle.current.copy(
                        color = OreoPalette.OnSurface,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    cursorBrush = SolidColor(OreoPalette.Accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    decorationBox = { inner ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (text.isEmpty()) {
                                    Text(
                                        "0",
                                        color = OreoPalette.OnSurfaceFaint,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                inner()
                            }
                            Text(
                                "min",
                                color = OreoPalette.OnSurfaceMuted,
                                fontSize = 16.sp,
                            )
                        }
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.toIntOrNull() ?: 0) }) {
                Text("Guardar", color = OreoPalette.Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = OreoPalette.OnSurfaceMuted)
            }
        },
        containerColor = OreoPalette.SurfaceCard,
        titleContentColor = OreoPalette.OnSurface,
        textContentColor = OreoPalette.OnSurfaceMuted,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    )
}

private fun formatNowDate(): String {
    val sdf = SimpleDateFormat("d 'de' MMMM 'de' yyyy 'a las' HH:mm", Locale("es"))
    return sdf.format(Date())
}

/**
 * Comparte la nota como texto plano (concatena los bloques de texto +
 * marcadores legibles para los media). El receptor (Drive, Gmail,
 * Telegram, etc.) lo abre como un text/plain común.
 */
private fun shareEditorState(
    context: android.content.Context,
    vmState: EditorUiState,
) {
    val title = vmState.title.ifBlank { "Nota" }
    val body = vmState.blocks.joinToString("\n\n") { b ->
        when (b) {
            is NoteBlock.Text -> b.markdown
            is NoteBlock.Image -> "[imagen: ${b.fileName}]"
            is NoteBlock.Video -> "[video: ${b.fileName}]"
        }
    }
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_SUBJECT, title)
        putExtra(android.content.Intent.EXTRA_TEXT, "$title\n\n$body")
    }
    context.startActivity(
        android.content.Intent.createChooser(intent, "Compartir nota"),
    )
}
