package com.oreoexperience.notes.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oreoexperience.notes.data.Discurso
import com.oreoexperience.notes.data.DiscursoRepository
import com.oreoexperience.notes.data.MediaStorage
import com.oreoexperience.notes.data.NoteBlock
import com.oreoexperience.notes.data.NoteBlockSerializer
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
 * Estado del editor — modelo basado en **bloques** para soportar
 * imágenes y videos intercalados con texto:
 *
 *   - [title]: título de la nota.
 *   - [blocks]: lista ordenada de [NoteBlock] (texto markdown,
 *     imágenes, videos). Hay siempre al menos un bloque de texto.
 *   - [targetDurationSec]: si > 0, se muestra la barra inferior de
 *     cronómetro.
 *
 * Migración legacy automática: si se carga un Discurso viejo (con
 * `pointsJson` no vacío y/o `notes` plano), se concatena todo en
 * un único bloque de texto al cargar. Al guardar, los bloques se
 * serializan al string markdown extendido (con marcadores de media)
 * que se persiste en `Discurso.notes`.
 */
/** Estado visible del indicador de guardado en la top bar. */
enum class SaveStatus { Idle, Dirty, Saving, Saved }

data class EditorUiState(
    val id: Long = 0L,
    val title: String = "",
    val blocks: List<NoteBlock> = listOf(NoteBlock.Text(markdown = "")),
    val targetDurationSec: Int = 0,
    val pinned: Boolean = false,
    val loaded: Boolean = false,
    val isSaving: Boolean = false,
    val saveStatus: SaveStatus = SaveStatus.Idle,
) {
    val isNew: Boolean get() = id == 0L
    val isEmpty: Boolean
        get() = title.isBlank() && blocks.all { b ->
            b is NoteBlock.Text && b.markdown.isBlank()
        }
}

class EditorViewModel(
    private val repository: DiscursoRepository,
    private val mediaStorage: MediaStorage,
) : ViewModel() {

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    // ---- AUTO-SAVE ROBUSTO ----
    //
    // El esquema anterior (cancel + reschedule en cada tecla) tenía un
    // problema: si el usuario escribía sin pausar 600ms, el job se
    // cancelaba indefinidamente y persist() NUNCA se ejecutaba. La nota
    // podía quedar sin persistir aunque hubiera mucho texto.
    //
    // Solución: un Channel(CONFLATED) + un loop único. Cada edición
    // hace trySend (no bloquea). El consumidor toma la señal, espera un
    // throttle corto, drena señales acumuladas y llama persist(). Así
    // un usuario que escribe sin parar dispara un save cada ~250ms y
    // los datos nunca se pierden.
    private val saveSignal = Channel<Unit>(capacity = Channel.CONFLATED)
    private val persistMutex = Mutex()
    private var savedFlashJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            saveSignal.consumeAsFlow().collect {
                // Throttle: agrupa ráfagas de tecla en un solo save.
                delay(250)
                // Drena cualquier señal adicional acumulada durante el delay.
                while (saveSignal.tryReceive().isSuccess) { /* drain */ }
                persist()
            }
        }
    }

    private fun markDirty() {
        // Marcamos el estado y disparamos una señal de save no-bloqueante.
        _state.value = _state.value.copy(saveStatus = SaveStatus.Dirty)
        saveSignal.trySend(Unit)
    }

    /**
     * Persiste el estado actual. Idempotente y protegido por mutex para
     * evitar dos persist() concurrentes pisando ids.
     */
    private suspend fun persist() = persistMutex.withLock {
        val s = _state.value
        if (s.isEmpty) {
            _state.value = s.copy(saveStatus = SaveStatus.Idle, isSaving = false)
            return@withLock
        }
        _state.value = s.copy(saveStatus = SaveStatus.Saving, isSaving = true)
        val body = NoteBlockSerializer.encode(s.blocks)
        val existing = if (s.id != 0L) repository.get(s.id) else null
        val d = Discurso(
            id = s.id,
            title = s.title.trim(),
            scriptures = "",
            tags = "",
            pointsJson = "[]",
            notes = body,
            targetDurationSec = s.targetDurationSec,
            pinned = s.pinned,
            deletedAt = null,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
        )
        val newId = repository.upsert(d)
        _state.value = _state.value.copy(
            id = newId,
            isSaving = false,
            saveStatus = SaveStatus.Saved,
        )
        // "Guardado" visible 1.5s y vuelve a Idle.
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
        viewModelScope.launch { persist() }
    }

    fun load(id: Long) {
        if (_state.value.loaded && _state.value.id == id) return
        viewModelScope.launch {
            if (id == 0L) {
                _state.value = EditorUiState(loaded = true)
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
                    loaded = true,
                )
            } else {
                _state.value = EditorUiState(loaded = true)
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

    fun setTitle(v: String) {
        _state.value = _state.value.copy(title = v)
        markDirty()
    }

    /** Reemplaza el markdown de un bloque de texto identificado por [id]. */
    fun updateTextBlock(id: String, markdown: String) {
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
     * en un nuevo bloque de texto debajo del media. Así el flujo de
     * escritura no se interrumpe.
     */
    fun insertMediaAfter(
        afterTextBlockId: String?,
        splitOffset: Int = -1,
        media: NoteBlock,
    ) {
        val blocks = _state.value.blocks.toMutableList()
        val idx = blocks.indexOfFirst { it.id == afterTextBlockId }
        if (idx == -1) {
            // No hay foco — insertamos al final + nuevo bloque de texto vacío.
            blocks.add(media)
            blocks.add(NoteBlock.Text(markdown = ""))
            _state.value = _state.value.copy(blocks = blocks)
            return
        }
        val target = blocks[idx]
        if (target !is NoteBlock.Text) {
            blocks.add(idx + 1, media)
            blocks.add(idx + 2, NoteBlock.Text(markdown = ""))
            _state.value = _state.value.copy(blocks = blocks)
            return
        }
        val md = target.markdown
        val cut = if (splitOffset in 0..md.length) splitOffset else md.length
        val before = md.substring(0, cut)
        val after = md.substring(cut)
        // Reemplazamos el bloque de texto por: antes + media + después
        blocks[idx] = target.copy(markdown = before)
        blocks.add(idx + 1, media)
        blocks.add(idx + 2, NoteBlock.Text(markdown = after))
        _state.value = _state.value.copy(blocks = blocks)
    }

    /** Elimina un bloque por id (y borra el archivo media si aplica). */
    fun removeBlock(id: String) {
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
        // Si después de borrar quedan dos bloques de texto adyacentes los
        // fusionamos para mantener cursor / undo limpio.
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
    }

    fun setTargetMinutes(min: Int) {
        _state.value = _state.value.copy(targetDurationSec = min.coerceAtLeast(0) * 60)
    }

    fun togglePin() {
        val s = _state.value
        _state.value = s.copy(pinned = !s.pinned)
        if (s.id != 0L) {
            viewModelScope.launch {
                val current = repository.get(s.id) ?: return@launch
                repository.setPinned(current, !s.pinned)
            }
        }
    }

    /**
     * Guarda y cierra. Si la nota quedó vacía, la elimina (caso back
     * sobre una nota nueva sin contenido).
     */
    suspend fun save(): Long = persistMutex.withLock {
        val s = _state.value
        if (s.isEmpty) {
            if (s.id != 0L) {
                val current = repository.get(s.id)
                if (current != null) {
                    NoteBlockSerializer.mediaFiles(s.blocks).forEach {
                        mediaStorage.deleteIfExists(it)
                    }
                    repository.delete(current)
                }
            }
            return@withLock 0L
        }
        _state.value = s.copy(isSaving = true, saveStatus = SaveStatus.Saving)
        val body = NoteBlockSerializer.encode(s.blocks)
        val existing = if (s.id != 0L) repository.get(s.id) else null
        val d = Discurso(
            id = s.id,
            title = s.title.trim(),
            scriptures = "",
            tags = "",
            pointsJson = "[]",
            notes = body,
            targetDurationSec = s.targetDurationSec,
            pinned = s.pinned,
            deletedAt = null,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
        )
        val newId = repository.upsert(d)
        _state.value = s.copy(id = newId, isSaving = false, saveStatus = SaveStatus.Saved)
        return@withLock newId
    }

    /**
     * "Eliminar" desde el editor manda la nota a la **papelera** (soft
     * delete) — no borra archivos de media, así "Restaurar" la deja
     * intacta. La purga real de los media ocurre cuando la papelera se
     * elimina permanentemente o cuando expira a los 30 días.
     */
    suspend fun deleteCurrent(): Boolean {
        val s = _state.value
        if (s.id == 0L) return false
        val current = repository.get(s.id) ?: return false
        repository.trash(current)
        return true
    }
}
