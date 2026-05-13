package com.oreoexperience.notes.ui.editor

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oreoexperience.notes.data.Discurso
import com.oreoexperience.notes.data.DiscursoRepository
import com.oreoexperience.notes.data.MediaStorage
import com.oreoexperience.notes.data.NoteBlock
import com.oreoexperience.notes.data.NoteBlockSerializer
import com.oreoexperience.notes.data.NoteCategory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Estado visible del indicador de guardado en la top bar.
 */
enum class SaveStatus { Idle, Dirty, Saving, Saved }

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

    // Pila de snapshots para deshacer cambios. Pusheamos el estado
    // anterior cada vez que el usuario hace una modificación, con un
    // debounce de 1.5 s para que rachas de tecleo no llenen la pila.
    // Capacidad fija: si se llena, se descartan los más viejos.
    private val undoStack = ArrayDeque<EditorUiState>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private var lastHistoryPushAt = 0L

    private fun pushHistory() {
        val now = System.currentTimeMillis()
        if (now - lastHistoryPushAt < HISTORY_DEBOUNCE_MS) return
        lastHistoryPushAt = now
        val cur = _state.value
        if (!cur.loaded) return
        // Evita pushear duplicados consecutivos.
        val top = undoStack.lastOrNull()
        if (top != null && top.title == cur.title && top.blocks == cur.blocks &&
            top.targetDurationSec == cur.targetDurationSec && top.pinned == cur.pinned
        ) return
        undoStack.addLast(cur)
        while (undoStack.size > UNDO_MAX) undoStack.removeFirst()
        _canUndo.value = undoStack.isNotEmpty()
    }

    /** Vuelve atrás un paso en el historial. Devuelve false si no hay nada que deshacer. */
    fun undo(): Boolean {
        val prev = undoStack.removeLastOrNull() ?: return false
        // Forzamos el push del próximo cambio (rompemos el debounce).
        lastHistoryPushAt = 0L
        _state.value = prev.copy(
            saveStatus = SaveStatus.Dirty,
            restoreVersion = _state.value.restoreVersion + 1,
        )
        _canUndo.value = undoStack.isNotEmpty()
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
        pushHistory()
        _state.value = _state.value.copy(title = v)
        markDirty()
    }

    /** Reemplaza el markdown de un bloque de texto identificado por [id]. */
    fun updateTextBlock(id: String, markdown: String) {
        pushHistory()
        _state.value = _state.value.copy(
            blocks = _state.value.blocks.map { b ->
                if (b is NoteBlock.Text && b.id == id) b.copy(markdown = markdown) else b
            },
        )
        markDirty()
    }

    /**
     * Inserta un bloque media después del bloque de texto identificado
     * por [afterTextBlockId]. Si el bloque tenía texto, lo dividimos
     * en dos en la posición [splitOffset] (cursor) — el resto continúa
     * en un nuevo bloque de texto debajo del media.
     */
    fun insertMediaAfter(
        afterTextBlockId: String?,
        splitOffset: Int = -1,
        media: NoteBlock,
    ) {
        pushHistory()
        val blocks = _state.value.blocks.toMutableList()
        val idx = blocks.indexOfFirst { it.id == afterTextBlockId }
        if (idx == -1) {
            blocks.add(media)
            blocks.add(NoteBlock.Text(markdown = ""))
            _state.value = _state.value.copy(blocks = blocks)
            markDirty()
            return
        }
        val target = blocks[idx]
        if (target !is NoteBlock.Text) {
            blocks.add(idx + 1, media)
            blocks.add(idx + 2, NoteBlock.Text(markdown = ""))
            _state.value = _state.value.copy(blocks = blocks)
            markDirty()
            return
        }
        val md = target.markdown
        val cut = if (splitOffset in 0..md.length) splitOffset else md.length
        val before = md.substring(0, cut)
        val after = md.substring(cut)
        blocks[idx] = target.copy(markdown = before)
        blocks.add(idx + 1, media)
        blocks.add(idx + 2, NoteBlock.Text(markdown = after))
        _state.value = _state.value.copy(blocks = blocks)
        markDirty()
    }

    /** Elimina un bloque por id (y borra el archivo media si aplica). */
    fun removeBlock(id: String) {
        pushHistory()
        val blocks = _state.value.blocks.toMutableList()
        val idx = blocks.indexOfFirst { it.id == id }
        if (idx == -1) return
        val removed = blocks.removeAt(idx)
        if (removed is NoteBlock.Image) {
            viewModelScope.launch { mediaStorage.deleteIfExists(removed.fileName) }
        }
        if (removed is NoteBlock.Video) {
            viewModelScope.launch { mediaStorage.deleteIfExists(removed.fileName) }
        }
        var i = 0
        while (i < blocks.size - 1) {
            val a = blocks[i]
            val b = blocks[i + 1]
            if (a is NoteBlock.Text && b is NoteBlock.Text) {
                blocks[i] = a.copy(
                    markdown = if (a.markdown.isEmpty()) b.markdown
                    else if (b.markdown.isEmpty()) a.markdown
                    else "${a.markdown}\n${b.markdown}",
                )
                blocks.removeAt(i + 1)
            } else {
                i++
            }
        }
        if (blocks.isEmpty() || blocks.last() !is NoteBlock.Text) {
            blocks.add(NoteBlock.Text(markdown = ""))
        }
        _state.value = _state.value.copy(blocks = blocks)
        markDirty()
    }

    fun setTargetDuration(seconds: Int) {
        pushHistory()
        _state.value = _state.value.copy(targetDurationSec = seconds)
        markDirty()
    }

    /** Atajo legacy usado por la UI: recibe minutos y convierte a segundos. */
    fun setTargetMinutes(minutes: Int) {
        setTargetDuration(minutes * 60)
    }

    fun togglePin() {
        pushHistory()
        _state.value = _state.value.copy(pinned = !_state.value.pinned)
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
        private const val UNDO_MAX = 30
        private const val HISTORY_DEBOUNCE_MS = 1500L
    }
}
