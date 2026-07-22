package com.oreoexperience.notes.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Registro diario de **servicio del campo** (predicación).
 *
 * Pensado para la organización de los Testigos de Jehová (jw.org):
 *
 * - [dateMillis] es el inicio del día (00:00 hora local del momento
 *   en que se guardó) — esto facilita agrupar por día sin tener que
 *   normalizar.
 * - [hours] son las horas predicadas ese día (Double, ej. 1.5).
 * - [revisits] número de revisitas hechas.
 * - [publications] publicaciones colocadas (revistas / folletos /
 *   libros).
 * - [videos] videos mostrados.
 * - [studies] cursos / estudios bíblicos dirigidos.
 * - [notes] espacio libre para anotaciones (lugar, anécdotas, etc).
 *
 * Sólo un registro por día — al insertar dos veces en el mismo día se
 * reemplaza el anterior. Eso evita confusión de "sumar dos horas en
 * el mismo día".
 */
@Entity(
    tableName = "registros_campo",
    indices = [Index(value = ["dateMillis"], unique = true)],
)
@Serializable
data class RegistroCampo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Inicio del día en ms (00:00 local). */
    val dateMillis: Long,
    val hours: Double = 0.0,
    val revisits: Int = 0,
    val publications: Int = 0,
    val videos: Int = 0,
    val studies: Int = 0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
