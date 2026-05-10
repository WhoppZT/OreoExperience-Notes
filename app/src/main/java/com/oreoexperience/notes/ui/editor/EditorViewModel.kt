package com.oreoexperience.notes.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oreoexperience.notes.data.Discurso
import com.oreoexperience.notes.data.DiscursoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado del editor — modelo simplificado estilo iOS Notes:
 *
 *   - [title]: título de la nota.
 *   - [body]: cuerpo de la nota como markdown.
 *   - [targetDurationSec]: opcional; si > 0, se muestra la barra de
 *     cronómetro inferior. No hay UI para editarlo en v0.7.0 — se conserva
 *     a partir de discursos guardados con versiones anteriores.
 *
 * Migración legacy automática al cargar:
 *   - Si la nota tiene secciones (`pointsJson != "[]"`) o un campo `notes`
 *     no vacío de versiones anteriores, se concatena todo en un único
 *     [body] con saltos de línea entre secciones.
 *   - Al guardar, se escribe sólo el [body] como `notes`, y se vacía
 *     `pointsJson` para limpiar la data legacy.
 */
data class EditorUiState(
    val id: Long = 0L,
    val title: String = "",
    val body: String = "",
    val targetDurationSec: Int = 0,
    val loaded: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isNew: Boolean get() = id == 0L
    val isEmpty: Boolean get() = title.isBlank() && body.isBlank()
}

class EditorViewModel(
    private val repository: DiscursoRepository,
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
                    body = migratedBody,
                    targetDurationSec = d.targetDurationSec,
                    loaded = true,
                )
            } else {
                _state.value = EditorUiState(loaded = true)
            }
        }
    }

    /**
     * Construye el cuerpo unificado a partir de un Discurso heredado:
     * concatena los puntos (cada uno con su título y body) seguidos del
     * antiguo bloque `notes`. Si el discurso no tiene secciones, devuelve
     * directamente `notes`.
     */
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

        val joined = listOf(sectionsText, d.notes)
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
        return joined
    }

    fun setTitle(v: String) { _state.value = _state.value.copy(title = v) }
    fun setBody(v: String) { _state.value = _state.value.copy(body = v) }

    /** Establece la duración objetivo en minutos (0 = sin objetivo). */
    fun setTargetMinutes(min: Int) {
        _state.value = _state.value.copy(targetDurationSec = min.coerceAtLeast(0) * 60)
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
                if (current != null) repository.delete(current)
            }
            return 0L
        }
        _state.value = s.copy(isSaving = true)
        val d = Discurso(
            id = s.id,
            title = s.title.trim(),
            scriptures = "",
            tags = "",
            pointsJson = "[]",
            notes = s.body,
            targetDurationSec = s.targetDurationSec,
        )
        val id = repository.upsert(d)
        _state.value = s.copy(id = id, isSaving = false)
        return id
    }

    suspend fun deleteCurrent(): Boolean {
        val s = _state.value
        if (s.id == 0L) return false
        val current = repository.get(s.id) ?: return false
        repository.delete(current)
        return true
    }
}
