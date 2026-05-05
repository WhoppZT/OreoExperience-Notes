package com.oreoexperience.notes.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oreoexperience.notes.data.Discurso
import com.oreoexperience.notes.data.DiscursoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val items: List<Discurso> = emptyList(),
    val query: String = "",
)

class HomeViewModel(
    private val repository: DiscursoRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val state: StateFlow<HomeUiState> = combine(repository.observeAll(), query) { items, q ->
        val filtered = if (q.isBlank()) items
        else {
            val needle = q.trim().lowercase()
            items.filter { d ->
                d.title.lowercase().contains(needle) ||
                    d.scriptures.lowercase().contains(needle) ||
                    d.tags.lowercase().contains(needle) ||
                    d.notes.lowercase().contains(needle)
            }
        }
        HomeUiState(items = filtered, query = q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    val queryState: StateFlow<String> = query.asStateFlow()

    fun setQuery(q: String) {
        query.value = q
    }
}
