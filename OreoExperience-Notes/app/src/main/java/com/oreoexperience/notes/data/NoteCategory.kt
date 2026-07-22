package com.oreoexperience.notes.data

/**
 * Categoría de una nota / discurso. Permite agrupar el contenido en
 * la lista principal y aplicar etiquetas visuales propias.
 */
enum class NoteCategory(val key: String, val label: String, val short: String) {
    DISCURSO("discurso", "Discursos", "Discurso"),
    CONSIDERACION("consideracion", "Consideraciones", "Consideración"),
    SERVICIO_CAMPO("servicio_campo", "Servicio del Campo", "Servicio"),
    GENERAL("general", "General", "Nota");

    companion object {
        fun fromKey(k: String?): NoteCategory =
            values().firstOrNull { it.key == k } ?: DISCURSO
    }
}
