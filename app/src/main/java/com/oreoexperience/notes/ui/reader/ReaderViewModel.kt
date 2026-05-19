package com.oreoexperience.notes.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oreoexperience.notes.data.Discurso
import com.oreoexperience.notes.data.DiscursoRepository
import com.oreoexperience.notes.data.NoteBlock
import com.oreoexperience.notes.data.NoteBlockSerializer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReaderUiState(
    val loaded: Boolean = false,
    val note: Discurso? = null,
    val blocks: List<NoteBlock> = emptyList(),
)

/**
 * ViewModel super-fino para el modo lectura: sólo carga la nota una
 * vez y expone sus bloques deserializados. No expone setters porque
 * la pantalla es de solo-lectura.
 */
class ReaderViewModel(
    private val repository: DiscursoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch {
            val note = repository.get(id) ?: return@launch
            val blocks = NoteBlockSerializer.decode(note.notes)
                .ifEmpty { listOf(NoteBlock.Text(markdown = "")) }
            _state.value = ReaderUiState(loaded = true, note = note, blocks = blocks)
        }
    }
}
