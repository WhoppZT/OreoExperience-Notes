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

data class EditorUiState(
    val id: Long = 0L,
    val title: String = "",
    val scriptures: String = "",
    val tags: String = "",
    val notes: String = "",
    val points: List<Punto> = emptyList(),
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
                _state.value = EditorUiState(
                    id = d.id,
                    title = d.title,
                    scriptures = d.scriptures,
                    tags = d.tags,
                    notes = d.notes,
                    points = repository.decodePoints(d.pointsJson),
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
    fun setNotes(v: String) { _state.value = _state.value.copy(notes = v) }

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

    fun addSubpoint(pointIndex: Int) {
        val list = _state.value.points.toMutableList()
        if (pointIndex in list.indices) {
            list[pointIndex] = list[pointIndex].copy(subpoints = list[pointIndex].subpoints + "")
        }
        _state.value = _state.value.copy(points = list)
    }

    fun setSubpoint(pointIndex: Int, subIndex: Int, text: String) {
        val list = _state.value.points.toMutableList()
        if (pointIndex in list.indices) {
            val subs = list[pointIndex].subpoints.toMutableList()
            if (subIndex in subs.indices) subs[subIndex] = text
            list[pointIndex] = list[pointIndex].copy(subpoints = subs)
        }
        _state.value = _state.value.copy(points = list)
    }

    fun removeSubpoint(pointIndex: Int, subIndex: Int) {
        val list = _state.value.points.toMutableList()
        if (pointIndex in list.indices) {
            val subs = list[pointIndex].subpoints.toMutableList()
            if (subIndex in subs.indices) subs.removeAt(subIndex)
            list[pointIndex] = list[pointIndex].copy(subpoints = subs)
        }
        _state.value = _state.value.copy(points = list)
    }

    suspend fun save(): Long {
        val s = _state.value
        _state.value = s.copy(isSaving = true)
        val d = Discurso(
            id = s.id,
            title = s.title.trim(),
            scriptures = s.scriptures.trim(),
            tags = s.tags.trim(),
            pointsJson = repository.encodePoints(s.points),
            notes = s.notes,
        )
        val id = repository.upsert(d)
        _state.value = s.copy(id = id, isSaving = false)
        return id
    }

    suspend fun deleteCurrent() {
        val s = _state.value
        if (s.id == 0L) return
        repository.get(s.id)?.let { repository.delete(it) }
    }
}
