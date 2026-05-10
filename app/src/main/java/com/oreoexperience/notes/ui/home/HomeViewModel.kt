package com.oreoexperience.notes.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oreoexperience.notes.data.Discurso
import com.oreoexperience.notes.data.DiscursoRepository
import com.oreoexperience.notes.data.MediaStorage
import com.oreoexperience.notes.data.NoteBlockSerializer
import com.oreoexperience.notes.data.SortBy
import com.oreoexperience.notes.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow

data class HomeUiState(
    val items: List<Discurso> = emptyList(),
    val query: String = "",
    val sortBy: SortBy = SortBy.UPDATED,
)

class HomeViewModel(
    private val repository: DiscursoRepository,
    private val mediaStorage: MediaStorage,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val sortByFlow = snapshotFlow { userPreferences.sortBy }

    val state: StateFlow<HomeUiState> = combine(
        repository.observeAll(),
        query,
        sortByFlow,
    ) { items, q, sortBy ->
        val filtered = if (q.isBlank()) items
        else {
            val needle = q.trim().lowercase()
            items.filter { d -> d.matchesQuery(needle) }
        }
        // Re-ordenar respetando pinned siempre arriba.
        val sorted = filtered.sortedWith(comparator(sortBy))
        HomeUiState(items = sorted, query = q, sortBy = sortBy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    val queryState: StateFlow<String> = query.asStateFlow()

    fun setQuery(q: String) {
        query.value = q
    }

    fun setSortBy(sortBy: SortBy) {
        userPreferences.sortBy = sortBy
    }

    /** Mueve la nota a la papelera (soft delete). */
    fun trashNote(d: Discurso) {
        viewModelScope.launch { repository.trash(d) }
    }

    /** Pin / unpin de la nota. */
    fun togglePin(d: Discurso) {
        viewModelScope.launch { repository.setPinned(d, !d.pinned) }
    }

    /**
     * Hard delete de la nota — borra archivos de media asociados y
     * elimina la fila. Reservado para "Vaciar papelera" o decisiones
     * explícitas del usuario.
     */
    fun deleteNoteHard(d: Discurso) {
        viewModelScope.launch {
            val blocks = NoteBlockSerializer.decode(d.notes)
            NoteBlockSerializer.mediaFiles(blocks).forEach { name ->
                mediaStorage.deleteIfExists(name)
            }
            repository.delete(d)
        }
    }

    private fun Discurso.matchesQuery(needle: String): Boolean =
        title.lowercase().contains(needle) ||
            scriptures.lowercase().contains(needle) ||
            tags.lowercase().contains(needle) ||
            notes.lowercase().contains(needle)

    private fun comparator(sortBy: SortBy): Comparator<Discurso> {
        // Pinned siempre arriba; dentro de cada grupo, el criterio elegido.
        val tail: Comparator<Discurso> = when (sortBy) {
            SortBy.UPDATED -> compareByDescending { it.updatedAt }
            SortBy.CREATED -> compareByDescending { it.createdAt }
            SortBy.TITLE   -> compareBy(String.CASE_INSENSITIVE_ORDER) {
                it.title.ifBlank { "Untitled" }
            }
        }
        return compareByDescending<Discurso> { it.pinned }.then(tail)
    }
}
