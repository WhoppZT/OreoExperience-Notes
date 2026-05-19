package com.oreoexperience.notes.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oreoexperience.notes.data.Discurso
import com.oreoexperience.notes.data.DiscursoRepository
import com.oreoexperience.notes.data.MediaStorage
import com.oreoexperience.notes.data.NoteBlock
import com.oreoexperience.notes.data.NoteBlockSerializer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
data class EditorUiState(
    val id: Long = 0L,
    val title: String = "",
    val blocks: List<NoteBlock> = listOf(NoteBlock.Text(markdown = "")),
    val targetDurationSec: Int = 0,
    val pinned: Boolean = false,
    val loaded: Boolean = false,
    val isSaving: Boolean = false,
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

    fun setTitle(v: String) { _state.value = _state.value.copy(title = v) }

    /** Reemplaza el markdown de un bloque de texto identificado por [id]. */
    fun updateTextBlock(id: String, markdown: String) {
        _state.value = _state.value.copy(
            blocks = _state.value.blocks.map { b ->
                if (b is NoteBlock.Text && b.id == id) b.copy(markdown = markdown) else b
            },
        )
    }

    /**
     * Inserta un bloque media después del bloque de texto identificado
     * por [afterTextBlockId]. Si el bloque tenía texto, lo dividimos
     * en dos — **siempre por límite de párrafo** (línea en blanco) más
     * cercano al cursor [splitOffset]. Si no hay un párrafo accesible
     * insertamos el media al final del bloque, preservando así el
     * markdown intacto (no cortamos en medio de `**negrita**`,
     * `[link](url)` o cualquier otro marcador).
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
        // Cortamos por límite de párrafo más próximo: nunca dentro de
        // un fragmento markdown formateado.
        val (before, after) = splitAtParagraph(md, splitOffset)
        blocks[idx] = target.copy(markdown = before)
        blocks.add(idx + 1, media)
        blocks.add(idx + 2, NoteBlock.Text(markdown = after))
        _state.value = _state.value.copy(blocks = blocks)
    }

    /**
     * Devuelve el par `(antes, después)` cortando el markdown por
     * límite de párrafo. Si `splitOffset` no está dentro del rango
     * o no hay un límite razonable, el corte se hace al final
     * (after = ""), lo que evita romper estructura.
     */
    private fun splitAtParagraph(md: String, splitOffset: Int): Pair<String, String> {
        if (md.isEmpty()) return "" to ""
        if (splitOffset !in 0..md.length) return md to ""
        // Buscamos el "\n\n" más cercano por arriba o por abajo del
        // cursor. Si lo encontramos cortamos exactamente ahí. Si no,
        // caemos al final del bloque.
        val before = md.lastIndexOf("\n\n", startIndex = (splitOffset - 1).coerceAtLeast(0))
        val after = md.indexOf("\n\n", startIndex = splitOffset)
        val cut = when {
            before >= 0 && after >= 0 ->
                if ((splitOffset - before) <= (after - splitOffset)) before + 2 else after + 2
            before >= 0 -> before + 2
            after >= 0 -> after + 2
            else -> md.length
        }
        return md.substring(0, cut) to md.substring(cut)
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
        // Si después de borrar quedan dos bloques de texto adyacentes
        // los fusionamos usando **doble salto de línea** para no
        // pegar dos párrafos como uno solo (markdown necesita la
        // línea en blanco para mantener la separación visual).
        var i = 0
        while (i < blocks.size - 1) {
            val a = blocks[i]
            val b = blocks[i + 1]
            if (a is NoteBlock.Text && b is NoteBlock.Text) {
                val merged = when {
                    a.markdown.isEmpty() -> b.markdown
                    b.markdown.isEmpty() -> a.markdown
                    a.markdown.endsWith("\n\n") || b.markdown.startsWith("\n\n") ->
                        a.markdown + b.markdown
                    a.markdown.endsWith("\n") || b.markdown.startsWith("\n") ->
                        a.markdown + "\n" + b.markdown
                    else -> a.markdown + "\n\n" + b.markdown
                }
                blocks[i] = a.copy(markdown = merged)
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
     * Guarda la nota. Si la nota está completamente vacía y es nueva,
     * no la persiste y devuelve 0L. Si era una nota existente y queda
     * vacía, la elimina.
     */
    suspend fun save(): Long {
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
            return 0L
        }
        _state.value = s.copy(isSaving = true)
        val body = NoteBlockSerializer.encode(s.blocks)
        // Conservar createdAt si la nota ya existía.
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
        val id = repository.upsert(d)
        _state.value = s.copy(id = id, isSaving = false)
        return id
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
