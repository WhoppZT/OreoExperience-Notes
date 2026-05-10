package com.oreoexperience.notes.data

import java.util.UUID

/**
 * Bloque atómico de una nota. La nota deja de ser un único string de
 * markdown y pasa a ser una **lista ordenada** de bloques. El usuario
 * puede insertar imágenes / videos en cualquier posición y el editor
 * los renderiza inline.
 *
 * Persistencia: serializamos la lista a un único string que sigue
 * cabiendo en `Discurso.notes` (no requiere migración SQL). El formato
 * usa marcadores HTML-comment de una línea que **nunca** aparecen en
 * texto normal:
 *
 *   ```
 *   <!--media:image:abc.jpg-->
 *   <!--media:video:xyz.mp4-->
 *   ```
 *
 * Todo lo que esté entre dos marcadores (o en los extremos) se trata
 * como un bloque de texto markdown. Esto preserva la compatibilidad
 * hacia atrás: notas viejas (que son sólo texto) se cargan como un
 * único bloque [NoteBlock.Text].
 */
sealed class NoteBlock {
    /** Identificador estable durante la edición (no se persiste). */
    abstract val id: String

    data class Text(
        override val id: String = UUID.randomUUID().toString(),
        val markdown: String,
    ) : NoteBlock()

    data class Image(
        override val id: String = UUID.randomUUID().toString(),
        /** Nombre relativo dentro de `filesDir/media/`. */
        val fileName: String,
    ) : NoteBlock()

    data class Video(
        override val id: String = UUID.randomUUID().toString(),
        /** Nombre relativo dentro de `filesDir/media/`. */
        val fileName: String,
    ) : NoteBlock()
}

object NoteBlockSerializer {

    private val MEDIA_LINE = Regex("^<!--media:(image|video):(.+?)-->\\s*$")

    /**
     * Convierte un cuerpo serializado a la lista de bloques. Si el
     * cuerpo no contiene marcadores devuelve un único bloque de texto
     * con todo el contenido (compatibilidad con notas previas).
     */
    fun decode(raw: String): List<NoteBlock> {
        if (raw.isEmpty()) return listOf(NoteBlock.Text(markdown = ""))
        val lines = raw.lines()
        val blocks = mutableListOf<NoteBlock>()
        val textBuf = StringBuilder()

        fun flushText() {
            // Quitamos un único '\n' final del buffer si existe (lo
            // agregamos como separador entre líneas).
            val text = textBuf.toString().trimEnd('\n')
            if (text.isNotEmpty() || blocks.isEmpty()) {
                blocks.add(NoteBlock.Text(markdown = text))
            }
            textBuf.clear()
        }

        for (line in lines) {
            val match = MEDIA_LINE.matchEntire(line)
            if (match != null) {
                flushText()
                val type = match.groupValues[1]
                val name = match.groupValues[2]
                blocks.add(
                    if (type == "video") NoteBlock.Video(fileName = name)
                    else NoteBlock.Image(fileName = name),
                )
            } else {
                textBuf.append(line)
                textBuf.append('\n')
            }
        }
        flushText()
        // Garantizamos al menos un bloque de texto al final para que el
        // usuario pueda seguir escribiendo después de un media.
        if (blocks.isEmpty() || blocks.last() !is NoteBlock.Text) {
            blocks.add(NoteBlock.Text(markdown = ""))
        }
        return blocks
    }

    /**
     * Serializa la lista al formato persistible. Bloques de texto se
     * concatenan tal cual, los media se emiten como una línea con el
     * marcador HTML-comment.
     */
    fun encode(blocks: List<NoteBlock>): String {
        val sb = StringBuilder()
        for ((idx, b) in blocks.withIndex()) {
            when (b) {
                is NoteBlock.Text -> sb.append(b.markdown)
                is NoteBlock.Image -> sb.append("<!--media:image:${b.fileName}-->")
                is NoteBlock.Video -> sb.append("<!--media:video:${b.fileName}-->")
            }
            if (idx < blocks.lastIndex) sb.append('\n')
        }
        return sb.toString()
    }

    /** Lista de filenames media referenciados en la nota. */
    fun mediaFiles(blocks: List<NoteBlock>): List<String> =
        blocks.mapNotNull {
            when (it) {
                is NoteBlock.Image -> it.fileName
                is NoteBlock.Video -> it.fileName
                else -> null
            }
        }
}
