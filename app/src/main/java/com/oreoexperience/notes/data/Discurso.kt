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
)

@Serializable
data class Punto(
    val text: String = "",
    val subpoints: List<String> = emptyList(),
)
