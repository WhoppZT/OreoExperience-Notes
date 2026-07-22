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
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FormatAlignCenter
import androidx.compose.material.icons.outlined.FormatAlignLeft
import androidx.compose.material.icons.outlined.FormatAlignRight
import androidx.compose.material.icons.outlined.FormatColorText
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import androidx.compose.ui.unit.Dp
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
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import com.oreoexperience.notes.data.ai.AiChunk
import com.oreoexperience.notes.data.ai.AiService
import com.oreoexperience.notes.data.ai.RewriteTone
import com.oreoexperience.notes.data.ChecklistItem
import com.oreoexperience.notes.data.NoteBlock
import com.oreoexperience.notes.ui.LocalAppContainer
import com.oreoexperience.notes.ui.LocalOreoWindowSizeClass
import com.oreoexperience.notes.ui.OreoWidthSizeClass
import com.oreoexperience.notes.ui.components.OreoTimer
import com.oreoexperience.notes.ui.components.OreoTimerVariant
import com.oreoexperience.notes.ui.components.EditorTopBar
import com.oreoexperience.notes.ui.components.MediaPreview
import com.oreoexperience.notes.ui.theme.OreoMotion
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


/**
 * Convierte solo **bold** a <strong> para setHtml().
 * No agrega <p> para evitar el crash al pegar texto.
 */
private fun markdownToSimpleHtml(md: String): String {
    if (md.isBlank()) return ""
    return md
        .replace(Regex("\\*\\*(.+?)\\*\\*"), "<strong>\$1</strong>")
        .replace(Regex("__(.+?)__"), "<strong>\$1</strong>")
}

private const val SWIPE_BACK_THRESHOLD_DP = 80f
private const val SWIPE_BACK_EDGE_DP = 24f

/**
 * Color del highlight de la coincidencia actual de Find & Replace.
 * Aurora violeta con 35% de opacidad para que el texto siga legible.
 */
private val HIGHLIGHT_COLOR = OreoPalette.Accent.copy(alpha = 0.35f)

/**
 * Rango sobre el que está aplicado el span de highlight en este momento.
 * Se usa para poder limpiar exactamente ese rango antes de aplicar el
 * próximo, sin afectar otros backgrounds que el usuario haya puesto.
 */
private data class HighlightRange(
    val blockId: String,
    val start: Int,
    val end: Int,
)

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
    onReaderMode: (Long) -> Unit = {},
) {
    val container = LocalAppContainer.current
    val mediaStorage = container.mediaStorage
    val vm: EditorViewModel = viewModel(
        factory = viewModelFactory {
            initializer { EditorViewModel(container.repository, mediaStorage) }
        }
    )
    val pdfExportManager = container.pdfExportManager
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = container.repository
    val oreoWc = LocalOreoWindowSizeClass.current
    val editorHorizontalPadding = when {
        oreoWc.isExpanded -> 64.dp
        oreoWc.isMedium -> 32.dp
        else -> 18.dp
    }

    LaunchedEffect(discursoId, initialCategoryKey) {
        vm.load(discursoId, initialCategoryKey)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val ctxLocal = LocalContext.current
    var showDelete by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }

    // IA — datos capturados al momento de disparar la acción
    var aiSheetData by remember { mutableStateOf<AiSheetData?>(null) }

    // Find & Replace — estado delegado al ViewModel
    val findState by vm.findUiState.collectAsStateWithLifecycle()

    // Matches calculados en la UI usando el texto real del RichTextState
    var findMatches by remember { mutableStateOf(emptyList<FindMatch>()) }
    var findRegexError by remember { mutableStateOf(false) }

    // Contador que se incrementa cada vez que un bloque registra/actualiza
    // su RichTextState. Lo usamos como key del LaunchedEffect que recompute
    // matches: cuando un bloque se monta tarde (caso típico del LazyColumn)
    // queremos volver a buscar en él.
    var blockStatesVersion by remember { mutableIntStateOf(0) }

    // Highlight visual del match actual: guardamos el rango sobre el que
    // aplicamos el SpanStyle de fondo para poder limpiarlo cuando cambia
    // el match o se cierra el panel. Al usar addSpanStyle(spanStyle, range)
    // de compose-rich-editor (rc11), el highlight NO modifica el HTML
    // persistido si lo quitamos antes del próximo toHtml() — pero igual
    // preferimos quitarlo siempre antes de hacer save.
    var activeHighlight by remember { mutableStateOf<HighlightRange?>(null) }

    // Token que se incrementa cada vez que el usuario hace una acción
    // explícita de "saltar al match" (Enter en el campo de búsqueda, o
    // tap en next/prev). El LaunchedEffect del highlight observa este
    // token, no el query: así, mientras tipeás en el campo de búsqueda
    // el contador se actualiza en vivo pero NO te roba el foco saltando
    // al primer match. El highlight aparece sólo cuando vos lo pedís.
    var highlightTrigger by remember { mutableIntStateOf(0) }

    // Flag para suprimir el guardado automático mientras estamos
    // aplicando/quitando el highlight visual del match actual. Si no lo
    // hiciéramos, el snapshotFlow del RichTextState dispararía
    // updateTextBlock con un HTML que incluye el span de highlight
    // (background-color), y eso terminaría persistido en disco.
    val suppressAutoUpdate = remember { mutableStateOf(false) }
    // Settings de exportación pendientes que se aplican cuando el SAF
    // launcher devuelve el URI elegido por el usuario. Default = lo
    // que está guardado en preferencias.
    var pendingExportSettings by remember {
        mutableStateOf(container.userPreferences.pdfExportSettings())
    }

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
                        settings = pendingExportSettings,
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
    // Capturas del focused state al momento de lanzar el picker,
    // para que no se pierdan cuando el picker abre y el foco cambia.
    var capturedBlockId by remember { mutableStateOf<String?>(null) }
    var capturedCursorOffset by remember { mutableStateOf(-1) }

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

    // Recalcular matches usando el texto real del RichTextState.
    // Depende también de `blockStatesVersion` para reaccionar cuando un
    // bloque se monta tarde y registra su estado por primera vez.
    LaunchedEffect(
        findState.query,
        findState.caseSensitive,
        findState.useRegex,
        state.blocks,
        blockStatesVersion,
    ) {
        // Debounce de 500ms para que el contador no se actualice en
        // cada keystroke mientras el usuario escribe una frase. Si el
        // usuario sigue tecleando antes de que se cumpla el delay, el
        // LaunchedEffect se cancela y se reinicia.
        delay(500)

        val query = findState.query
        if (query.isBlank()) {
            findMatches = emptyList()
            findRegexError = false
            return@LaunchedEffect
        }

        val cs = findState.caseSensitive
        val ur = findState.useRegex

        // Validar que el regex (cuando está activo) compile. Si no,
        // marcamos error y limpiamos matches; FindReplaceEngine también
        // valida pero queremos detectarlo antes para mostrar feedback.
        val regexCompiles = runCatching {
            if (ur) Regex(query) else Regex(Regex.escape(query))
        }.isSuccess
        if (ur && !regexCompiles) {
            findRegexError = true
            findMatches = emptyList()
            return@LaunchedEffect
        }
        findRegexError = false

        // Construir mapa blockId -> texto plano del RichTextState. Si un
        // bloque aún no tiene su estado registrado, lo salteamos; el
        // próximo recompute (cuando blockStatesVersion cambie) lo va a
        // incluir.
        val blockTexts = mutableMapOf<String, String>()
        for (block in state.blocks) {
            if (block is NoteBlock.Text) {
                val rs = blockStates[block.id] ?: continue
                blockTexts[block.id] = rs.annotatedString.text
            }
        }

        val (results, regexErr) = FindReplaceEngine.computeMatches(
            blocks = state.blocks,
            blockTexts = blockTexts,
            query = query,
            caseSensitive = cs,
            useRegex = ur,
        )
        findRegexError = regexErr
        findMatches = results
    }

    // Índice efectivo (clamp por si la cantidad de matches se redujo).
    val effectiveFindIndex = findState.currentIndex.coerceAtMost(
        findMatches.lastIndex.coerceAtLeast(0),
    )

    // Highlight del match actual. Se ejecuta sólo cuando:
    //  - El panel se abre o cierra.
    //  - El usuario presiona Enter en el campo de búsqueda o tap en
    //    las flechas next/prev (incrementa `highlightTrigger`).
    //  - El índice de match actual cambia (después de un reemplazo, etc.).
    //
    // Importante: NO depende de `findMatches` directamente. Si lo hiciera,
    // cada keystroke recomputaría matches → cambiaría la lista → el effect
    // saltaría al primer match y le robaría el foco al campo de búsqueda.
    LaunchedEffect(highlightTrigger, effectiveFindIndex, findState.open) {
        suppressAutoUpdate.value = true

        // Limpiar el highlight previo si existe.
        activeHighlight?.let { prev ->
            blockStates[prev.blockId]?.removeSpanStyle(
                spanStyle = SpanStyle(background = HIGHLIGHT_COLOR),
                textRange = TextRange(prev.start, prev.end),
            )
        }
        activeHighlight = null

        if (!findState.open || findMatches.isEmpty()) {
            delay(60)
            suppressAutoUpdate.value = false
            return@LaunchedEffect
        }

        val m = findMatches.getOrNull(effectiveFindIndex)
        if (m == null) {
            delay(60)
            suppressAutoUpdate.value = false
            return@LaunchedEffect
        }
        if (m.itemId != null) {
            // Los matches en checklist items no se resaltan en el editor
            // (no usan RichTextState). Compromiso aceptable.
            delay(60)
            suppressAutoUpdate.value = false
            return@LaunchedEffect
        }
        val rs = blockStates[m.blockId]
        if (rs == null) {
            delay(60)
            suppressAutoUpdate.value = false
            return@LaunchedEffect
        }
        val len = rs.annotatedString.length
        if (m.plainStart < 0 || m.plainEnd > len) {
            delay(60)
            suppressAutoUpdate.value = false
            return@LaunchedEffect
        }

        val range = TextRange(m.plainStart, m.plainEnd)
        rs.addSpanStyle(
            spanStyle = SpanStyle(background = HIGHLIGHT_COLOR),
            textRange = range,
        )
        activeHighlight = HighlightRange(m.blockId, m.plainStart, m.plainEnd)

        // Llevar la selección al match para que el rich text haga
        // scroll automático a la línea correspondiente. Esto roba el
        // foco al campo de búsqueda — pero sólo pasa cuando el usuario
        // lo pide (Enter / next / prev), no en cada keystroke.
        rs.selection = range
        blockFocusRequesters[m.blockId]?.requestFocus()

        delay(60)
        suppressAutoUpdate.value = false
    }

    // Cuando los matches cambian (por edición de la nota o cambio de
    // query), si no hay match en el índice actual, sólo limpiamos el
    // highlight residual SIN saltar a ningún otro match. El usuario
    // tiene que apretar Enter o las flechas para ver el highlight.
    LaunchedEffect(findMatches) {
        val m = findMatches.getOrNull(effectiveFindIndex)
        if (m == null && activeHighlight != null) {
            suppressAutoUpdate.value = true
            activeHighlight?.let { prev ->
                blockStates[prev.blockId]?.removeSpanStyle(
                    spanStyle = SpanStyle(background = HIGHLIGHT_COLOR),
                    textRange = TextRange(prev.start, prev.end),
                )
            }
            activeHighlight = null
            delay(60)
            suppressAutoUpdate.value = false
        }
    }

    // Cuando se acaba de abrir el panel y ya hay matches, hacemos UN
    // único auto-trigger para resaltar el primero. Después de eso, sólo
    // se actualiza si el usuario presiona Enter o las flechas.
    var hadMatchesWhileOpen by remember { mutableStateOf(false) }
    LaunchedEffect(findState.open) {
        if (!findState.open) hadMatchesWhileOpen = false
    }
    LaunchedEffect(findMatches.isNotEmpty(), findState.open) {
        if (findState.open && findMatches.isNotEmpty() && !hadMatchesWhileOpen) {
            hadMatchesWhileOpen = true
            // Pequeño delay para que el rich text termine de procesar
            // ediciones recientes antes de saltar.
            delay(200)
            highlightTrigger += 1
        }
    }

    // Pickers de media.
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val name = mediaStorage.importUri(uri, fallbackExt = "jpg") ?: return@launch
                vm.insertMediaAfter(
                    afterTextBlockId = capturedBlockId,
                    splitOffset = capturedCursorOffset,
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
                    afterTextBlockId = capturedBlockId,
                    splitOffset = capturedCursorOffset,
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

    // Offset de dismiss para swipe-to-dismiss con spring.
    val dismissOffset = remember { Animatable(0f) }
    val isDragging = remember { mutableStateOf(false) }
    val velocityTracker = remember { VelocityTracker() }

    // Snackbar para mostrar el resultado de undo/redo (con un botón
    // que invierte la operación recién hecha).
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(vm) {
        vm.historyEvents.collect { ev ->
            // Cancelar el snackbar previo y mostrar el nuevo. Texto:
            //   "Se deshizo: editar texto"  ·  acción "Rehacer"
            //   "Se rehizo: insertar imagen" ·  acción "Deshacer"
            val (msg, actionLabel, isUndoEvent) = when (ev.kind) {
                HistoryUiEvent.Kind.Undone -> Triple("Se deshizo: ${ev.label}", "Rehacer", true)
                HistoryUiEvent.Kind.Redone -> Triple("Se rehizo: ${ev.label}", "Deshacer", false)
            }
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = actionLabel,
                duration = SnackbarDuration.Short,
                withDismissAction = false,
            )
            if (result == SnackbarResult.ActionPerformed) {
                if (isUndoEvent) vm.redo() else vm.undo()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OreoPalette.Bg0)
            // Atajos de teclado físico para deshacer / rehacer:
            //   Ctrl+Z  → undo
            //   Ctrl+Y  → redo
            //   Ctrl+Shift+Z → redo (compatible con la convención
            //   estándar de macOS / Linux / Windows)
            // En macOS Compose mapea Cmd como `isMetaPressed`, así
            // que aceptamos cualquiera de los dos modificadores.
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val cmd = ev.isCtrlPressed || ev.isMetaPressed
                if (!cmd) return@onPreviewKeyEvent false
                when (ev.key) {
                    Key.Z -> {
                        if (ev.isShiftPressed) vm.redo() else vm.undo()
                        true
                    }
                    Key.Y -> {
                        vm.redo()
                        true
                    }
                    Key.F -> {
                        if (findState.open) vm.closeFindReplace() else vm.openFindReplace()
                        true
                    }
                    else -> false
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .graphicsLayer {
                    translationY = dismissOffset.value
                    alpha = 1f - (dismissOffset.value / 800f).coerceIn(0f, 0.5f)
                },
        ) {
            // Cuerpo scrolleable: título + bloques.
            val scrollState = rememberScrollState()
            // Fracción suave (0→1) para la línea divisoria inferior de la
            // topbar — visible solo cuando el usuario scrollea el contenido.
            val rawScrolled by remember { derivedStateOf { scrollState.value > 0 } }
            val scrollFraction by animateFloatAsState(
                targetValue = if (rawScrolled) 1f else 0f,
                animationSpec = tween(180, easing = OreoMotion.EaseInOut),
                label = "scrollFraction",
            )

            // Subtítulo: "N palabras · Editado hace X min"
            val subtitle = remember(state.title, state.blocks, state.updatedAt, state.createdAt) {
                computeSubtitle(state.blocks, state.updatedAt, state.createdAt)
            }

            // FocusRequester para el campo de título: al tocar la zona de
            // título en la topbar, enfoca el BasicTextField.
            val titleFocusRequester = remember { FocusRequester() }

            // ¿La nota tiene contenido textual para mostrar en modo lectura?
            val hasTextContent = remember(state.blocks) {
                state.blocks.any { it is NoteBlock.Text && it.markdown.isNotBlank() }
            }
            EditorTopBar(
                title = state.title,
                subtitle = subtitle,
                isEmpty = state.isEmpty,
                saveStatus = state.saveStatus,
                findOpen = findState.open,
                scrollFraction = scrollFraction,
                onReaderMode = ({ onReaderMode(state.id) }).takeIf { hasTextContent },
                onBack = { scope.launch { saveAndBack() } },
                onTitleClick = { titleFocusRequester.requestFocus() },
                onToggleFind = { if (findState.open) vm.closeFindReplace() else vm.openFindReplace() },
                onTogglePin = { vm.togglePin() },
                onShare = { shareEditorState(context = ctxLocal, vmState = state) },
                onTimer = { showTimerDialog = true },
                onExportPdf = { showExportSheet = true },
                onDelete = { showDelete = true },
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        val dismissThreshold = 200f
                        detectDragGestures(
                            onDragStart = {
                                isDragging.value = true
                                velocityTracker.resetTracking()
                            },
                            onDrag = { change, dragAmount ->
                                if (dragAmount.y > 0) {
                                    change.consume()
                                    dismissOffset.updateBounds(lowerBound = 0f)
                                    scope.launch {
                                        dismissOffset.snapTo(
                                            (dismissOffset.value + dragAmount.y).coerceAtLeast(0f),
                                        )
                                    }
                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                }
                            },
                            onDragEnd = {
                                isDragging.value = false
                                val velocity = velocityTracker.calculateVelocity().y
                                val shouldDismiss = dismissOffset.value > dismissThreshold || velocity > 600f
                                scope.launch {
                                    if (shouldDismiss) {
                                        dismissOffset.animateTo(
                                            targetValue = 1000f,
                                            animationSpec = spring(
                                                dampingRatio = 0.6f,
                                                stiffness = 200f,
                                            ),
                                        )
                                        saveAndBack()
                                    } else {
                                        dismissOffset.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = 0.7f,
                                                stiffness = 300f,
                                            ),
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                isDragging.value = false
                                scope.launch {
                                    dismissOffset.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = 0.7f,
                                            stiffness = 300f,
                                        ),
                                    )
                                }
                            },
                        )
                    },
                contentAlignment = Alignment.TopCenter,
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .verticalScroll(scrollState)
                    .padding(horizontal = editorHorizontalPadding),
            ) {
                Spacer(Modifier.height(4.dp))

                // Animación de entrada para fecha y título
                val titleEntryAlpha = remember { Animatable(0f) }
                val titleEntryOffset = remember { Animatable(20f) }
                LaunchedEffect(Unit) {
                    titleEntryAlpha.animateTo(1f, tween(350, easing = OreoMotion.EaseOut))
                    titleEntryOffset.animateTo(0f, tween(350, easing = OreoMotion.EaseOut))
                }

                Text(
                    text = formatNowDate(),
                    color = OreoPalette.OnSurfaceFaint,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .graphicsLayer {
                            alpha = titleEntryAlpha.value
                            translationY = titleEntryOffset.value
                        },
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocusRequester)
                        .graphicsLayer {
                            alpha = titleEntryAlpha.value
                            translationY = titleEntryOffset.value
                        },
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
                            key(block.id) {
                                TextBlockEditor(
                                    block = block,
                                    initialMarkdown = block.markdown,
                                    focusRequester = fr,
                                    restoreVersion = state.restoreVersion,
                                    suppressAutoUpdate = suppressAutoUpdate,
                                    onMarkdownChange = { md -> vm.updateTextBlock(block.id, md) },
                                    onFocused = { rts, cursor ->
                                        focusedTextBlockId = block.id
                                        focusedCursorOffset = cursor
                                        activeStateRef.value = rts
                                        // Si el RichTextState cambió (montaje
                                        // tardío de un bloque), notificamos al
                                        // recompute de Find & Replace.
                                        val prev = blockStates[block.id]
                                        if (prev !== rts) {
                                            blockStates[block.id] = rts
                                            blockStatesVersion += 1
                                        }
                                    },
                                    onInsertBlock = { newBlock ->
                                        vm.insertBlockAfter(block.id, newBlock)
                                    },
                                    onUpdateTextAndInsertBlock = { blockId, newText, newBlock ->
                                        vm.updateTextAndInsertBlockAfter(blockId, newText, newBlock)
                                    },
                                )
                            }
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
                        is NoteBlock.Checklist -> ChecklistBlock(
                            block = block,
                            onToggle = { itemId -> vm.toggleChecklistItem(block.id, itemId) },
                            onTextChange = { itemId, text ->
                                vm.updateChecklistItemText(block.id, itemId, text)
                            },
                            onAddItem = { vm.addChecklistItem(block.id) },
                            onRemoveItem = { itemId -> vm.removeChecklistItem(block.id, itemId) },
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
                
                // Chip de conteo de palabras y tiempo de lectura
                val wordCount = remember(state.blocks) {
                    state.blocks.filterIsInstance<NoteBlock.Text>()
                        .sumOf { it.markdown.replace(Regex("<[^>]*>"), "").trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size }
                }
                val readTime = (wordCount / 200.0).coerceAtLeast(1.0).toInt()
                
                if (wordCount > 0) {
                    Text(
                        text = "$wordCount palabras · $readTime min",
                        color = OreoPalette.OnSurfaceFaint,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 8.dp),
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
            } // cierra Box centrador del body

            // Estado del cronómetro (vive en el editor, se resetea si cambia el target)
            var timerElapsed by remember(state.targetDurationSec) { mutableIntStateOf(0) }
            var timerRunning by remember { mutableStateOf(false) }

            LaunchedEffect(timerRunning, state.targetDurationSec) {
                while (timerRunning && state.targetDurationSec > 0) {
                    delay(1_000)
                    timerElapsed += 1
                }
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    OreoTimer(
                        targetSec = state.targetDurationSec,
                        elapsedSec = timerElapsed,
                        running = timerRunning,
                        onToggle = { timerRunning = !timerRunning },
                        onReset = { timerElapsed = 0; timerRunning = false },
                        onClose = { vm.setTargetMinutes(0); timerElapsed = 0; timerRunning = false },
                        variant = OreoTimerVariant.Editor,
                    )
                }
            }

            // Toolbar de formato + media
            FormatToolbar(
                activeState = activeStateRef.value,
                onPickImage = {
                    capturedBlockId = focusedTextBlockId
                    capturedCursorOffset = focusedCursorOffset
                    imagePicker.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        ),
                    )
                },
                onPickVideo = {
                    capturedBlockId = focusedTextBlockId
                    capturedCursorOffset = focusedCursorOffset
                    videoPicker.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.VideoOnly
                        ),
                    )
                },
                onInsertChecklist = {
                    vm.insertChecklistAfter(
                        afterTextBlockId = focusedTextBlockId,
                        splitOffset = focusedCursorOffset,
                    )
                },
                aiService = container.aiService.takeIf { it.isAvailable() },
                onAiAction = { action ->
                    val rts = activeStateRef.value
                    val hasSelection = rts != null && !rts.selection.collapsed

                    if (hasSelection) {
                        // Selección explícita → operar sobre eso
                        val fullText = rts!!.annotatedString.text
                        val start = rts.selection.start
                        val end = rts.selection.end
                        val text = fullText.substring(start, end)
                        if (text.isBlank()) return@FormatToolbar
                        val bid = focusedTextBlockId ?: return@FormatToolbar
                        aiSheetData = AiSheetData(
                            label = action.label,
                            text = text,
                            createFlow = { action.createFlow(container.aiService, text) },
                            allTextBlockIds = null,
                            richState = rts,
                            selStart = start,
                            selEnd = end,
                            blockId = bid,
                            onReplace = { result, _, state, s, e ->
                                state!!.replaceTextRange(TextRange(s, e), result)
                            },
                            onInsertBelow = { result ->
                                vm.insertBlockAfter(bid, NoteBlock.Text(markdown = result))
                            },
                        )
                    } else {
                        // Sin selección → tomar TODA la nota
                        val textBlocks = state.blocks.filterIsInstance<NoteBlock.Text>()
                        if (textBlocks.isEmpty()) return@FormatToolbar
                        val allText = textBlocks.joinToString("\n\n") { b ->
                            b.markdown.replace(Regex("<[^>]*>"), "")
                        }
                        if (allText.isBlank()) return@FormatToolbar
                        val allIds = textBlocks.map { it.id }
                        aiSheetData = AiSheetData(
                            label = action.label,
                            text = allText,
                            createFlow = { action.createFlow(container.aiService, allText) },
                            allTextBlockIds = allIds,
                            richState = null,
                            selStart = 0,
                            selEnd = 0,
                            blockId = null,
                            onReplace = { result, ids, _, _, _ ->
                                // Reemplazar todos los bloques de texto con el resultado
                                if (ids != null && ids.isNotEmpty()) {
                                    val firstId = ids.first()
                                    vm.updateTextBlock(firstId, result)
                                    // Eliminar los bloques restantes (de atrás hacia adelante)
                                    for (i in ids.size - 1 downTo 1) {
                                        vm.removeBlock(ids[i])
                                    }
                                }
                            },
                            onInsertBelow = { result ->
                                val lastBid = allIds.lastOrNull()
                                if (lastBid != null) {
                                    vm.insertBlockAfter(lastBid, NoteBlock.Text(markdown = result))
                                }
                            },
                        )
                    }
                },
            )
        }

        // Find & Replace Panel — overlay flotante sobre el editor
        AnimatedVisibility(
            visible = findState.open,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(280, easing = OreoMotion.EaseOut),
            ) + fadeIn(tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(220, easing = OreoMotion.EaseInOut),
            ) + fadeOut(tween(180)),
        ) {
            // Al cerrarse el panel, asegurar que el highlight se limpia.
            // También suspendemos el auto-update mientras se quita el span,
            // para que el background no termine persistido.
            DisposableEffect(Unit) {
                onDispose {
                    suppressAutoUpdate.value = true
                    activeHighlight?.let { prev ->
                        blockStates[prev.blockId]?.removeSpanStyle(
                            spanStyle = SpanStyle(background = HIGHLIGHT_COLOR),
                            textRange = TextRange(prev.start, prev.end),
                        )
                    }
                    activeHighlight = null
                    // Re-habilitar el auto-update tras un par de frames.
                    scope.launch {
                        delay(50)
                        suppressAutoUpdate.value = false
                    }
                }
            }

            FindReplacePanel(
                findQuery = findState.query,
                replaceText = findState.replacement,
                caseSensitive = findState.caseSensitive,
                useRegex = findState.useRegex,
                matchCount = findMatches.size,
                currentMatchIndex = effectiveFindIndex,
                regexError = findRegexError,
                onFindQueryChange = vm::setFindQuery,
                onReplaceTextChange = vm::setFindReplacement,
                onToggleCaseSensitive = vm::toggleFindCaseSensitive,
                onToggleRegex = vm::toggleFindUseRegex,
                onClose = { vm.closeFindReplace() },
                onNext = {
                    if (findMatches.isEmpty()) return@FindReplacePanel
                    val nextIdx = FindReplaceEngine.nextIndex(effectiveFindIndex, findMatches.size)
                    vm.setFindCurrentIndex(nextIdx)
                    highlightTrigger += 1
                },
                onPrev = {
                    if (findMatches.isEmpty()) return@FindReplacePanel
                    val prevIdx = FindReplaceEngine.prevIndex(effectiveFindIndex, findMatches.size)
                    vm.setFindCurrentIndex(prevIdx)
                    highlightTrigger += 1
                },
                onReplaceCurrent = {
                    val m = findMatches.getOrNull(effectiveFindIndex) ?: return@FindReplacePanel
                    val richState = blockStates[m.blockId] ?: return@FindReplacePanel

                    // Antes de mutar el texto, quitar el highlight para que
                    // el reemplazo no herede el background del span.
                    suppressAutoUpdate.value = true
                    activeHighlight?.let { prev ->
                        blockStates[prev.blockId]?.removeSpanStyle(
                            spanStyle = SpanStyle(background = HIGHLIGHT_COLOR),
                            textRange = TextRange(prev.start, prev.end),
                        )
                    }
                    activeHighlight = null
                    suppressAutoUpdate.value = false

                    // Reemplazar el rango usando la API del rich text.
                    // Esto va a disparar el snapshotFlow del bloque que
                    // termina llamando vm.updateTextBlock — eso pushea
                    // su propia acción TextBlockEdit. Por eso acá NO
                    // pusheamos otra; el undo va a funcionar como un
                    // único paso vía la coalescencia natural del editor.
                    val len = richState.annotatedString.length
                    if (m.plainStart < 0 || m.plainEnd > len) return@FindReplacePanel
                    richState.replaceTextRange(
                        textRange = TextRange(m.plainStart, m.plainEnd),
                        text = findState.replacement,
                    )
                    // Después del reemplazo, los matches se recalculan
                    // automáticamente. Pedimos highlight al siguiente.
                    highlightTrigger += 1
                },
                onReplaceAll = {
                    if (findMatches.isEmpty()) return@FindReplacePanel

                    // Antes de mutar, quitar el highlight activo.
                    suppressAutoUpdate.value = true
                    activeHighlight?.let { prev ->
                        blockStates[prev.blockId]?.removeSpanStyle(
                            spanStyle = SpanStyle(background = HIGHLIGHT_COLOR),
                            textRange = TextRange(prev.start, prev.end),
                        )
                    }
                    activeHighlight = null
                    suppressAutoUpdate.value = false

                    // Agrupar por bloque y reemplazar de derecha a izquierda
                    // dentro de cada bloque para no invalidar offsets.
                    // Cada replaceTextRange dispara el snapshotFlow del rich
                    // text que termina llamando vm.updateTextBlock — esas
                    // acciones se coalescen (mismo bloque) en una sola
                    // entrada de undo. Entre bloques distintos no coalescen,
                    // así que queda una entrada por bloque tocado (suficiente
                    // y predecible para el usuario).
                    val byBlock = findMatches
                        .filter { it.itemId == null && blockStates.containsKey(it.blockId) }
                        .groupBy { it.blockId }

                    byBlock.forEach { (blockId, blockMatches) ->
                        val richState = blockStates[blockId] ?: return@forEach
                        val sorted = blockMatches.sortedByDescending { it.plainStart }
                        for (mm in sorted) {
                            val len = richState.annotatedString.length
                            if (mm.plainStart < 0 || mm.plainEnd > len) continue
                            richState.replaceTextRange(
                                textRange = TextRange(mm.plainStart, mm.plainEnd),
                                text = findState.replacement,
                            )
                        }
                    }
                },
            )
        }

        // Snackbar host alineado a la base.
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = OreoPalette.SurfaceCard,
                contentColor = OreoPalette.OnSurface,
                actionColor = OreoPalette.Accent,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            )
        }

        // Diálogos
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

        if (showExportSheet) {
            var discursoToExport by remember { mutableStateOf<com.oreoexperience.notes.data.Discurso?>(null) }
            LaunchedEffect(showExportSheet) {
                val id = runCatching { vm.saveBlocking() }.getOrNull() ?: state.id
                val effectiveId = if (id > 0L) id else state.id
                discursoToExport = runCatching { repository.get(effectiveId) }.getOrNull()
            }
            val d = discursoToExport
            if (d != null) {
                ExportPdfSheet(
                    discurso = d,
                    onDismiss = { showExportSheet = false },
                    onRequestSafExport = { settings ->
                        pendingExportSettings = settings
                        val safeTitle = state.title
                            .ifBlank { "nota" }
                            .lowercase()
                            .replace(" ", "_")
                            .replace("/", "-")
                            .take(40)
                        pdfLauncher.launch("oreo_${safeTitle}.pdf")
                    },
                )
            }
        }

        // IA Suggestion Sheet
        aiSheetData?.let { data ->
            AiSuggestionSheet(
                actionLabel = data.label,
                originalText = data.text,
                createStreamFlow = data.createFlow,
                onDismiss = { aiSheetData = null },
                onReplace = { result ->
                    data.onReplace(result, data.allTextBlockIds, data.richState, data.selStart, data.selEnd)
                },
                onInsertBelow = data.onInsertBelow,
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
    restoreVersion: Long,
    suppressAutoUpdate: androidx.compose.runtime.MutableState<Boolean>,
    onMarkdownChange: (String) -> Unit,
    onFocused: (RichTextState, Int) -> Unit,
    onInsertBlock: (NoteBlock) -> Unit,
    onUpdateTextAndInsertBlock: (String, String, NoteBlock) -> Unit,
) {
    val richState = rememberRichTextState()
    val currentOnChange by rememberUpdatedState(onMarkdownChange)
    val currentOnFocused by rememberUpdatedState(onFocused)
    LaunchedEffect(block.id, restoreVersion) {
        richState.setHtml(markdownToSimpleHtml(initialMarkdown))
    }
    // Registrar el RichTextState apenas se monte el bloque, para que
    // Find & Replace pueda buscar en él aunque el usuario nunca le
    // haya hecho foco. Sólo lo anunciamos como "registrado" — sin
    // cambiar focusedTextBlockId, para no robarle el foco al usuario.
    LaunchedEffect(richState) {
        currentOnFocused(richState, richState.selection.start)
    }
    LaunchedEffect(richState) {
        snapshotFlow { richState.annotatedString }
            .drop(1)
            .distinctUntilChanged()
            .collect {
                // Cuando estamos aplicando/quitando el highlight de
                // Find & Replace, ignoramos el cambio para que el span
                // de background no se persista en el modelo.
                if (suppressAutoUpdate.value) return@collect
                currentOnChange(richState.toHtml())
            }
    }
    val cursor by remember(richState) {
        derivedStateOf { richState.selection.start }
    }
    LaunchedEffect(cursor) {
        onFocused(richState, cursor)
    }

    // Slash menu state
    var showSlashMenu by remember { mutableStateOf(false) }
    var slashMenuQuery by remember { mutableStateOf("") }
    
    // Detect "/" to show slash menu
    LaunchedEffect(richState) {
        snapshotFlow { richState.annotatedString.text }
            .distinctUntilChanged()
            .collect { text ->
                val lastLineStart = text.lastIndexOf('\n').let { if (it == -1) 0 else it + 1 }
                val currentLine = text.substring(lastLineStart)
                val slashIdx = currentLine.lastIndexOf('/')
                val spaceAfterSlash = currentLine.indexOf(' ', slashIdx + 1)
                val queryEnd = if (spaceAfterSlash == -1) currentLine.length else spaceAfterSlash
                val query = if (slashIdx >= 0) currentLine.substring(slashIdx + 1, queryEnd) else ""
                
                if (slashIdx >= 0 && (slashIdx == 0 || text[lastLineStart + slashIdx - 1] == ' ')) {
                    showSlashMenu = true
                    slashMenuQuery = query
                } else {
                    showSlashMenu = false
                }
            }
    }

    // Uso BasicRichTextEditor (no la variante material3) para evitar
    // el padding interno del TextField, que descalibra el hit-test:
    // antes el texto visible quedaba en una posición pero la tap area
    // de las palabras estaba corrida unos dípis, y al tocar una
    // palabra el cursor caía en otro lado. Sin el wrapper material3,
    // el toque coincide exactamente con el texto.
    Box(modifier = Modifier.fillMaxWidth()) {
        BasicRichTextEditor(
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
            cursorBrush = SolidColor(OreoPalette.Accent),
            decorationBox = { inner ->
                if (richState.annotatedString.isEmpty()) {
                    Text(
                        text = "Empezá a escribir…",
                        color = OreoPalette.OnSurfaceFaint,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Start,
                    )
                }
                inner()
            },
        )
        
        // Slash Menu Popup
        if (showSlashMenu) {
            val slashItems = listOf(
                "text" to "Texto",
                "h1" to "Título 1",
                "h2" to "Título 2",
                "checklist" to "Lista de tareas",
                "quote" to "Cita",
                "divider" to "Separador",
            ).filter { it.first.contains(slashMenuQuery, ignoreCase = true) || it.second.contains(slashMenuQuery, ignoreCase = true) }
            
            if (slashItems.isNotEmpty()) {
                DropdownMenu(
                    expanded = true,
                    onDismissRequest = { showSlashMenu = false },
                    modifier = Modifier
                        .width(220.dp)
                        .background(OreoPalette.SurfaceCard),
                ) {
                    slashItems.forEach { (type, label) ->
                        DropdownMenuItem(
                            text = { Text(label, color = OreoPalette.OnSurface) },
                            onClick = {
                                val cursorPos = richState.selection.start
                                val fullText = richState.annotatedString.text
                                val lastLineStart = fullText.lastIndexOf('\n', cursorPos - 1).let { if (it == -1) 0 else it + 1 }
                                val slashPos = fullText.lastIndexOf('/', cursorPos - 1)
                                
                                val beforeSlash = fullText.substring(0, slashPos)
                                val afterCursor = fullText.substring(cursorPos)
                                val newText = beforeSlash + afterCursor
                                
                                showSlashMenu = false
                                
                                val newBlock = when (type) {
                                    "h1" -> NoteBlock.Text(markdown = "# Título")
                                    "h2" -> NoteBlock.Text(markdown = "## Subtítulo")
                                    "checklist" -> NoteBlock.Checklist(items = listOf())
                                    "quote" -> NoteBlock.Text(markdown = "> Cita")
                                    "divider" -> NoteBlock.Text(markdown = "---")
                                    else -> NoteBlock.Text(markdown = "")
                                }
                                onUpdateTextAndInsertBlock(block.id, newText, newBlock)
                            },
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// IA: toolbar button con submenús
// ---------------------------------------------------------------------------

@Composable
private fun AiToolbarButton(
    activeState: RichTextState?,
    aiService: AiService?,
    onAiAction: (AiAction) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var rewriteSubExpanded by remember { mutableStateOf(false) }
    var translateSubExpanded by remember { mutableStateOf(false) }
    val isRateLimited = aiService?.let {
        it.geminiClient.getCooldownSeconds() > 0
    } ?: false

    Box {
        ToolbarButton(
            icon = Icons.Outlined.AutoAwesome,
            description = "Inteligencia artificial",
            active = false,
            onClick = { if (!isRateLimited) menuExpanded = !menuExpanded },
        )
        // Badge de requests usados o cooldown
        aiService?.let { svc ->
            val cooldown = svc.geminiClient.getCooldownSeconds()
            val used = svc.geminiClient.getRequestsUsed()
            
            if (cooldown > 0) {
                // Rate-limited: mostrar countdown
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(OreoPalette.DangerFill)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = "${cooldown}s",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else if (used >= 12) {
                // Warning cuando quedan 3 o menos
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(OreoPalette.CategoryConsideracion)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = "$used/15",
                        color = OreoPalette.Bg0,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = {
                menuExpanded = false
                rewriteSubExpanded = false
                translateSubExpanded = false
            },
            modifier = Modifier
                .background(color = OreoPalette.SurfaceCard, shape = RoundedCornerShape(18.dp))
                .padding(vertical = 4.dp),
        ) {
            DropdownMenuItem(
                text = { Text("Corregir ortografía", color = OreoPalette.OnSurface, fontSize = 14.sp) },
                onClick = { menuExpanded = false; onAiAction(AiAction.Correct) },
            )
            Box {
                DropdownMenuItem(
                    text = { Text("Reescribir…", color = OreoPalette.OnSurface, fontSize = 14.sp) },
                    onClick = { rewriteSubExpanded = true },
                    trailingIcon = { Icon(Icons.Outlined.ChevronRight, null, tint = OreoPalette.OnSurfaceFaint, modifier = Modifier.size(16.dp)) },
                )
                DropdownMenu(
                    expanded = rewriteSubExpanded,
                    onDismissRequest = { rewriteSubExpanded = false },
                    modifier = Modifier.background(color = OreoPalette.SurfaceCard, shape = RoundedCornerShape(18.dp)).padding(vertical = 4.dp),
                ) {
                    RewriteTone.values().forEach { tone ->
                        DropdownMenuItem(
                            text = { Text(tone.label, color = OreoPalette.OnSurface, fontSize = 14.sp) },
                            onClick = { rewriteSubExpanded = false; menuExpanded = false; onAiAction(AiAction.Rewrite(tone)) },
                        )
                    }
                }
            }
            DropdownMenuItem(
                text = { Text("Acortar", color = OreoPalette.OnSurface, fontSize = 14.sp) },
                onClick = { menuExpanded = false; onAiAction(AiAction.Shorten) },
            )
            DropdownMenuItem(
                text = { Text("Ampliar", color = OreoPalette.OnSurface, fontSize = 14.sp) },
                onClick = { menuExpanded = false; onAiAction(AiAction.Expand) },
            )
            DropdownMenuItem(
                text = { Text("Explicar", color = OreoPalette.OnSurface, fontSize = 14.sp) },
                onClick = { menuExpanded = false; onAiAction(AiAction.Explain) },
            )
            Box {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Translate, null, tint = OreoPalette.OnSurfaceMuted, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Traducir…", color = OreoPalette.OnSurface, fontSize = 14.sp)
                        }
                    },
                    onClick = { translateSubExpanded = true },
                    trailingIcon = { Icon(Icons.Outlined.ChevronRight, null, tint = OreoPalette.OnSurfaceFaint, modifier = Modifier.size(16.dp)) },
                )
                DropdownMenu(
                    expanded = translateSubExpanded,
                    onDismissRequest = { translateSubExpanded = false },
                    modifier = Modifier.background(color = OreoPalette.SurfaceCard, shape = RoundedCornerShape(18.dp)).padding(vertical = 4.dp),
                ) {
                    listOf("Inglés", "Portugués", "Italiano").forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang, color = OreoPalette.OnSurface, fontSize = 14.sp) },
                            onClick = { translateSubExpanded = false; menuExpanded = false; onAiAction(AiAction.Translate(lang)) },
                        )
                    }
                }
            }
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
        animationSpec = OreoMotion.SpringPress(),
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

// ═══════════════════════════════════════════════════════════════
// Helpers para el subtítulo de la topbar (conteo de palabras +
// tiempo relativo).
// ═══════════════════════════════════════════════════════════════

private val TAG_STRIP = Regex("<[^>]*>")

/**
 * Cuenta palabras visibles en todos los bloques de texto y checklist.
 * Para los bloques de texto usa el markdown limpio de tags HTML.
 */
private fun countWords(blocks: List<NoteBlock>): Int {
    var count = 0
    for (block in blocks) {
        when (block) {
            is NoteBlock.Text -> {
                val plain = TAG_STRIP.replace(block.markdown, " ").trim()
                if (plain.isNotBlank()) {
                    count += plain.split(Regex("\\s+")).size
                }
            }
            is NoteBlock.Checklist -> {
                for (item in block.items) {
                    if (item.text.isNotBlank()) {
                        count += item.text.trim().split(Regex("\\s+")).size
                    }
                }
            }
            else -> {} // Image y Video no aportan palabras
        }
    }
    return count
}

/**
 * Formatea la diferencia entre [updatedAt] y ahora como texto relativo
 * en español ("hace un momento", "hace 5 min", "hoy 14:30", etc.).
 * Si la nota nunca se editó ([updatedAt] == [createdAt]), usa "Creado"
 * en vez de "Editado".
 */
private fun formatRelativeTime(updatedAt: Long, createdAt: Long): String {
    val prefix = if (updatedAt == createdAt) "Creado" else "Editado"
    val now = System.currentTimeMillis()
    val diff = now - updatedAt
    val timeStr = when {
        diff < 60_000 -> "hace un momento"
        diff < 60 * 60_000 -> "hace ${(diff / 60_000).toInt()} min"
        diff < 24 * 60 * 60_000 -> "hace ${(diff / (60 * 60_000)).toInt()} h"
        else -> {
            val cal = java.util.Calendar.getInstance()
            val then = java.util.Calendar.getInstance().apply { timeInMillis = updatedAt }
            val nowDay = cal.get(java.util.Calendar.DAY_OF_YEAR)
            val thenDay = then.get(java.util.Calendar.DAY_OF_YEAR)
            val nowYear = cal.get(java.util.Calendar.YEAR)
            val thenYear = then.get(java.util.Calendar.YEAR)
            when {
                nowYear == thenYear && nowDay == thenDay ->
                    SimpleDateFormat("'hoy' HH:mm", Locale("es")).format(Date(updatedAt))
                nowYear == thenYear && nowDay - thenDay == 1 ->
                    SimpleDateFormat("'ayer' HH:mm", Locale("es")).format(Date(updatedAt))
                else ->
                    SimpleDateFormat("dd/MM/yyyy", Locale("es")).format(Date(updatedAt))
            }
        }
    }
    return "$prefix $timeStr"
}

private fun computeSubtitle(
    blocks: List<NoteBlock>,
    updatedAt: Long,
    createdAt: Long,
): String {
    if (updatedAt == 0L && createdAt == 0L) return ""
    val words = countWords(blocks)
    val time = formatRelativeTime(updatedAt, createdAt)
    return "$words palabra${if (words != 1) "s" else ""} · $time"
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
            is NoteBlock.Text -> b.markdown.replace(Regex("<[^>]*>|<[^>]+"), "")
            is NoteBlock.Image -> "[imagen: ${b.fileName}]"
            is NoteBlock.Video -> "[video: ${b.fileName}]"
            is NoteBlock.Checklist -> b.items.joinToString("\n") { item ->
                val box = if (item.checked) "[x]" else "[ ]"
                "$box ${item.text}"
            }
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

// ---------------------------------------------------------------------------
// Toolbar helpers: heading semántico, quote, code, color de texto, link,
// alineación, resaltado y bloque de checklist.
// ---------------------------------------------------------------------------

/**
 * Niveles de heading que ofrece el dropdown del toolbar. El nivel se
 * detecta leyendo `currentSpanStyle.fontSize`. "Body" no aplica
 * ningún span — usa el tamaño por defecto del bloque (17 sp).
 */
private enum class HeadingLevel(
    val label: String,
    val fontSize: TextUnit?,
    val fontWeight: FontWeight?,
) {
    Title("Título",      32.sp, FontWeight.Bold),
    Heading("Encabezado",24.sp, FontWeight.SemiBold),
    Subheading("Subtítulo",20.sp, FontWeight.SemiBold),
    Body("Cuerpo",       null,  null),
}

private val ALL_HEADING_SIZES_SP = listOf(20.sp, 24.sp, 32.sp)

private fun detectHeadingLevel(state: RichTextState?): HeadingLevel {
    val span = state?.currentSpanStyle ?: return HeadingLevel.Body
    val size = span.fontSize
    if (size == TextUnit.Unspecified) return HeadingLevel.Body
    return when (size) {
        32.sp -> HeadingLevel.Title
        24.sp -> HeadingLevel.Heading
        20.sp -> HeadingLevel.Subheading
        else -> HeadingLevel.Body
    }
}

private fun applyHeading(state: RichTextState?, target: HeadingLevel) {
    val s = state ?: return
    // Limpiamos cualquier nivel previo: se quitan los fontSize y los
    // pesos asociados. Usamos `removeSpanStyle` con cada talla porque
    // RichTextState no expone una forma de borrar "todos los fontSize".
    for (size in ALL_HEADING_SIZES_SP) {
        s.removeSpanStyle(SpanStyle(fontSize = size))
    }
    // FontWeight.Bold puede haberse aplicado por Title también. Si el
    // usuario tenía bold manual, este toggle lo quita; es aceptable
    // porque al cambiar de heading se "reinicia" la jerarquía.
    s.removeSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
    s.removeSpanStyle(SpanStyle(fontWeight = FontWeight.SemiBold))
    if (target.fontSize != null) {
        s.addSpanStyle(SpanStyle(fontSize = target.fontSize))
    }
    if (target.fontWeight != null) {
        s.addSpanStyle(SpanStyle(fontWeight = target.fontWeight))
    }
}

@Composable
private fun HeadingDropdownButton(activeState: RichTextState?) {
    var open by remember { mutableStateOf(false) }
    val current = detectHeadingLevel(activeState)
    Box {
        ToolbarButton(
            icon = Icons.Outlined.Title,
            description = "Estilo de párrafo: ${current.label}",
            active = current != HeadingLevel.Body,
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
                )
                .padding(vertical = 4.dp),
        ) {
            HeadingLevel.values().forEach { level ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = level.label,
                            color = if (level == current) OreoPalette.Accent else OreoPalette.OnSurface,
                            fontSize = when (level) {
                                HeadingLevel.Title -> 22.sp
                                HeadingLevel.Heading -> 18.sp
                                HeadingLevel.Subheading -> 16.sp
                                HeadingLevel.Body -> 14.sp
                            },
                            fontWeight = level.fontWeight ?: FontWeight.Normal,
                        )
                    },
                    onClick = {
                        applyHeading(activeState, level)
                        open = false
                    },
                )
            }
        }
    }
}

/**
 * Botón "code" que aplica un span con `FontFamily.Monospace` y un
 * fondo gris sutil (`SurfaceCardHi`). Es código *inline*; para code
 * blocks completos haría falta un bloque dedicado tipo `NoteBlock.Code`,
 * que dejamos para una iteración futura.
 */
@Composable
private fun CodeToolbarButton(activeState: RichTextState?) {
    val isCode = activeState?.currentSpanStyle?.fontFamily == androidx.compose.ui.text.font.FontFamily.Monospace
    ToolbarButton(
        icon = Icons.Outlined.Code,
        description = "Código en línea",
        active = isCode,
        onClick = {
            val s = activeState ?: return@ToolbarButton
            // Toggle manual: si ya es monospace, quitamos los dos
            // spans; si no, los aplicamos juntos.
            val codeFont = androidx.compose.ui.text.font.FontFamily.Monospace
            val codeBg = OreoPalette.SurfaceCardHi
            if (isCode) {
                s.removeSpanStyle(SpanStyle(fontFamily = codeFont))
                s.removeSpanStyle(SpanStyle(background = codeBg))
            } else {
                s.addSpanStyle(SpanStyle(fontFamily = codeFont, background = codeBg))
            }
        },
    )
}

/** Paleta de colores de texto del editor. */
private val TEXT_COLORS = listOf(
    OreoPalette.EditorTextBlack to "Negro",
    OreoPalette.Accent to "Aurora",
    OreoPalette.EditorTextRed to "Rojo",
    OreoPalette.EditorTextOrange to "Naranja",
    OreoPalette.EditorTextGreen to "Verde",
    OreoPalette.EditorTextBlue to "Azul",
)

@Composable
private fun TextColorToolbarButton(activeState: RichTextState?) {
    var open by remember { mutableStateOf(false) }
    val currentColor = activeState?.currentSpanStyle?.color ?: Color.Unspecified
    val hasColor = currentColor != Color.Unspecified
    Box {
        ToolbarButton(
            icon = Icons.Outlined.FormatColorText,
            description = "Color de texto",
            active = hasColor,
            tintActive = if (hasColor) currentColor else OreoPalette.Accent,
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TEXT_COLORS.forEach { (color, label) ->
                    ColorSwatch(
                        color = color,
                        label = label,
                        selected = hasColor && currentColor == color,
                        onClick = {
                            applyTextColor(activeState, color)
                            open = false
                        },
                    )
                }
                ColorSwatch(
                    color = Color.Transparent,
                    label = "Quitar",
                    selected = false,
                    isClear = true,
                    onClick = {
                        clearTextColor(activeState)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    isClear: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(28.dp)
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
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isClear) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = label,
                tint = OreoPalette.OnSurfaceMuted,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

private fun applyTextColor(state: RichTextState?, color: Color) {
    val s = state ?: return
    val cur = s.currentSpanStyle.color
    if (cur != Color.Unspecified) {
        s.removeSpanStyle(SpanStyle(color = cur))
    }
    s.addSpanStyle(SpanStyle(color = color))
}

private fun clearTextColor(state: RichTextState?) {
    val s = state ?: return
    val cur = s.currentSpanStyle.color
    if (cur != Color.Unspecified) {
        s.removeSpanStyle(SpanStyle(color = cur))
    }
}

/**
 * Dropdown de alineación: izquierda / centro / derecha. Aplica un
 * `ParagraphStyle` con el `textAlign` correspondiente. Como Compose
 * `RichTextState` permite múltiples paragraph styles, primero quitamos
 * los anteriores para que el toggle sea exclusivo.
 */
@Composable
private fun AlignmentDropdownButton(activeState: RichTextState?) {
    var open by remember { mutableStateOf(false) }
    val currentAlign = activeState?.currentParagraphStyle?.textAlign
    Box {
        ToolbarButton(
            icon = when (currentAlign) {
                TextAlign.Center -> Icons.Outlined.FormatAlignCenter
                TextAlign.End,
                TextAlign.Right -> Icons.Outlined.FormatAlignRight
                else -> Icons.Outlined.FormatAlignLeft
            },
            description = "Alineación",
            active = currentAlign != null && currentAlign != TextAlign.Start && currentAlign != TextAlign.Unspecified,
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
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlignmentMiniButton(
                    icon = Icons.Outlined.FormatAlignLeft,
                    selected = currentAlign == null || currentAlign == TextAlign.Start || currentAlign == TextAlign.Left,
                    onClick = {
                        applyAlignment(activeState, TextAlign.Start)
                        open = false
                    },
                )
                AlignmentMiniButton(
                    icon = Icons.Outlined.FormatAlignCenter,
                    selected = currentAlign == TextAlign.Center,
                    onClick = {
                        applyAlignment(activeState, TextAlign.Center)
                        open = false
                    },
                )
                AlignmentMiniButton(
                    icon = Icons.Outlined.FormatAlignRight,
                    selected = currentAlign == TextAlign.End || currentAlign == TextAlign.Right,
                    onClick = {
                        applyAlignment(activeState, TextAlign.End)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun AlignmentMiniButton(
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) OreoPalette.Accent.copy(alpha = 0.20f) else Color.Transparent,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) OreoPalette.Accent else OreoPalette.OnSurfaceMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}

private fun applyAlignment(state: RichTextState?, align: TextAlign) {
    val s = state ?: return
    // Quitamos los paragraph styles previos de alignment.
    for (a in listOf(TextAlign.Start, TextAlign.Center, TextAlign.End, TextAlign.Left, TextAlign.Right, TextAlign.Justify)) {
        s.removeParagraphStyle(androidx.compose.ui.text.ParagraphStyle(textAlign = a))
    }
    s.addParagraphStyle(androidx.compose.ui.text.ParagraphStyle(textAlign = align))
}

/**
 * Diálogo para insertar un enlace. El usuario ve dos campos: URL
 * (obligatorio) y "texto a mostrar" (opcional, por defecto la URL).
 * Si la URL no contiene `://`, se le antepone `https://` para que
 * el link sea válido al ser tocado.
 */
@Composable
private fun LinkDialog(
    initialUrl: String,
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: (url: String, text: String) -> Unit,
) {
    var url by remember { mutableStateOf(initialUrl) }
    var text by remember { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Insertar enlace") },
        text = {
            Column {
                Text(
                    text = "Si el campo de texto está vacío, se mostrará la URL como etiqueta.",
                    color = OreoPalette.OnSurfaceMuted,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(12.dp))
                BasicTextField(
                    value = url,
                    onValueChange = { url = it },
                    textStyle = LocalTextStyle.current.copy(
                        color = OreoPalette.OnSurface,
                        fontSize = 16.sp,
                    ),
                    cursorBrush = SolidColor(OreoPalette.Accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = OreoPalette.SurfaceCardHi,
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            if (url.isEmpty()) {
                                Text(
                                    text = "https://…",
                                    color = OreoPalette.OnSurfaceFaint,
                                    fontSize = 16.sp,
                                )
                            }
                            inner()
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = LocalTextStyle.current.copy(
                        color = OreoPalette.OnSurface,
                        fontSize = 16.sp,
                    ),
                    cursorBrush = SolidColor(OreoPalette.Accent),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = OreoPalette.SurfaceCardHi,
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            if (text.isEmpty()) {
                                Text(
                                    text = "Texto a mostrar (opcional)",
                                    color = OreoPalette.OnSurfaceFaint,
                                    fontSize = 16.sp,
                                )
                            }
                            inner()
                        }
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(url.trim(), text.trim()) }) {
                Text("Insertar", color = OreoPalette.Accent)
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
        shape = RoundedCornerShape(28.dp),
    )
}

/** Paleta de resaltado — colores originales con 50% de intensidad. */
private val HIGHLIGHT_COLORS = listOf(
    OreoPalette.EditorHighlightViolet to "Violeta",
    OreoPalette.EditorHighlightYellow to "Amarillo",
    OreoPalette.EditorHighlightGreen to "Verde",
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
    // Eliminamos cualquier background previo para evitar solapamientos.
    val current = s.currentSpanStyle.background
    if (current != Color.Unspecified && current != Color.Transparent) {
        s.removeSpanStyle(SpanStyle(background = current))
    }
    // Aplicamos el nuevo color con 50% de intensidad.
    s.addSpanStyle(SpanStyle(background = color))
    
    // Forzamos la recomposición inmediata del editor manipulando la selección.
    // Esto es necesario porque RichTextState a veces no repinta los spans
    // hasta que el contenido textual cambia.
    val sel = s.selection
    s.selection = TextRange(sel.end, sel.start)
    s.selection = sel
}

private fun clearHighlight(state: RichTextState?) {
    val s = state ?: return
    val current = s.currentSpanStyle.background
    if (current != Color.Unspecified && current != Color.Transparent) {
        s.removeSpanStyle(SpanStyle(background = current))
    }
}


// ---------------------------------------------------------------------------
// Render: bloque de checklist
// ---------------------------------------------------------------------------

/**
 * Render de un [NoteBlock.Checklist]. Cada ítem es un row con:
 *   - Checkbox custom redondeada (estilo iOS Notes) que togglea el flag.
 *   - BasicTextField para editar el texto del ítem; al presionar Enter
 *     se inserta un nuevo ítem en blanco; al borrar con backspace en
 *     un ítem vacío se elimina ese ítem.
 *
 * El estilo cuando un ítem está marcado: tachado + opacidad reducida.
 */
@Composable
private fun ChecklistBlock(
    block: NoteBlock.Checklist,
    onToggle: (itemId: String) -> Unit,
    onTextChange: (itemId: String, text: String) -> Unit,
    onAddItem: () -> String?,
    onRemoveItem: (itemId: String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        block.items.forEach { item ->
            key(item.id) {
                ChecklistRow(
                    item = item,
                    onToggle = { onToggle(item.id) },
                    onTextChange = { text -> onTextChange(item.id, text) },
                    onEnter = { onAddItem() },
                    onBackspaceWhenEmpty = { onRemoveItem(item.id) },
                )
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    item: ChecklistItem,
    onToggle: () -> Unit,
    onTextChange: (String) -> Unit,
    onEnter: () -> Unit,
    onBackspaceWhenEmpty: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChecklistCheckbox(checked = item.checked, onClick = onToggle)
        Spacer(Modifier.width(10.dp))
        // BasicTextField con onPreviewKeyEvent para Enter / Backspace.
        BasicTextField(
            value = item.text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .onPreviewKeyEvent { ev ->
                    if (ev.type == KeyEventType.KeyDown) {
                        when (ev.key) {
                            Key.Enter,
                            Key.NumPadEnter -> {
                                onEnter()
                                return@onPreviewKeyEvent true
                            }
                            Key.Backspace -> {
                                if (item.text.isEmpty()) {
                                    onBackspaceWhenEmpty()
                                    return@onPreviewKeyEvent true
                                }
                            }
                            else -> Unit
                        }
                    }
                    false
                },
            textStyle = LocalTextStyle.current.copy(
                color = if (item.checked) OreoPalette.OnSurfaceFaint else OreoPalette.OnSurface,
                fontSize = 17.sp,
                lineHeight = 24.sp,
                textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
            ),
            cursorBrush = SolidColor(OreoPalette.Accent),
            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { onEnter() }),
            singleLine = true,
            decorationBox = { inner ->
                Box {
                    if (item.text.isEmpty()) {
                        Text(
                            text = "Lista de pendientes",
                            color = OreoPalette.OnSurfaceFaint,
                            fontSize = 17.sp,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun ChecklistCheckbox(
    checked: Boolean,
    onClick: () -> Unit,
) {
    val bg by androidx.compose.animation.animateColorAsState(
        targetValue = if (checked) OreoPalette.Accent else Color.Transparent,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 180,
            easing = OreoMotion.EaseOut,
        ),
        label = "checkBg",
    )
    val border by androidx.compose.animation.animateColorAsState(
        targetValue = if (checked) OreoPalette.Accent else OreoPalette.Outline,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 180,
            easing = OreoMotion.EaseOut,
        ),
        label = "checkBorder",
    )
    val scale = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(checked) {
        // Pulse al togglearse — feedback visual.
        scale.snapTo(0.85f)
        scale.animateTo(1f, animationSpec = OreoMotion.SpringPress())
    }
    Box(
        modifier = Modifier
            .size(22.dp)
            .scale(scale.value)
            .clip(CircleShape)
            .background(bg)
            .border(
                width = 1.5.dp,
                color = border,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = checked,
            enter = fadeIn(tween(120, easing = OreoMotion.EaseOut)),
            exit = fadeOut(tween(120, easing = OreoMotion.EaseInOut)),
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun FormatToolbar(
    activeState: RichTextState?,
    onPickImage: () -> Unit,
    onPickVideo: () -> Unit,
    onInsertChecklist: () -> Unit,
    aiService: AiService?,
    onAiAction: (AiAction) -> Unit,
) {
    var linkDialog by remember { mutableStateOf(false) }

    val oreoWc = LocalOreoWindowSizeClass.current
    val toolbarMaxWidth = when (oreoWc.widthSizeClass) {
        OreoWidthSizeClass.Compact  -> Dp.Infinity
        OreoWidthSizeClass.Medium   -> 720.dp
        OreoWidthSizeClass.Expanded -> 820.dp
    }

    val infiniteTransition = rememberInfiniteTransition(label = "toolbarBorder")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "toolbarGradient",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        OreoPalette.SurfaceCard.copy(alpha = 0.95f),
                        OreoPalette.Bg1,
                    ),
                ),
            )
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
        ) {
            val width = size.width
            val gradient = Brush.linearGradient(
                colors = listOf(
                    OreoPalette.Accent.copy(alpha = 0f),
                    OreoPalette.Accent.copy(alpha = 0.6f),
                    OreoPalette.AccentSub.copy(alpha = 0.8f),
                    OreoPalette.Accent.copy(alpha = 0.6f),
                    OreoPalette.Accent.copy(alpha = 0f),
                ),
                start = Offset(gradientOffset * width - 200f, 0f),
                end = Offset(gradientOffset * width + 200f, 0f),
            )
            drawRect(gradient)
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = toolbarMaxWidth)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
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
                ToolbarButton(
                    icon = Icons.Outlined.CheckBox,
                    description = "Insertar checklist",
                    active = false,
                    tintActive = OreoPalette.AccentSub,
                    onClick = onInsertChecklist,
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
                    active = activeState?.currentSpanStyle?.textDecoration == androidx.compose.ui.text.style.TextDecoration.Underline,
                    onClick = { activeState?.toggleSpanStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) },
                )
                ToolbarButton(
                    icon = Icons.Outlined.FormatStrikethrough,
                    description = "Tachado",
                    active = activeState?.currentSpanStyle?.textDecoration == androidx.compose.ui.text.style.TextDecoration.LineThrough,
                    onClick = { activeState?.toggleSpanStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)) },
                )
                VerticalDivider()
                HeadingDropdownButton(activeState)
                CodeToolbarButton(activeState)
                TextColorToolbarButton(activeState)
                AlignmentDropdownButton(activeState)
                HighlightToolbarButton(activeState)
                ToolbarButton(
                    icon = Icons.Outlined.Link,
                    description = "Insertar enlace",
                    active = false,
                    onClick = { linkDialog = true },
                )
                VerticalDivider()
                AiToolbarButton(
                    activeState = activeState,
                    aiService = aiService,
                    onAiAction = onAiAction,
                )
            }
        }
    }

    if (linkDialog) {
        LinkDialog(
            initialUrl = "",
            initialText = "",
            onDismiss = { linkDialog = false },
            onConfirm = { url, text ->
                val s = activeState
                if (s != null && url.isNotBlank()) {
                    val finalUrl = if (url.contains("://")) url else "https://$url"
                    if (!s.selection.collapsed) {
                        s.addLinkToSelection(finalUrl)
                    } else {
                        val display = text.ifBlank { url }
                        s.addLink(text = display, url = finalUrl)
                    }
                }
                linkDialog = false
            },
        )
    }
}

