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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Timer
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.oreoexperience.notes.ui.components.OreoCookieIcon
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
    initialCategoryKey: String? = null,
) {
    val container = LocalAppContainer.current
    val mediaStorage = container.mediaStorage
    val vm: EditorViewModel = viewModel(
        factory = viewModelFactory {
            initializer { EditorViewModel(container.repository, mediaStorage) }
        }
    )
    val pdfExportManager = container.pdfExportManager
    val repository = container.repository
    LaunchedEffect(discursoId, initialCategoryKey) {
        vm.load(discursoId, initialCategoryKey)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val ctxLocal = LocalContext.current
    var showDelete by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    // SAF launcher para exportar la nota actual a PDF.
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            // Aseguramos que la versión persistida esté al día antes de exportar.
            val id = runCatching { vm.saveBlocking() }.getOrNull() ?: state.id
            val effectiveId = if (id > 0L) id else state.id
            val discurso = runCatching { repository.get(effectiveId) }.getOrNull()
            if (discurso == null) {
                android.widget.Toast.makeText(
                    ctxLocal,
                    "No se pudo exportar la nota",
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
                return@launch
            }
            runCatching {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    pdfExportManager.export(
                        discursos = listOf(discurso),
                        title = discurso.title.ifBlank { "Nota" },
                        outUri = uri,
                    )
                }
            }.onSuccess { pages ->
                android.widget.Toast.makeText(
                    ctxLocal,
                    "PDF exportado · $pages página${if (pages > 1) "s" else ""}",
                    android.widget.Toast.LENGTH_LONG,
                ).show()
            }.onFailure { err ->
                android.widget.Toast.makeText(
                    ctxLocal,
                    "No se pudo exportar: ${err.message ?: "error"}",
                    android.widget.Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    // Track del bloque de texto enfocado para insertar media después.
    var focusedTextBlockId by remember { mutableStateOf<String?>(null) }
    var focusedCursorOffset by remember { mutableStateOf(-1) }

    // Map de FocusRequester y RichTextState por bloque. Lo usamos para
    // que al tocar el espacio libre (debajo de todos los bloques o
    // entre bloque y bloque) el cursor caiga en el bloque de texto más
    // cercano, en vez de quedarse trabado donde estaba.
    val blockFocusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }
    val blockStates = remember { mutableStateMapOf<String, RichTextState>() }
    fun focusBlockAtEnd(id: String) {
        val rts = blockStates[id]
        if (rts != null) {
            val len = rts.annotatedString.length
            rts.selection = TextRange(len)
        }
        blockFocusRequesters[id]?.requestFocus()
    }

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
        val newId = vm.saveBlocking()
        onSaved(newId)
    }
    BackHandler {
        scope.launch { saveAndBack() }
    }

    // Flush en cualquier evento ON_PAUSE — si el usuario manda la app
    // al background o cierra de cualquier forma, este observer dispara
    // un save inmediato. Es la red de seguridad final encima del
    // auto-save que ya corre cada ~250ms.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                vm.saveNow()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
                Spacer(Modifier.width(6.dp))
                // Indicador de estado de guardado visible al lado del back —
                // muestra "Sin guardar / Guardando / Guardado".
                SaveStatusPill(status = state.saveStatus)
                Spacer(Modifier.weight(1f))

                EditorOreoMenu(
                    expanded = menuOpen,
                    pinned = state.pinned,
                    onExpand = { menuOpen = true },
                    onDismiss = { menuOpen = false },
                    onTogglePin = {
                        menuOpen = false
                        vm.togglePin()
                    },
                    onShare = {
                        menuOpen = false
                        shareEditorState(context = ctxLocal, vmState = state)
                    },
                    onTimer = {
                        menuOpen = false
                        showTimerDialog = true
                    },
                    onExportPdf = {
                        menuOpen = false
                        val safeTitle = state.title
                            .ifBlank { "nota" }
                            .lowercase()
                            .replace(" ", "_")
                            .replace("/", "-")
                            .take(40)
                        pdfLauncher.launch("oreo_${safeTitle}.pdf")
                    },
                    onDelete = {
                        menuOpen = false
                        showDelete = true
                    },
                )
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
                state.blocks.forEachIndexed { idx, block ->
                    when (block) {
                        is NoteBlock.Text -> {
                            val fr = blockFocusRequesters.getOrPut(block.id) { FocusRequester() }
                            TextBlockEditor(
                                block = block,
                                initialMarkdown = block.markdown,
                                focusRequester = fr,
                                onMarkdownChange = { md -> vm.updateTextBlock(block.id, md) },
                                onFocused = { rts, cursor ->
                                    focusedTextBlockId = block.id
                                    focusedCursorOffset = cursor
                                    activeStateRef.value = rts
                                    blockStates[block.id] = rts
                                },
                            )
                        }
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
                    // Spacer entre bloques. Cuando el usuario toca este
                    // área (que de otra forma se vería como un "hueco"
                    // entre párrafos), enfocamos el bloque de texto más
                    // cercano hacia arriba o, si no hay, el más cercano
                    // hacia abajo.
                    val nearestTextId: String? = remember(state.blocks, idx) {
                        val before = state.blocks.subList(0, idx + 1)
                            .asReversed()
                            .firstNotNullOfOrNull { (it as? NoteBlock.Text)?.id }
                        before ?: state.blocks.drop(idx + 1)
                            .firstNotNullOfOrNull { (it as? NoteBlock.Text)?.id }
                    }
                    Spacer(
                        modifier = Modifier
                            .height(10.dp)
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    nearestTextId?.let { focusBlockAtEnd(it) }
                                },
                            ),
                    )
                }
                // Área libre al final del cuerpo. Al tocarla, el cursor
                // va al final del último bloque de texto y aparece el
                // teclado, como en iOS Notes.
                Spacer(
                    modifier = Modifier
                        .height(120.dp)
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                val lastTextId = state.blocks
                                    .filterIsInstance<NoteBlock.Text>()
                                    .lastOrNull()?.id
                                lastTextId?.let { focusBlockAtEnd(it) }
                            },
                        ),
                )
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
                    onClose = { vm.setTargetMinutes(0) },
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
                onClear = {
                    vm.setTargetMinutes(0)
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
    focusRequester: FocusRequester,
    onMarkdownChange: (String) -> Unit,
    onFocused: (RichTextState, Int) -> Unit,
) {
    val richState = rememberRichTextState()
    // Mantenemos el callback siempre actualizado — sin esto el
    // LaunchedEffect(richState) capturaba el `onMarkdownChange`
    // inicial y nunca lo refrescaba. Cuando el VM reemplazaba el
    // bloque por otro con UUID nuevo (caso load() que reasigna
    // state.blocks), el callback seguía llamando a updateTextBlock
    // con el UUID viejo → nada matcheaba → nada se guardaba.
    val currentOnChange by rememberUpdatedState(onMarkdownChange)
    LaunchedEffect(block.id) {
        if (richState.toMarkdown() != initialMarkdown) {
            richState.setMarkdown(initialMarkdown)
        }
    }
    LaunchedEffect(richState) {
        snapshotFlow { richState.annotatedString }
            .drop(1)
            .distinctUntilChanged()
            .collect { currentOnChange(richState.toMarkdown()) }
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
            .focusRequester(focusRequester)
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
            FontSizeToolbarButton(activeState = activeState)
            HighlightToolbarButton(activeState = activeState)
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
    // El botón del toolbar muestra una píldora redondeada cuando está
    // activo (estilo iOS Notes). Animamos color de fondo + tint con
    // springs para que el toggle se sienta vivo.
    val bgColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (active) tintActive.copy(alpha = 0.20f) else Color.Transparent,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 220,
            easing = OreoMotion.EaseOut,
        ),
        label = "toolbarBtnBg",
    )
    val tint by androidx.compose.animation.animateColorAsState(
        targetValue = if (active) tintActive else OreoPalette.OnSurfaceMuted,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 220,
            easing = OreoMotion.EaseOut,
        ),
        label = "toolbarBtnTint",
    )
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = OreoMotion.SpringBouncy(),
        label = "toolbarBtnScale",
    )
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(38.dp)
            .scale(scale),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    color = bgColor,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                ),
        )
        IconButton(
            onClick = onClick,
            interactionSource = interaction,
            modifier = Modifier.size(38.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Botón "T" que cicla tamaño de letra y, cada vez que cambia el nivel,
 * hace un pequeño pulso de escala. Visualiza el cambio: subió un
 * paso → la T rebota brevemente para confirmar el efecto.
 */
@Composable
private fun FontSizeToolbarButton(activeState: RichTextState?) {
    val level = currentFontLevel(activeState)
    val pulse = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(level) {
        pulse.snapTo(1.22f)
        pulse.animateTo(1f, animationSpec = OreoMotion.SpringBouncy())
    }
    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = pulse.value
            scaleY = pulse.value
        },
    ) {
        ToolbarButton(
            icon = Icons.Outlined.Title,
            description = "Tamaño de letra (x1 → x2 → x3 → x4 → x1)",
            active = level > 0,
            onClick = { cycleFontSize(activeState) },
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
    onClear: () -> Unit,
) {
    var text by remember { mutableStateOf(if (initialMinutes > 0) initialMinutes.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cronómetro de la nota") },
        text = {
            Column {
                Text(
                    text = "Duración objetivo en minutos. Si la nota ya tiene cronómetro y querés sacarlo, tocá \"Eliminar\".",
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (initialMinutes > 0) {
                    TextButton(onClick = onClear) {
                        Text("Eliminar", color = OreoPalette.DangerFill)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = OreoPalette.OnSurfaceMuted)
                }
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
 * Píldora animada en la top bar que muestra el estado del auto-save:
 *  - `Idle`: nada visible.
 *  - `Dirty`: punto naranja con "Cambios sin guardar".
 *  - `Saving`: icono Sync con rotación + "Guardando…".
 *  - `Saved`: check verde con "Guardado" — visible 1.5s.
 */
@Composable
private fun SaveStatusPill(status: SaveStatus) {
    // Pulso de brillo cuando el estado pasa a Saved — una corona violeta
    // que se expande y se desvanece sobre la píldora.
    val pulseScale = remember { androidx.compose.animation.core.Animatable(1f) }
    val pulseAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(status) {
        if (status == SaveStatus.Saved) {
            pulseScale.snapTo(0.9f)
            pulseAlpha.snapTo(0.55f)
            kotlinx.coroutines.coroutineScope {
                launch { pulseScale.animateTo(1.45f, tween(560, easing = OreoMotion.EaseOut)) }
                launch { pulseAlpha.animateTo(0f, tween(620, easing = OreoMotion.EaseInOut)) }
            }
        }
    }
    AnimatedVisibility(
        visible = status != SaveStatus.Idle,
        enter = fadeIn(tween(180, easing = OreoMotion.EaseOut)) +
            slideInVertically(
                animationSpec = OreoMotion.SpringBouncy(),
                initialOffsetY = { -it / 2 },
            ),
        exit = fadeOut(tween(180, easing = OreoMotion.EaseInOut)) +
            slideOutVertically(
                animationSpec = tween(160, easing = OreoMotion.EaseInOut),
                targetOffsetY = { -it / 2 },
            ),
    ) {
        val (icon, text, tint) = when (status) {
            SaveStatus.Dirty -> Triple(
                Icons.Outlined.CloudDone,
                "Sin guardar",
                OreoPalette.OnSurfaceFaint,
            )
            SaveStatus.Saving -> Triple(
                Icons.Outlined.Sync,
                "Guardando",
                OreoPalette.Accent,
            )
            SaveStatus.Saved -> Triple(
                Icons.Outlined.Check,
                "Guardado",
                OreoPalette.Accent,
            )
            SaveStatus.Idle -> Triple(
                Icons.Outlined.Check,
                "",
                OreoPalette.OnSurfaceFaint,
            )
        }
        // Rotación continua del icono de sync mientras guarda.
        val infinite = rememberInfiniteTransition(label = "saving")
        val rotation: Float by infinite.animateFloat(
            initialValue = 0f,
            targetValue = if (status == SaveStatus.Saving) 360f else 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
            ),
            label = "saveRot",
        )
        Box(contentAlignment = Alignment.Center) {
            // Corona del pulso — dibuja un halo expansivo cuando el
            // estado se acaba de marcar como Guardado.
            if (pulseAlpha.value > 0.01f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            scaleX = pulseScale.value
                            scaleY = pulseScale.value
                            alpha = pulseAlpha.value
                        }
                        .background(
                            color = OreoPalette.Accent.copy(alpha = 0.35f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                        ),
                )
            }
            Row(
                modifier = Modifier
                    .background(
                        color = tint.copy(alpha = 0.12f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier
                        .size(14.dp)
                        .let {
                            if (rotation != 0f) it.rotate(rotation) else it
                        },
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = text,
                    color = tint,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
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

/**
 * Menú "galleta Oreo" del editor de notas. Reemplaza el clásico
 * trío "···" con un icono dibujado a mano que evoca una Oreo, y
 * presenta las acciones (fijar, compartir, cronómetro, eliminar)
 * dentro de un popup más redondeado, con padding generoso, divisor
 * sutil para la acción destructiva y *leading icon* en cada item.
 */
@Composable
private fun EditorOreoMenu(
    expanded: Boolean,
    pinned: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
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
                tint = OreoPalette.Accent,
            )
        }
        androidx.compose.material3.MaterialTheme(
            colorScheme = androidx.compose.material3.MaterialTheme.colorScheme.copy(
                surface = OreoPalette.SurfaceCard,
                onSurface = OreoPalette.OnSurface,
            ),
            shapes = androidx.compose.material3.MaterialTheme.shapes.copy(
                extraSmall = RoundedCornerShape(18.dp),
            ),
        ) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = onDismiss,
                offset = androidx.compose.ui.unit.DpOffset(x = (-4).dp, y = 4.dp),
                modifier = Modifier
                    .background(
                        color = OreoPalette.SurfaceCard,
                        shape = RoundedCornerShape(18.dp),
                    )
                    .padding(vertical = 4.dp),
            ) {
                OreoMenuItem(
                    icon = Icons.Outlined.PushPin,
                    label = if (pinned) "Quitar fijado" else "Fijar al tope",
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

// ---------------------------------------------------------------------------
// Toolbar helpers: tamaño de letra cíclico + resaltado de colores
// ---------------------------------------------------------------------------

/**
 * Niveles de tamaño de letra que cicla el botón "T" del toolbar.
 * Index 0 = "x1" = sin SpanStyle (tamaño por defecto del bloque, 17 sp).
 * Los siguientes niveles aplican un fontSize explícito vía SpanStyle.
 */
private val FONT_LEVELS_SP = listOf(17.sp, 22.sp, 28.sp, 34.sp)

/**
 * Devuelve el índice del nivel actual de tamaño de letra (0..3) leyendo
 * `currentSpanStyle.fontSize`. 0 = default; 1..3 = x2/x3/x4. Si el span
 * tiene un fontSize que no coincide con ninguno de los niveles, lo
 * tratamos como "default" para no romper la cycle.
 */
private fun currentFontLevel(state: RichTextState?): Int {
    val size = state?.currentSpanStyle?.fontSize ?: return 0
    if (size == TextUnit.Unspecified) return 0
    val idx = FONT_LEVELS_SP.indexOf(size)
    return if (idx < 0) 0 else idx
}

/**
 * Cicla el tamaño de letra: x1 → x2 → x3 → x4 → x1.
 * Quita el span de tamaño actual (si lo hubiese) y aplica el siguiente.
 */
private fun cycleFontSize(state: RichTextState?) {
    val s = state ?: return
    val cur = currentFontLevel(s)
    val next = (cur + 1) % FONT_LEVELS_SP.size
    // Quitar el tamaño actual (solo si no es default).
    if (cur > 0) {
        s.removeSpanStyle(SpanStyle(fontSize = FONT_LEVELS_SP[cur]))
    }
    // Aplicar el siguiente (solo si no es default).
    if (next > 0) {
        s.addSpanStyle(SpanStyle(fontSize = FONT_LEVELS_SP[next]))
    }
}

/** Paleta de resaltado — violeta Aurora, amarillo cálido, verde menta. */
private val HIGHLIGHT_COLORS = listOf(
    Color(0xFF9333EA) to "Violeta",
    Color(0xFFFACC15) to "Amarillo",
    Color(0xFF22C55E) to "Verde",
)

/**
 * Botón "resaltador" del toolbar: muestra un icono de marcador y, al
 * tocarlo, abre un popup pequeño con 3 colores (violeta / amarillo /
 * verde) más un swatch tachado para quitar el resaltado actual.
 *
 * El resaltado se persiste como `SpanStyle(background = color)` sobre
 * la selección. Si ya hay un background, primero se quita para no
 * acumular spans solapados.
 */
@Composable
private fun HighlightToolbarButton(activeState: RichTextState?) {
    var open by remember { mutableStateOf(false) }
    val currentBg = activeState?.currentSpanStyle?.background ?: Color.Unspecified
    val hasHighlight = currentBg != Color.Unspecified && currentBg != Color.Transparent
    Box {
        ToolbarButton(
            icon = Icons.Outlined.BorderColor,
            description = "Resaltar texto",
            active = hasHighlight,
            tintActive = if (hasHighlight) currentBg else OreoPalette.Accent,
            onClick = { open = !open },
        )
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            offset = androidx.compose.ui.unit.DpOffset(x = 0.dp, y = (-8).dp),
            modifier = Modifier
                .background(
                    color = OreoPalette.SurfaceCard,
                    shape = RoundedCornerShape(18.dp),
                ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HIGHLIGHT_COLORS.forEach { (color, label) ->
                    HighlightSwatch(
                        color = color,
                        label = label,
                        selected = hasHighlight && currentBg == color,
                        onClick = {
                            applyHighlight(activeState, color)
                            open = false
                        },
                    )
                }
                HighlightSwatch(
                    color = Color.Transparent,
                    label = "Quitar",
                    selected = false,
                    isClear = true,
                    onClick = {
                        clearHighlight(activeState)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun HighlightSwatch(
    color: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    isClear: Boolean = false,
) {
    // Onda expansiva que sale del swatch al tocarlo. Cada toque la
    // reinicia con un nuevo "trigger" que dispara el LaunchedEffect.
    val ringScale = remember { androidx.compose.animation.core.Animatable(1f) }
    val ringAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    var rippleTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(rippleTrigger) {
        if (rippleTrigger > 0) {
            ringScale.snapTo(1f)
            ringAlpha.snapTo(0.55f)
            kotlinx.coroutines.coroutineScope {
                launch { ringScale.animateTo(2.0f, tween(520, easing = OreoMotion.EaseOut)) }
                launch { ringAlpha.animateTo(0f, tween(560, easing = OreoMotion.EaseInOut)) }
            }
        }
    }
    Box(contentAlignment = Alignment.Center) {
        // Anillo expansivo del ripple, dibujado detrás del swatch.
        if (!isClear && ringAlpha.value > 0.01f) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .graphicsLayer {
                        scaleX = ringScale.value
                        scaleY = ringScale.value
                        alpha = ringAlpha.value
                    }
                    .border(
                        width = 2.dp,
                        color = color,
                        shape = CircleShape,
                    ),
            )
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    color = if (isClear) OreoPalette.SurfaceCardHi else color,
                    shape = CircleShape,
                )
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) OreoPalette.OnSurface else OreoPalette.Outline,
                    shape = CircleShape,
                )
                .clickable {
                    rippleTrigger += 1
                    onClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            if (isClear) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = label,
                    tint = OreoPalette.OnSurfaceMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private fun applyHighlight(state: RichTextState?, color: Color) {
    val s = state ?: return
    val current = s.currentSpanStyle.background
    if (current != Color.Unspecified && current != Color.Transparent) {
        s.removeSpanStyle(SpanStyle(background = current))
    }
    s.addSpanStyle(SpanStyle(background = color))
}

private fun clearHighlight(state: RichTextState?) {
    val s = state ?: return
    val current = s.currentSpanStyle.background
    if (current != Color.Unspecified && current != Color.Transparent) {
        s.removeSpanStyle(SpanStyle(background = current))
    }
}
