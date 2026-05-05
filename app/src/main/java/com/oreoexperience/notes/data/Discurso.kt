package com.oreoexperience.notes.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Un discurso es básicamente una nota estructurada que se sirve para
 * predicar / dar una charla. El contenido principal vive en JSON en
 * la columna [pointsJson] (lista de Punto con sub-puntos), más bloques
 * de texto libre en [notes] y metadatos en el resto de columnas.
 *
 * Mantenemos los puntos serializados como JSON en una sola columna para
 * no caer en complejidad de relaciones. Para el volumen típico (cientos
 * de discursos con una decena de puntos cada uno) esto es perfectamente
 * suficiente y mantiene el modelo plano.
 */
@Entity(tableName = "discursos")
@Serializable
data class Discurso(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val scriptures: String = "",
    val tags: String = "",            // separadas por coma
    val pointsJson: String = "[]",    // List<Punto> serializada
    val notes: String = "",           // markdown libre
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    /** Duración objetivo del discurso en segundos. 0 = sin objetivo. */
    val targetDurationSec: Int = 0,
)

/**
 * Un "punto" del discurso es ahora una **sección** con título + cuerpo.
 *
 * - [text] es el título de la sección (ej: "Introducción", "Punto 2",
 *   "Conclusión").
 * - [body] es el texto libre que va dentro de esa sección.
 *
 * Para retro-compatibilidad con discursos guardados en versiones previas
 * (que usaban una lista de [subpoints] en lugar de un body), aceptamos
 * los dos campos al deserializar: si llega un Punto sin [body] pero con
 * [subpoints], el ViewModel los une como un único string al cargar
 * (ver `migrateLegacy()`).
 */
@Serializable
data class Punto(
    val text: String = "",
    val body: String = "",
    /** @deprecated reemplazado por [body]. Sólo se conserva para poder
     *  leer JSON viejos sin romper nada. */
    val subpoints: List<String> = emptyList(),
) {
    /**
     * Si este punto viene de un JSON viejo (sin body, con subpoints),
     * devuelve una versión normalizada con el body construido a partir
     * de los subpoints. En cualquier otro caso devuelve `this`.
     */
    fun migrateLegacy(): Punto =
        if (body.isBlank() && subpoints.isNotEmpty()) {
            copy(body = subpoints.joinToString("\n"), subpoints = emptyList())
        } else this
}
