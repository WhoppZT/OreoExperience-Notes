package com.oreoexperience.notes.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oreoexperience.notes.data.Discurso
import com.oreoexperience.notes.data.DiscursoRepository
import com.oreoexperience.notes.data.MediaStorage
import com.oreoexperience.notes.data.NoteBlockSerializer
import com.oreoexperience.notes.data.NoteCategory
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

/**
 * Pestaña activa del Home. `null` = "Todos".
 *
 * Servicio del Campo se trata aparte (es su propia pantalla con
 * mini-calendario y registros estructurados), por eso aquí solo
 * exponemos las tabs que listan notas-libres. El `icon` se usa en
 * los chips de la fila de categorías para identificación instantánea.
 */
enum class HomeTab(
    val label: String,
    val category: NoteCategory?,
    val icon: ImageVector,
) {
    TODOS("Todos", null, Icons.Outlined.AllInclusive),
    DISCURSOS("Discursos", NoteCategory.DISCURSO, Icons.Outlined.RecordVoiceOver),
    CONSIDERACIONES("Consideraciones", NoteCategory.CONSIDERACION, Icons.Outlined.Lightbulb),
    GENERAL("General", NoteCategory.GENERAL, Icons.AutoMirrored.Outlined.StickyNote2),
}

data class HomeUiState(
    val items: List<Discurso> = emptyList(),
    val query: String = "",
    val sortBy: SortBy = SortBy.UPDATED,
    val activeTab: HomeTab = HomeTab.TODOS,
)

class HomeViewModel(
    private val repository: DiscursoRepository,
    private val mediaStorage: MediaStorage,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val sortByFlow = snapshotFlow { userPreferences.sortBy }
    private val activeTab = MutableStateFlow(HomeTab.TODOS)

    val state: StateFlow<HomeUiState> = combine(
        repository.observeAll(),
        query,
        sortByFlow,
        activeTab,
    ) { items, q, sortBy, tab ->
        val byTab = if (tab.category == null) items
        else items.filter { it.category == tab.category.key }
        val filtered = if (q.isBlank()) byTab
        else {
            val needle = q.trim().lowercase()
            byTab.filter { d -> d.matchesQuery(needle) }
        }
        // Re-ordenar respetando pinned siempre arriba.
        val sorted = filtered.sortedWith(comparator(sortBy))
        HomeUiState(items = sorted, query = q, sortBy = sortBy, activeTab = tab)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    val queryState: StateFlow<String> = query.asStateFlow()
    val activeTabState: StateFlow<HomeTab> = activeTab.asStateFlow()

    fun setQuery(q: String) {
        query.value = q
    }

    fun setSortBy(sortBy: SortBy) {
        userPreferences.sortBy = sortBy
    }

    fun setActiveTab(tab: HomeTab) {
        activeTab.value = tab
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
