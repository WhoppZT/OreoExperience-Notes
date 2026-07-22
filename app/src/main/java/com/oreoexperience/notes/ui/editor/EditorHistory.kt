package com.oreoexperience.notes.ui.editor

import com.oreoexperience.notes.data.ChecklistItem
import com.oreoexperience.notes.data.NoteBlock

/**
 * Sistema de historial basado en **acciones tipadas** en lugar de
 * snapshots completos del estado.
 *
 * Cada acción describe cómo aplicarla (`apply`) y cómo revertirla
 * (`unapply`) sobre un [EditorUiState]. Así se evita duplicar el
 * estado entero por cada paso de undo y se gana:
 *
 *   - Memoria O(deltas) en lugar de O(estado × pasos).
 *   - **Coalescencia** de tipeo: dos ediciones de texto consecutivas
 *     sobre el mismo bloque dentro de [COALESCE_WINDOW_MS] se fusionan
 *     en una sola entrada. Acciones discretas (toggle pin, insertar
 *     bloque, marcar checkbox, etc.) **rompen** la cadena, por lo que
 *     cada paso de undo se siente atómico.
 *   - Etiquetas legibles (`label`) para mostrar feedback al usuario:
 *     "Se deshizo: insertar imagen", "Se rehizo: cambiar título"…
 *
 * El cambio NO es ABI-breaking: el resto del ViewModel sigue
 * exponiendo `undo()`, `redo()`, `canUndo`, `canRedo` con la misma
 * semántica.
 */
internal sealed interface EditAction {
    /** Etiqueta legible mostrada en el snackbar al deshacer/rehacer. */
    val label: String

    /** Aplica el cambio "hacia adelante" (estado posterior al usuario). */
    fun apply(state: EditorUiState): EditorUiState

    /** Revierte el cambio (estado anterior al usuario). */
    fun unapply(state: EditorUiState): EditorUiState
}

// =============================================================================
// Implementaciones concretas
// =============================================================================

internal data class TitleEdit(
    val before: String,
    val after: String,
    val timestamp: Long = System.currentTimeMillis(),
) : EditAction {
    override val label = "cambiar título"
    override fun apply(state: EditorUiState) = state.copy(title = after)
    override fun unapply(state: EditorUiState) = state.copy(title = before)
}

internal data class TextBlockEdit(
    val blockId: String,
    val before: String,
    val after: String,
    val timestamp: Long = System.currentTimeMillis(),
) : EditAction {
    override val label = "editar texto"
    override fun apply(state: EditorUiState): EditorUiState =
        state.copy(blocks = state.blocks.map {
            if (it is NoteBlock.Text && it.id == blockId) it.copy(markdown = after) else it
        })

    override fun unapply(state: EditorUiState): EditorUiState =
        state.copy(blocks = state.blocks.map {
            if (it is NoteBlock.Text && it.id == blockId) it.copy(markdown = before) else it
        })
}

internal data class InsertBlocks(
    val index: Int,
    val inserted: List<NoteBlock>,
    /** Para `insertMediaAfter`: si dividimos un bloque de texto, guardamos el
     *  estado previo del bloque dividido para poder restaurarlo en undo. */
    val splitTextBlockBefore: NoteBlock.Text? = null,
    val splitTextBlockAfter: NoteBlock.Text? = null,
) : EditAction {
    override val label: String
        get() {
            val key = inserted.firstOrNull { it !is NoteBlock.Text }
            return when (key) {
                is NoteBlock.Image -> "insertar imagen"
                is NoteBlock.Video -> "insertar video"
                is NoteBlock.Checklist -> "insertar checklist"
                is NoteBlock.Text, null -> "insertar bloque"
            }
        }

    override fun apply(state: EditorUiState): EditorUiState {
        val list = state.blocks.toMutableList()
        // Si hay split, reemplazamos el bloque en `index - 1` por su versión "before".
        if (splitTextBlockBefore != null) {
            val splitIdx = index - 1
            if (splitIdx in list.indices) {
                list[splitIdx] = splitTextBlockBefore
            }
        }
        list.addAll(index, inserted)
        return state.copy(blocks = list)
    }

    override fun unapply(state: EditorUiState): EditorUiState {
        val list = state.blocks.toMutableList()
        // Quitamos los `inserted.size` bloques en `index`.
        repeat(inserted.size) {
            if (index in list.indices) list.removeAt(index)
        }
        // Restauramos el bloque de texto previo al split (si lo hubo).
        if (splitTextBlockAfter != null) {
            val splitIdx = index - 1
            if (splitIdx in list.indices) {
                list[splitIdx] = splitTextBlockAfter
            }
        }
        return state.copy(blocks = list)
    }
}

internal data class RemoveBlock(
    val index: Int,
    val removed: NoteBlock,
    /** Para reflejar el merge de bloques de texto vecinos al eliminar. */
    val mergedTextSnapshot: List<NoteBlock>? = null,
    val originalSnapshot: List<NoteBlock>? = null,
) : EditAction {
    override val label = when (removed) {
        is NoteBlock.Image -> "eliminar imagen"
        is NoteBlock.Video -> "eliminar video"
        is NoteBlock.Checklist -> "eliminar checklist"
        is NoteBlock.Text -> "eliminar texto"
    }

    override fun apply(state: EditorUiState): EditorUiState {
        // Si guardamos el snapshot post-merge, lo aplicamos directo —
        // así replicamos exactamente el resultado del merge legacy.
        return if (mergedTextSnapshot != null) {
            state.copy(blocks = mergedTextSnapshot)
        } else {
            state.copy(blocks = state.blocks.toMutableList().apply {
                if (index in indices) removeAt(index)
            })
        }
    }

    override fun unapply(state: EditorUiState): EditorUiState {
        return if (originalSnapshot != null) {
            state.copy(blocks = originalSnapshot)
        } else {
            state.copy(blocks = state.blocks.toMutableList().apply { add(index, removed) })
        }
    }
}

internal data class TogglePinAction(val before: Boolean) : EditAction {
    override val label = if (before) "quitar fijado" else "fijar"
    override fun apply(state: EditorUiState) = state.copy(pinned = !before)
    override fun unapply(state: EditorUiState) = state.copy(pinned = before)
}

internal data class TargetDurationEdit(val before: Int, val after: Int) : EditAction {
    override val label = "cambiar cronómetro"
    override fun apply(state: EditorUiState) = state.copy(targetDurationSec = after)
    override fun unapply(state: EditorUiState) = state.copy(targetDurationSec = before)
}

internal data class ChecklistItemTextEdit(
    val blockId: String,
    val itemId: String,
    val before: String,
    val after: String,
    val timestamp: Long = System.currentTimeMillis(),
) : EditAction {
    override val label = "editar ítem"
    override fun apply(state: EditorUiState) = patch(state, after)
    override fun unapply(state: EditorUiState) = patch(state, before)
    private fun patch(state: EditorUiState, value: String): EditorUiState =
        state.copy(blocks = state.blocks.map {
            if (it is NoteBlock.Checklist && it.id == blockId) {
                it.copy(items = it.items.map { i -> if (i.id == itemId) i.copy(text = value) else i })
            } else it
        })
}

internal data class ChecklistItemToggleAction(
    val blockId: String,
    val itemId: String,
) : EditAction {
    override val label = "marcar ítem"
    override fun apply(state: EditorUiState) = flip(state)
    override fun unapply(state: EditorUiState) = flip(state)
    private fun flip(state: EditorUiState): EditorUiState =
        state.copy(blocks = state.blocks.map {
            if (it is NoteBlock.Checklist && it.id == blockId) {
                it.copy(items = it.items.map { i -> if (i.id == itemId) i.copy(checked = !i.checked) else i })
            } else it
        })
}

internal data class ChecklistItemAdd(
    val blockId: String,
    val item: ChecklistItem,
) : EditAction {
    override val label = "agregar ítem"
    override fun apply(state: EditorUiState) =
        state.copy(blocks = state.blocks.map {
            if (it is NoteBlock.Checklist && it.id == blockId) it.copy(items = it.items + item) else it
        })

    override fun unapply(state: EditorUiState) =
        state.copy(blocks = state.blocks.map {
            if (it is NoteBlock.Checklist && it.id == blockId) it.copy(items = it.items.filter { i -> i.id != item.id }) else it
        })
}

internal data class ChecklistItemRemove(
    val blockId: String,
    val item: ChecklistItem,
    val originalIndex: Int,
) : EditAction {
    override val label = "eliminar ítem"
    override fun apply(state: EditorUiState) =
        state.copy(blocks = state.blocks.map {
            if (it is NoteBlock.Checklist && it.id == blockId) it.copy(items = it.items.filter { i -> i.id != item.id }) else it
        })

    override fun unapply(state: EditorUiState) =
        state.copy(blocks = state.blocks.map {
            if (it is NoteBlock.Checklist && it.id == blockId) {
                val list = it.items.toMutableList()
                list.add(originalIndex.coerceIn(0, list.size), item)
                it.copy(items = list)
            } else it
        })
}

/**
 * Acción de undo para un reemplazo individual de Find & Replace.
 * Guardamos el HTML completo del bloque antes y después para evitar
 * tener que recalcular offsets al revertir.
 */
internal data class FindReplaceOne(
    val blockId: String,
    val beforeHtml: String,
    val afterHtml: String,
) : EditAction {
    override val label = "reemplazo"
    override fun apply(state: EditorUiState) =
        state.copy(blocks = state.blocks.map {
            if (it is NoteBlock.Text && it.id == blockId) it.copy(markdown = afterHtml) else it
        })
    override fun unapply(state: EditorUiState) =
        state.copy(blocks = state.blocks.map {
            if (it is NoteBlock.Text && it.id == blockId) it.copy(markdown = beforeHtml) else it
        })
}

/**
 * Acción de undo para "Reemplazar todo". Guardamos snapshots completos
 * de la lista de bloques antes y después. Es la forma más segura de
 * revertir sin perder formato ni romper el orden.
 */
internal data class FindReplaceAll(
    val snapshotBefore: List<NoteBlock>,
    val snapshotAfter: List<NoteBlock>,
) : EditAction {
    override val label = "reemplazar todo"
    override fun apply(state: EditorUiState) = state.copy(blocks = snapshotAfter)
    override fun unapply(state: EditorUiState) = state.copy(blocks = snapshotBefore)
}

// =============================================================================
// Manager de pilas
// =============================================================================

/**
 * Resultado de aplicar undo/redo: el nuevo estado y la etiqueta de la
 * acción para mostrarle al usuario en el snackbar.
 */
internal data class HistoryStep(
    val newState: EditorUiState,
    val label: String,
)

/**
 * Pila doble de acciones con coalescencia. No tiene UI; sólo lógica.
 *
 * `undoStack` guarda acciones que se aplicaron al estado actual y
 * pueden revertirse con `unapply`. `redoStack` guarda acciones
 * revertidas que pueden volver a aplicarse con `apply`.
 *
 * Reglas de coalescencia (sólo para [TextBlockEdit] y [ChecklistItemTextEdit]):
 *
 *  - Las dos acciones tienen que ser del **mismo tipo** y referirse al
 *    **mismo blockId/itemId**.
 *  - El `after` de la primera tiene que ser igual al `before` de la
 *    segunda (la nueva continúa donde quedó la anterior).
 *  - El delta de timestamp tiene que ser menor que [COALESCE_WINDOW_MS].
 *
 * Cuando se cumplen, se reemplaza la cima de la pila con una sola
 * acción que va de `prev.before` a `next.after`. El usuario hace una
 * sola undo y vuelve atrás toda la ráfaga.
 */
internal class EditorHistoryManager(
    private val maxSize: Int = 60,
) {
    private val undoStack: ArrayDeque<EditAction> = ArrayDeque()
    private val redoStack: ArrayDeque<EditAction> = ArrayDeque()

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    /**
     * Registra una acción ya aplicada al estado. Intenta coalescer con
     * la cima del stack; si no se puede, la apila.
     *
     * Cualquier acción nueva descarta el redo stack (rama alternativa).
     */
    fun push(action: EditAction) {
        // Limpiamos el redo: editar es desviarse de la rama anterior.
        if (redoStack.isNotEmpty()) redoStack.clear()

        val top = undoStack.lastOrNull()
        val coalesced = if (top != null) tryCoalesce(top, action) else null
        if (coalesced != null) {
            undoStack.removeLast()
            undoStack.addLast(coalesced)
        } else {
            undoStack.addLast(action)
            while (undoStack.size > maxSize) undoStack.removeFirst()
        }
    }

    /**
     * Pop de undoStack y retorna el nuevo estado tras revertir.
     * Mueve la acción al redoStack para permitir rehacer.
     */
    fun undo(currentState: EditorUiState): HistoryStep? {
        val action = undoStack.removeLastOrNull() ?: return null
        val newState = action.unapply(currentState)
        redoStack.addLast(action)
        while (redoStack.size > maxSize) redoStack.removeFirst()
        return HistoryStep(newState, action.label)
    }

    /**
     * Pop de redoStack y retorna el nuevo estado tras reaplicar.
     * Mueve la acción de vuelta al undoStack.
     */
    fun redo(currentState: EditorUiState): HistoryStep? {
        val action = redoStack.removeLastOrNull() ?: return null
        val newState = action.apply(currentState)
        undoStack.addLast(action)
        while (undoStack.size > maxSize) undoStack.removeFirst()
        return HistoryStep(newState, action.label)
    }

    /** Vacía las dos pilas (al cargar otra nota, por ejemplo). */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    private fun tryCoalesce(prev: EditAction, next: EditAction): EditAction? {
        return when {
            prev is TextBlockEdit && next is TextBlockEdit
                && prev.blockId == next.blockId
                && prev.after == next.before
                && next.timestamp - prev.timestamp <= COALESCE_WINDOW_MS ->
                TextBlockEdit(
                    blockId = prev.blockId,
                    before = prev.before,
                    after = next.after,
                    timestamp = next.timestamp,
                )

            prev is ChecklistItemTextEdit && next is ChecklistItemTextEdit
                && prev.blockId == next.blockId
                && prev.itemId == next.itemId
                && prev.after == next.before
                && next.timestamp - prev.timestamp <= COALESCE_WINDOW_MS ->
                ChecklistItemTextEdit(
                    blockId = prev.blockId,
                    itemId = prev.itemId,
                    before = prev.before,
                    after = next.after,
                    timestamp = next.timestamp,
                )

            prev is TitleEdit && next is TitleEdit
                && prev.after == next.before
                && next.timestamp - prev.timestamp <= COALESCE_WINDOW_MS ->
                TitleEdit(
                    before = prev.before,
                    after = next.after,
                    timestamp = next.timestamp,
                )

            else -> null
        }
    }

    companion object {
        private const val COALESCE_WINDOW_MS = 1500L
    }
}
