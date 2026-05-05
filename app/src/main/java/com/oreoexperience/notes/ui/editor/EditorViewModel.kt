package com.oreoexperience.notes.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oreoexperience.notes.data.Discurso
import com.oreoexperience.notes.data.DiscursoRepository
import com.oreoexperience.notes.data.Punto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado del editor.
 *
 * El concepto de "Notas" como bloque suelto desapareció: ahora el contenido
 * del discurso son **secciones** (Punto.text = título, Punto.body = cuerpo).
 * El campo legacy [legacyNotes] sólo persiste para mantener round-trip con
 * la columna Discurso.notes en discursos viejos. En cuanto el usuario edita
 * el discurso lo desplegamos como una sección "Notas" al final y dejamos el
 * campo vacío.
 */
data class EditorUiState(
    val id: Long = 0L,
    val title: String = "",
    val scriptures: String = "",
    val tags: String = "",
    val points: List<Punto> = emptyList(),
    /** Duración objetivo en minutos (string para que el usuario pueda
     *  borrar el campo libremente sin saltar a 0). */
    val targetMinutes: String = "",
    val loaded: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isNew: Boolean get() = id == 0L
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
                // Migración legacy:
                //   1) puntos viejos con `subpoints` → los unificamos como `body`.
                //   2) campo `notes` suelto → lo añadimos como última sección "Notas".
                val migratedPoints = repository.decodePoints(d.pointsJson)
                    .map { it.migrateLegacy() }
                    .toMutableList()
                if (d.notes.isNotBlank()) {
                    migratedPoints += Punto(text = "Notas", body = d.notes)
                }
                _state.value = EditorUiState(
                    id = d.id,
                    title = d.title,
                    scriptures = d.scriptures,
                    tags = d.tags,
                    points = migratedPoints,
                    targetMinutes = if (d.targetDurationSec > 0)
                        (d.targetDurationSec / 60).toString() else "",
                    loaded = true,
                )
            } else {
                _state.value = EditorUiState(loaded = true)
            }
        }
    }

    fun setTitle(v: String) { _state.value = _state.value.copy(title = v) }
    fun setScriptures(v: String) { _state.value = _state.value.copy(scriptures = v) }
    fun setTags(v: String) { _state.value = _state.value.copy(tags = v) }
    fun setTargetMinutes(v: String) {
        // Sólo aceptamos dígitos (hasta 4 — cubre 9999 min). Si quieren
        // borrar el campo, [v] queda vacío y se interpreta como 0.
        val cleaned = v.filter { it.isDigit() }.take(4)
        _state.value = _state.value.copy(targetMinutes = cleaned)
    }

    /** Agrega una sección nueva al final con título sugerido vacío. */
    fun addPoint() {
        _state.value = _state.value.copy(points = _state.value.points + Punto())
    }

    fun removePoint(index: Int) {
        val list = _state.value.points.toMutableList()
        if (index in list.indices) list.removeAt(index)
        _state.value = _state.value.copy(points = list)
    }

    fun setPointText(index: Int, text: String) {
        val list = _state.value.points.toMutableList()
        if (index in list.indices) list[index] = list[index].copy(text = text)
        _state.value = _state.value.copy(points = list)
    }

    fun setPointBody(index: Int, body: String) {
        val list = _state.value.points.toMutableList()
        if (index in list.indices) list[index] = list[index].copy(body = body)
        _state.value = _state.value.copy(points = list)
    }

    fun movePointUp(index: Int) {
        val list = _state.value.points.toMutableList()
        if (index in 1..list.lastIndex) {
            val item = list.removeAt(index)
            list.add(index - 1, item)
            _state.value = _state.value.copy(points = list)
        }
    }

    fun movePointDown(index: Int) {
        val list = _state.value.points.toMutableList()
        if (index in 0 until list.lastIndex) {
            val item = list.removeAt(index)
            list.add(index + 1, item)
            _state.value = _state.value.copy(points = list)
        }
    }

    suspend fun save(): Long {
        val s = _state.value
        _state.value = s.copy(isSaving = true)
        val targetSec = (s.targetMinutes.toIntOrNull() ?: 0) * 60
        // Sanitizamos los puntos: limpiamos legacy subpoints y descartamos
        // secciones completamente vacías (sin título y sin cuerpo).
        val cleaned = s.points
            .map { it.copy(subpoints = emptyList()) }
            .filter { it.text.isNotBlank() || it.body.isNotBlank() }
        val d = Discurso(
            id = s.id,
            title = s.title.trim(),
            scriptures = s.scriptures.trim(),
            tags = s.tags.trim(),
            pointsJson = repository.encodePoints(cleaned),
            // Notes ya no se usa: las notas viven dentro de cada Punto.
            // Dejamos el campo vacío para mantener compatibilidad con la
            // columna existente de Room.
            notes = "",
            targetDurationSec = targetSec,
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
