package com.oreoexperience.notes.ui.editor

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oreoexperience.notes.data.ChecklistItem
import com.oreoexperience.notes.data.Discurso
import com.oreoexperience.notes.data.DiscursoRepository
import com.oreoexperience.notes.data.MediaStorage
import com.oreoexperience.notes.data.NoteBlock
import com.oreoexperience.notes.data.NoteBlockSerializer
import com.oreoexperience.notes.data.NoteCategory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Estado visible del indicador de guardado en la top bar.
 */
enum class SaveStatus { Idle, Dirty, Saving, Saved }

/**
 * Evento de UI emitido tras un undo/redo. La pantalla del editor lo
 * consume con un Snackbar y un botón "Deshacer / Rehacer" que invierte
 * la operación recién efectuada.
 */
data class HistoryUiEvent(
    val label: String,
    val kind: Kind,
) {
    enum class Kind { Undone, Redone }
}

/**
 * Estado de UI del editor.
 *
 *   - [id]: el id real de la fila en la base de datos. **Siempre != 0**
 *     una vez que [loaded] es true: al entrar al editor con un id=0
 *     (nota nueva) creamos inmediatamente la fila vacía y este campo
 *     pasa a ser el id asignado por Room.
 *   - [title]: título de la nota.
 *   - [blocks]: lista ordenada de [NoteBlock] (texto markdown + media).
 *     Hay siempre al menos un bloque de texto.
 *   - [targetDurationSec]: cronómetro de la nota (0 = sin objetivo).
 *   - [pinned]: si está fijada al tope del listado.
 *   - [loaded]: false hasta que terminamos la carga inicial (o la
 *     creación de la fila vacía para una nota nueva).
 *   - [saveStatus]: indicador visible (Sin guardar / Guardando / Guardado).
 */
data class EditorUiState(
    val id: Long = 0L,
    val title: String = "",
    val blocks: List<NoteBlock> = listOf(NoteBlock.Text(markdown = "")),
    val targetDurationSec: Int = 0,
    val pinned: Boolean = false,
    val category: NoteCategory = NoteCategory.DISCURSO,
    val loaded: Boolean = false,
    val isSaving: Boolean = false,
    val saveStatus: SaveStatus = SaveStatus.Idle,
    /**
     * Aumenta cada vez que se hace undo. La UI lo usa como key para
     * forzar la re-sincronización del estado interno del editor de
     * texto enriquecido (que de otro modo conservaría el contenido
     * tecleado por el usuario y no aplicaría la versión revertida).
     */
    val restoreVersion: Long = 0L,
    /** Timestamps originales de la nota, para mostrar tiempo relativo en la top bar. */
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    val isEmpty: Boolean
        get() = title.isBlank() && blocks.all { b ->
            b is NoteBlock.Text && b.markdown.isBlank()
        }
}

/**
 * ViewModel del editor — modelo basado en bloques con auto-guardado
 * eager.
 *
 * Estrategia de guardado (v3, después de varios bugs):
 *
 *   1. **Insert inmediato al entrar a una nota nueva.** En lugar de
 *      mantener la nota en memoria con id=0 hasta el primer save, al
 *      hacer `load(0L)` insertamos *ya* una fila vacía y guardamos el
 *      id real en el estado. Ventajas:
 *        - Cada modificación posterior es un simple UPDATE WHERE id=X.
 *        - El usuario puede crear notas vacías y editarlas más tarde
 *          (aparecen en el listado del home desde el primer momento).
 *        - Se elimina la transición frágil id=0 → id real a mitad de
 *          sesión que arrastraba bugs cuando el ciclo de vida cortaba
 *          el ViewModel antes de propagarse.
 *
 *   2. **Auto-save por canal CONFLATED + loop.** Cada cambio dispara
 *      una señal en un Channel(CONFLATED). Un loop consumidor toma
 *      la señal, espera 200 ms para coalescer ráfagas de tecla, drena
 *      señales acumuladas y llama `persist()`. Esto garantiza que un
 *      usuario que escribe sin parar reciba un save cada ~200 ms —
 *      el bug anterior (`Job` debounceado con cancel+reschedule) podía
 *      cancelar el save indefinidamente.
 *
 *   3. **Persist serializado por Mutex.** Para que dos saves
 *      concurrentes nunca pisen ids ni timestamps.
 *
 *   4. **Flush explícito en lifecycle ON_PAUSE y en back.** Red de
 *      seguridad si el usuario mata la app entre el último cambio y
 *      el próximo tick del canal.
 */
class EditorViewModel(
    private val repository: DiscursoRepository,
    private val mediaStorage: MediaStorage,
) : ViewModel() {

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private val saveSignal = Channel<Unit>(capacity = Channel.CONFLATED)
    private val persistMutex = Mutex()
    private var savedFlashJob: kotlinx.coroutines.Job? = null

    // Historial basado en acciones tipadas (ver EditorHistory.kt).
    // Reemplaza el sistema antiguo de snapshots completos del
    // EditorUiState. Ventajas:
    //  - Memoria O(deltas) en lugar de O(estado * pasos).
    //  - Coalescencia inteligente: tipear durante 5 s genera UNA sola
    //    entrada de undo en lugar de la cantidad arbitraria que
    //    cupiera en el debounce. Acciones discretas (toggle pin,
    //    insertar bloque, marcar checkbox, aplicar formato) rompen la
    //    cadena, así que cada paso de undo se siente atómico.
    //  - Etiqueta legible que se muestra en el snackbar al
    //    deshacer/rehacer ("editar texto", "insertar imagen", etc.).
    private val history = EditorHistoryManager(maxSize = 60)
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    /**
     * Eventos de un solo disparo para mostrar al usuario el resultado
     * de un undo/redo. La UI los consume con un snackbar y un botón
     * para revertir el revert. Usamos `SharedFlow` con replay=0 para
     * no re-mostrar eventos viejos al recomponer.
     */
    private val _historyEvents = MutableSharedFlow<HistoryUiEvent>(
        replay = 0,
        extraBufferCapacity = 8,
    )
    val historyEvents: SharedFlow<HistoryUiEvent> = _historyEvents.asSharedFlow()

    private fun refreshHistoryFlags() {
        _canUndo.value = history.canUndo()
        _canRedo.value = history.canRedo()
    }

    private fun pushAction(action: EditAction) {
        if (!_state.value.loaded) return
        history.push(action)
        refreshHistoryFlags()
    }

    /**
     * Vuelve atrás un paso en el historial. Devuelve `true` si había
     * algo para deshacer.
     */
    fun undo(): Boolean {
        val step = history.undo(_state.value) ?: return false
        _state.value = step.newState.copy(
            saveStatus = SaveStatus.Dirty,
            restoreVersion = _state.value.restoreVersion + 1,
        )
        refreshHistoryFlags()
        _historyEvents.tryEmit(HistoryUiEvent(label = step.label, kind = HistoryUiEvent.Kind.Undone))
        saveSignal.trySend(Unit)
        return true
    }

    /**
     * Rehace un paso previamente deshecho. Devuelve `true` si había
     * algo para rehacer.
     */
    fun redo(): Boolean {
        val step = history.redo(_state.value) ?: return false
        _state.value = step.newState.copy(
            saveStatus = SaveStatus.Dirty,
            restoreVersion = _state.value.restoreVersion + 1,
        )
        refreshHistoryFlags()
        _historyEvents.tryEmit(HistoryUiEvent(label = step.label, kind = HistoryUiEvent.Kind.Redone))
        saveSignal.trySend(Unit)
        return true
    }

    init {
        viewModelScope.launch {
            saveSignal.consumeAsFlow().collect {
                // Throttle corto para agrupar ráfagas de tecla.
                delay(200)
                // Drena cualquier señal acumulada durante el delay.
                while (saveSignal.tryReceive().isSuccess) { /* drain */ }
                runCatching { persist() }.onFailure {
                    Log.e(TAG, "auto-save persist falló", it)
                }
            }
        }
    }

    /**
     * Carga inicial. Si [id] es 0L crea una fila vacía y la usa como
     * la nota activa; en caso contrario carga la nota existente.
     *
     * Llamado por la UI en `LaunchedEffect(discursoId)`. Es idempotente:
     * si ya cargamos esta nota, no hace nada.
     */
    fun load(id: Long, initialCategoryKey: String? = null) {
        val current = _state.value
        if (current.loaded && (current.id == id || (id == 0L && current.id != 0L))) {
            Log.d(TAG, "load($id) — ya cargado (state.id=${current.id}), skip")
            return
        }
        // Cargar otra nota = empezar con una pila de undo limpia.
        history.clear()
        refreshHistoryFlags()
        val initialCategory = NoteCategory.fromKey(initialCategoryKey)
        viewModelScope.launch {
            if (id == 0L) {
                // Nueva nota: insertamos una fila vacía YA y trabajamos
                // sobre ese id real durante toda la sesión.
                val now = System.currentTimeMillis()
                val newId = repository.upsert(
                    Discurso(
                        id = 0,
                        title = "",
                        scriptures = "",
                        tags = "",
                        pointsJson = "[]",
                        notes = "",
                        createdAt = now,
                        updatedAt = now,
                        targetDurationSec = 0,
                        pinned = false,
                        deletedAt = null,
                        category = initialCategory.key,
                    ),
                )
                Log.d(TAG, "load(0) — insertada fila vacía id=$newId")
                _state.value = EditorUiState(
                    id = newId,
                    category = initialCategory,
                    loaded = true,
                )
                return@launch
            }
            val d = repository.get(id)
            if (d != null) {
                val migratedBody = buildLegacyBody(d)
                _state.value = EditorUiState(
                    id = d.id,
                    title = d.title,
                    blocks = NoteBlockSerializer.decode(migratedBody),
                    targetDurationSec = d.targetDurationSec,
                    pinned = d.pinned,
                    category = NoteCategory.fromKey(d.category),
                    loaded = true,
                    createdAt = d.createdAt,
                    updatedAt = d.updatedAt,
                )
                Log.d(TAG, "load($id) — cargada (title='${d.title}', notes.len=${d.notes.length})")
            } else {
                Log.w(TAG, "load($id) — no existe en DB, creando fila vacía")
                val now = System.currentTimeMillis()
                val newId = repository.upsert(
                    Discurso(
                        id = 0,
                        createdAt = now,
                        updatedAt = now,
                        category = initialCategory.key,
                    ),
                )
                _state.value = EditorUiState(
                    id = newId,
                    category = initialCategory,
                    loaded = true,
                    createdAt = now,
                    updatedAt = now,
                )
            }
        }
    }

    private fun buildLegacyBody(d: Discurso): String {
        val sectionsText = if (d.pointsJson.isNotBlank() && d.pointsJson != "[]") {
            val pts = repository.decodePoints(d.pointsJson).map { it.migrateLegacy() }
            pts.joinToString(separator = "\n\n") { p ->
                when {
                    p.text.isBlank() -> p.body
                    p.body.isBlank() -> "## ${p.text}"
                    else -> "## ${p.text}\n\n${p.body}"
                }
            }
        } else ""

        return listOf(sectionsText, d.notes)
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
    }

    private fun markDirty() {
        _state.value = _state.value.copy(saveStatus = SaveStatus.Dirty)
        saveSignal.trySend(Unit)
    }

    /**
     * Persiste el estado actual. Idempotente y serializado por mutex.
     * No filtra por isEmpty — incluso una nota vacía se sigue
     * actualizando en disco (la fila existe desde el load).
     */
    private suspend fun persist() = persistMutex.withLock {
        val s = _state.value
        if (!s.loaded || s.id == 0L) {
            Log.d(TAG, "persist() — state no listo (loaded=${s.loaded}, id=${s.id}), skip")
            return@withLock
        }
        _state.value = s.copy(saveStatus = SaveStatus.Saving, isSaving = true)
        val body = NoteBlockSerializer.encode(s.blocks)
        val existing = repository.get(s.id)
        val d = Discurso(
            id = s.id,
            title = s.title.trim(),
            scriptures = existing?.scriptures ?: "",
            tags = existing?.tags ?: "",
            pointsJson = existing?.pointsJson ?: "[]",
            notes = body,
            targetDurationSec = s.targetDurationSec,
            pinned = s.pinned,
            deletedAt = existing?.deletedAt,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            category = s.category.key,
        )
        repository.upsert(d)
        Log.d(TAG, "persist() — guardado id=${s.id} title='${s.title}' body.len=${body.length}")
        _state.value = _state.value.copy(
            isSaving = false,
            saveStatus = SaveStatus.Saved,
        )
        // "Guardado" visible 1.5 s y vuelve a Idle.
        savedFlashJob?.cancel()
        savedFlashJob = viewModelScope.launch {
            delay(1500)
            if (_state.value.saveStatus == SaveStatus.Saved) {
                _state.value = _state.value.copy(saveStatus = SaveStatus.Idle)
            }
        }
    }

    /** Fuerza un save inmediato (botón "Guardar" explícito o lifecycle). */
    fun saveNow() {
        viewModelScope.launch {
            runCatching { persist() }.onFailure {
                Log.e(TAG, "saveNow() falló", it)
            }
        }
    }

    /**
     * Guarda y devuelve el id. A diferencia de [saveNow], esta variante
     * suspende hasta que el disco haya recibido la versión final — la
     * usa el flujo de back/swipe-back para garantizar que cuando el
     * editor se desmonte el contenido esté en disco.
     */
    suspend fun saveBlocking(): Long {
        runCatching { persist() }.onFailure {
            Log.e(TAG, "saveBlocking() falló", it)
        }
        return _state.value.id
    }

    fun setTitle(v: String) {
        val before = _state.value.title
        if (before == v) return
        pushAction(TitleEdit(before = before, after = v))
        _state.value = _state.value.copy(title = v)
        markDirty()
    }

    /** Reemplaza el markdown de un bloque de texto identificado por [id]. */
    fun updateTextBlock(id: String, markdown: String) {
        val current = _state.value.blocks
        val target = current.firstOrNull { it is NoteBlock.Text && it.id == id } as? NoteBlock.Text
            ?: return
        if (target.markdown == markdown) return
        pushAction(
            TextBlockEdit(
                blockId = id,
                before = target.markdown,
                after = markdown,
            ),
        )
        _state.value = _state.value.copy(
            blocks = current.map { b ->
                if (b is NoteBlock.Text && b.id == id) b.copy(markdown = markdown) else b
            },
        )
        markDirty()
    }

    /**
     * Inserta un bloque media después del bloque de texto identificado
     * por [afterTextBlockId]. El bloque de texto se divide en dos: el
     * contenido antes del cursor queda en el bloque original, la imagen
     * se inserta después, y el contenido después del cursor va en un
     * nuevo bloque debajo de la imagen.
     *
     * IMPORTANTE: [splitOffset] es un offset de texto plano (del
     * RichTextState), pero el markdown almacenado es HTML. Para dividir
     * correctamente convertimos ambos lados a HTML independiente.
     */
    fun insertMediaAfter(
        afterTextBlockId: String?,
        splitOffset: Int = -1,
        media: NoteBlock,
    ) {
        val blocks = _state.value.blocks.toMutableList()
        var idx = blocks.indexOfFirst { it.id == afterTextBlockId }
        if (idx == -1) {
            val lastTextIdx = blocks.indexOfLast { it is NoteBlock.Text }
            idx = if (lastTextIdx >= 0) lastTextIdx else blocks.size - 1
            if (idx < 0) idx = 0
        }
        val target = blocks[idx]
        if (target !is NoteBlock.Text) {
            val tail = NoteBlock.Text(markdown = "")
            pushAction(
                InsertBlocks(
                    index = idx + 1,
                    inserted = listOf(media, tail),
                ),
            )
            blocks.add(idx + 1, media)
            blocks.add(idx + 2, tail)
            _state.value = _state.value.copy(blocks = blocks)
            markDirty()
            return
        }

        val md = target.markdown
        val plainText = htmlToPlainText(md)

        if (splitOffset < 0 || splitOffset > plainText.length) {
            val tail = NoteBlock.Text(markdown = "")
            pushAction(
                InsertBlocks(
                    index = idx + 1,
                    inserted = listOf(media, tail),
                ),
            )
            blocks.add(idx + 1, media)
            blocks.add(idx + 2, tail)
            _state.value = _state.value.copy(blocks = blocks)
            markDirty()
            return
        }

        val safeOffset = splitOffset.coerceIn(0, plainText.length)
        val charIndex = markdownCharIndexForPlainTextOffset(md, safeOffset)
        val before = md.substring(0, charIndex)
        val after = md.substring(charIndex)
        val targetSplit = target.copy(markdown = before)
        val tail = NoteBlock.Text(markdown = after)
        pushAction(
            InsertBlocks(
                index = idx + 1,
                inserted = listOf(media, tail),
                splitTextBlockBefore = targetSplit,
                splitTextBlockAfter = target,
            ),
        )
        blocks[idx] = targetSplit
        blocks.add(idx + 1, media)
        blocks.add(idx + 2, tail)
        _state.value = _state.value.copy(blocks = blocks)
        markDirty()
    }

    /** Inserta un bloque de cualquier tipo después del bloque de texto actual. */
    fun insertBlockAfter(afterTextBlockId: String?, block: NoteBlock) {
        val blocks = _state.value.blocks.toMutableList()
        val idx = blocks.indexOfFirst { it.id == afterTextBlockId }
        if (idx == -1) {
            blocks.add(block)
            val tail = NoteBlock.Text(markdown = "")
            blocks.add(tail)
            pushAction(InsertBlocks(index = blocks.size - 2, inserted = listOf(block, tail)))
            _state.value = _state.value.copy(blocks = blocks)
            markDirty()
            return
        }
        val target = blocks[idx]
        if (target !is NoteBlock.Text) {
            val tail = NoteBlock.Text(markdown = "")
            pushAction(InsertBlocks(index = idx + 1, inserted = listOf(block, tail)))
            blocks.add(idx + 1, block)
            blocks.add(idx + 2, tail)
            _state.value = _state.value.copy(blocks = blocks)
            markDirty()
            return
        }
        pushAction(InsertBlocks(index = idx + 1, inserted = listOf(block)))
        blocks.add(idx + 1, block)
        _state.value = _state.value.copy(blocks = blocks)
        markDirty()
    }

    /** Actualiza el texto de un bloque e inserta un nuevo bloque después, en una sola operación atómica. */
    fun updateTextAndInsertBlockAfter(blockId: String, newMarkdown: String, newBlock: NoteBlock) {
        val blocks = _state.value.blocks.toMutableList()
        val idx = blocks.indexOfFirst { it.id == blockId }
        if (idx == -1) return
        
        // Update the current block
        val updatedBlock = blocks[idx]
        if (updatedBlock is NoteBlock.Text) {
            blocks[idx] = updatedBlock.copy(markdown = newMarkdown)
        }
        
        // Insert the new block after
        blocks.add(idx + 1, newBlock)
        
        pushAction(InsertBlocks(index = idx + 1, inserted = listOf(newBlock)))
        _state.value = _state.value.copy(blocks = blocks)
        markDirty()
    }

    /** Elimina un bloque por id (y borra el archivo media si aplica). */
    fun removeBlock(id: String) {
        val originalBlocks = _state.value.blocks
        val idx = originalBlocks.indexOfFirst { it.id == id }
        if (idx == -1) return
        val removed = originalBlocks[idx]
        // Computamos el resultado del merge en una lista nueva sin
        // mutar el estado todavía. Así podemos guardar ambos snapshots
        // (antes y después) en la acción para el undo/redo.
        val mutated = originalBlocks.toMutableList()
        mutated.removeAt(idx)
        var i = 0
        while (i < mutated.size - 1) {
            val a = mutated[i]
            val b = mutated[i + 1]
            if (a is NoteBlock.Text && b is NoteBlock.Text) {
                mutated[i] = a.copy(
                    markdown = if (a.markdown.isEmpty()) b.markdown
                    else if (b.markdown.isEmpty()) a.markdown
                    else "${a.markdown}\n${b.markdown}",
                )
                mutated.removeAt(i + 1)
            } else {
                i++
            }
        }
        if (mutated.isEmpty() || mutated.last() !is NoteBlock.Text) {
            mutated.add(NoteBlock.Text(markdown = ""))
        }
        pushAction(
            RemoveBlock(
                index = idx,
                removed = removed,
                mergedTextSnapshot = mutated.toList(),
                originalSnapshot = originalBlocks.toList(),
            ),
        )
        _state.value = _state.value.copy(blocks = mutated)
        markDirty()
        if (removed is NoteBlock.Image) {
            viewModelScope.launch { mediaStorage.deleteIfExists(removed.fileName) }
        }
        if (removed is NoteBlock.Video) {
            viewModelScope.launch { mediaStorage.deleteIfExists(removed.fileName) }
        }
    }

    /**
     * Inserta un nuevo [NoteBlock.Checklist] vacío después del bloque
     * de texto enfocado [afterTextBlockId], usando la misma mecánica
     * de partido del texto que [insertMediaAfter]. Si no hay foco,
     * se agrega al final.
     */
    fun insertChecklistAfter(
        afterTextBlockId: String?,
        splitOffset: Int = -1,
    ) {
        val newChecklist = NoteBlock.Checklist(
            items = listOf(ChecklistItem(text = "", checked = false)),
        )
        val blocks = _state.value.blocks.toMutableList()
        val idx = blocks.indexOfFirst { it.id == afterTextBlockId }
        if (idx == -1) {
            val tail = NoteBlock.Text(markdown = "")
            pushAction(
                InsertBlocks(
                    index = blocks.size,
                    inserted = listOf(newChecklist, tail),
                ),
            )
            blocks.add(newChecklist)
            blocks.add(tail)
            _state.value = _state.value.copy(blocks = blocks)
            markDirty()
            return
        }
        val target = blocks[idx]
        if (target !is NoteBlock.Text) {
            val tail = NoteBlock.Text(markdown = "")
            pushAction(
                InsertBlocks(
                    index = idx + 1,
                    inserted = listOf(newChecklist, tail),
                ),
            )
            blocks.add(idx + 1, newChecklist)
            blocks.add(idx + 2, tail)
            _state.value = _state.value.copy(blocks = blocks)
            markDirty()
            return
        }
        val md = target.markdown
        val plainText = htmlToPlainText(md)

        if (splitOffset < 0 || splitOffset > plainText.length) {
            val tail = NoteBlock.Text(markdown = "")
            pushAction(
                InsertBlocks(
                    index = idx + 1,
                    inserted = listOf(newChecklist, tail),
                ),
            )
            blocks.add(idx + 1, newChecklist)
            blocks.add(idx + 2, tail)
            _state.value = _state.value.copy(blocks = blocks)
            markDirty()
            return
        }

        val safeOffset = splitOffset.coerceIn(0, plainText.length)
        val charIndex = markdownCharIndexForPlainTextOffset(md, safeOffset)
        val before = md.substring(0, charIndex)
        val after = md.substring(charIndex)
        val targetSplit = target.copy(markdown = before)
        val tail = NoteBlock.Text(markdown = after)
        pushAction(
            InsertBlocks(
                index = idx + 1,
                inserted = listOf(newChecklist, tail),
                splitTextBlockBefore = targetSplit,
                splitTextBlockAfter = target,
            ),
        )
        blocks[idx] = targetSplit
        blocks.add(idx + 1, newChecklist)
        blocks.add(idx + 2, tail)
        _state.value = _state.value.copy(blocks = blocks)
        markDirty()
    }

    /** Cambia el texto de un ítem de un checklist (no marca history en cada tecla). */
    fun updateChecklistItemText(blockId: String, itemId: String, text: String) {
        val current = _state.value.blocks
        val checklist = current.firstOrNull { it is NoteBlock.Checklist && it.id == blockId } as? NoteBlock.Checklist
            ?: return
        val item = checklist.items.firstOrNull { it.id == itemId } ?: return
        if (item.text == text) return
        pushAction(
            ChecklistItemTextEdit(
                blockId = blockId,
                itemId = itemId,
                before = item.text,
                after = text,
            ),
        )
        _state.value = _state.value.copy(
            blocks = current.map { b ->
                if (b is NoteBlock.Checklist && b.id == blockId) {
                    b.copy(items = b.items.map { it ->
                        if (it.id == itemId) it.copy(text = text) else it
                    })
                } else b
            },
        )
        markDirty()
    }

    /** Marca / desmarca un ítem de checklist. */
    fun toggleChecklistItem(blockId: String, itemId: String) {
        val current = _state.value.blocks
        val exists = current.any { b ->
            b is NoteBlock.Checklist && b.id == blockId && b.items.any { it.id == itemId }
        }
        if (!exists) return
        pushAction(ChecklistItemToggleAction(blockId = blockId, itemId = itemId))
        _state.value = _state.value.copy(
            blocks = current.map { b ->
                if (b is NoteBlock.Checklist && b.id == blockId) {
                    b.copy(items = b.items.map { it ->
                        if (it.id == itemId) it.copy(checked = !it.checked) else it
                    })
                } else b
            },
        )
        markDirty()
    }

    /** Agrega un ítem vacío al final del checklist. */
    fun addChecklistItem(blockId: String): String? {
        val current = _state.value.blocks
        val checklist = current.firstOrNull { it is NoteBlock.Checklist && it.id == blockId } as? NoteBlock.Checklist
            ?: return null
        val newItem = ChecklistItem(text = "", checked = false)
        pushAction(ChecklistItemAdd(blockId = blockId, item = newItem))
        _state.value = _state.value.copy(
            blocks = current.map { b ->
                if (b is NoteBlock.Checklist && b.id == blockId) b.copy(items = b.items + newItem) else b
            },
        )
        markDirty()
        return newItem.id
    }

    /**
     * Elimina un ítem del checklist. Si era el último, también elimina
     * el bloque entero (estilo iOS Notes).
     */
    fun removeChecklistItem(blockId: String, itemId: String) {
        val current = _state.value.blocks
        val idx = current.indexOfFirst { it is NoteBlock.Checklist && it.id == blockId }
        if (idx == -1) return
        val checklist = current[idx] as NoteBlock.Checklist
        val itemIdx = checklist.items.indexOfFirst { it.id == itemId }
        if (itemIdx == -1) return
        val item = checklist.items[itemIdx]
        val remaining = checklist.items.filter { it.id != itemId }
        if (remaining.isEmpty()) {
            // Borrado del bloque entero — reusamos la lógica de
            // removeBlock para que se mergeen vecinos de texto.
            removeBlock(blockId)
            return
        }
        pushAction(
            ChecklistItemRemove(
                blockId = blockId,
                item = item,
                originalIndex = itemIdx,
            ),
        )
        val mutated = current.toMutableList()
        mutated[idx] = checklist.copy(items = remaining)
        _state.value = _state.value.copy(blocks = mutated)
        markDirty()
    }

    fun setTargetDuration(seconds: Int) {
        val before = _state.value.targetDurationSec
        if (before == seconds) return
        pushAction(TargetDurationEdit(before = before, after = seconds))
        _state.value = _state.value.copy(targetDurationSec = seconds)
        markDirty()
    }

    /** Atajo legacy usado por la UI: recibe minutos y convierte a segundos. */
    fun setTargetMinutes(minutes: Int) {
        setTargetDuration(minutes * 60)
    }

    fun togglePin() {
        val before = _state.value.pinned
        pushAction(TogglePinAction(before = before))
        _state.value = _state.value.copy(pinned = !before)
        markDirty()
    }

    fun setCategory(category: NoteCategory) {
        if (_state.value.category == category) return
        _state.value = _state.value.copy(category = category)
        markDirty()
    }

    /**
     * Elimina la nota (soft-delete: va a la papelera). Usado por el
     * menú "Eliminar nota" desde el editor.
     */
    suspend fun deleteCurrent() {
        val s = _state.value
        if (s.id == 0L) return
        val d = repository.get(s.id) ?: return
        repository.trash(d)
    }

    companion object {
        private const val TAG = "EditorVM"
    }

    // ─── Find & Replace ──────────────────────────────────────────
    //
    // El VM sólo guarda **configuración** del panel (query, replacement,
    // flags, índice del match actual, panel abierto/cerrado). El cálculo
    // de matches lo hace la UI sobre `RichTextState.annotatedString.text`
    // porque sólo ahí los offsets son consistentes con `richState.selection`
    // y con `replaceTextRange`. Tratar de calcular matches acá usando el
    // HTML de `Discurso.notes` da resultados desincronizados.

    private val _findUiState = MutableStateFlow(FindUiState())
    val findUiState: StateFlow<FindUiState> = _findUiState.asStateFlow()

    fun openFindReplace() {
        _findUiState.value = _findUiState.value.copy(open = true)
    }

    fun closeFindReplace() {
        _findUiState.value = _findUiState.value.copy(open = false)
    }

    fun setFindQuery(query: String) {
        _findUiState.value = _findUiState.value.copy(query = query, currentIndex = 0)
    }

    fun setFindReplacement(replacement: String) {
        _findUiState.value = _findUiState.value.copy(replacement = replacement)
    }

    fun toggleFindCaseSensitive() {
        val s = _findUiState.value
        _findUiState.value = s.copy(caseSensitive = !s.caseSensitive, currentIndex = 0)
    }

    fun toggleFindUseRegex() {
        val s = _findUiState.value
        _findUiState.value = s.copy(useRegex = !s.useRegex, currentIndex = 0)
    }

    fun setFindCurrentIndex(index: Int) {
        _findUiState.value = _findUiState.value.copy(currentIndex = index)
    }

    /**
     * Reemplaza una coincidencia. El caller (UI) ya mutó el RichTextState
     * con `replaceTextRange`, así que acá sólo persistimos el HTML
     * resultante en el modelo y empujamos la acción de undo.
     */
    fun replaceCurrentMatch(
        blockId: String,
        beforeHtml: String,
        afterHtml: String,
    ) {
        if (beforeHtml == afterHtml) return
        pushAction(FindReplaceOne(blockId, beforeHtml, afterHtml))

        _state.value = _state.value.copy(
            blocks = _state.value.blocks.map { b ->
                if (b is NoteBlock.Text && b.id == blockId) b.copy(markdown = afterHtml) else b
            },
        )
        markDirty()
    }

    /**
     * Reemplaza todas las coincidencias en una sola operación atómica.
     * El caller (UI) ya mutó cada RichTextState; recibimos los HTML
     * resultantes para persistirlos.
     */
    fun replaceAllMatches(
        newBlockMarkdowns: Map<String, String>,
        snapshotBefore: List<NoteBlock>,
    ) {
        if (newBlockMarkdowns.isEmpty()) return
        val snapshotAfter = _state.value.blocks.map { b ->
            if (b is NoteBlock.Text && b.id in newBlockMarkdowns) {
                b.copy(markdown = newBlockMarkdowns.getValue(b.id))
            } else b
        }

        pushAction(FindReplaceAll(snapshotBefore, snapshotAfter))

        // Bumpamos restoreVersion para que cada TextBlockEditor recargue
        // su HTML desde el modelo. Eso garantiza que si hubo eventos
        // pendientes del snapshotFlow del rich text (cambios "intermedios"
        // del replaceTextRange) no terminen pisando el estado correcto.
        _state.value = _state.value.copy(
            blocks = snapshotAfter,
            restoreVersion = _state.value.restoreVersion + 1,
        )
        markDirty()
    }
}

/**
 * Convierte HTML simple (producido por rich-text-compose) a texto
 * plano, quitando todos los tags y decodificando entidades HTML.
 */
private fun htmlToPlainText(html: String): String {
    val sb = StringBuilder()
    var inTag = false
    var tagBuf = StringBuilder()
    var i = 0
    while (i < html.length) {
        val c = html[i]
        when {
            c == '<' -> { inTag = true; tagBuf.clear(); tagBuf.append(c) }
            c == '>' && inTag -> {
                inTag = false
                val tag = tagBuf.toString().lowercase().trim()
                if (tag.contains("br") || tag.startsWith("/p") || tag.startsWith("p ") || tag == "p") {
                    sb.append('\n')
                }
                i++
                continue
            }
            inTag -> { tagBuf.append(c); i++; continue }
            !inTag -> {
                when {
                    html.startsWith("&amp;", i) -> { sb.append('&'); i += 5; continue }
                    html.startsWith("&lt;", i) -> { sb.append('<'); i += 4; continue }
                    html.startsWith("&gt;", i) -> { sb.append('>'); i += 4; continue }
                    html.startsWith("&nbsp;", i) -> { sb.append(' '); i += 6; continue }
                    html.startsWith("&#39;", i) -> { sb.append('\''); i += 5; continue }
                    html.startsWith("&quot;", i) -> { sb.append('"'); i += 6; continue }
                    else -> sb.append(c)
                }
            }
        }
        i++
    }
    return sb.toString()
}

/**
 * Dado un HTML y un offset de texto plano, devuelve el índice
 * dentro del HTML donde se encuentra ese carácter plano.
 * Maneja correctamente tags HTML como `<b>`, `<br>`, `</p>`, etc.
 */
private fun markdownCharIndexForPlainTextOffset(html: String, offset: Int): Int {
    var plainCount = 0
    var inTag = false
    var tagBuf = StringBuilder()
    var inEntity = false
    var entityStart = -1
    for (i in html.indices) {
        val c = html[i]
        when {
            c == '<' -> { inTag = true; tagBuf.clear(); tagBuf.append(c); continue }
            c == '>' && inTag -> {
                inTag = false
                val tag = tagBuf.toString().lowercase().trim()
                if (tag.contains("br") || tag.startsWith("/p") || tag.startsWith("p ") || tag == "p") {
                    if (plainCount == offset) return i - tagBuf.length + 1
                    plainCount++
                }
                continue
            }
            inTag -> { tagBuf.append(c); continue }
            c == '&' -> { inEntity = true; entityStart = i; continue }
            inEntity && c == ';' -> {
                inEntity = false
                val entity = html.substring(entityStart, i + 1)
                val decoded = when (entity) {
                    "&amp;" -> '&'
                    "&lt;" -> '<'
                    "&gt;" -> '>'
                    "&nbsp;" -> ' '
                    "&#39;" -> '\''
                    "&quot;" -> '"'
                    else -> '?'
                }
                if (plainCount == offset) return entityStart
                plainCount++
                continue
            }
            inEntity -> continue
            else -> {
                if (plainCount == offset) return i
                plainCount++
            }
        }
    }
    return html.length
}
