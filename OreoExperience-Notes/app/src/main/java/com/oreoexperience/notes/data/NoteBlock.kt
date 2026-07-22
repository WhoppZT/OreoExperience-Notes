package com.oreoexperience.notes.data

import android.util.Base64
import java.util.UUID

/**
 * Bloque atómico de una nota. La nota deja de ser un único string de
 * markdown y pasa a ser una **lista ordenada** de bloques. El usuario
 * puede insertar imágenes / videos / checklists en cualquier posición
 * y el editor los renderiza inline.
 *
 * Persistencia: serializamos la lista a un único string que sigue
 * cabiendo en `Discurso.notes` (no requiere migración SQL). El formato
 * usa marcadores HTML-comment de una línea que **nunca** aparecen en
 * texto normal:
 *
 *   ```
 *   <!--media:image:abc.jpg-->
 *   <!--media:video:xyz.mp4-->
 *   <!--checklist:start-->
 *   <!--checklist:item:c:BASE64-->
 *   <!--checklist:item:u:BASE64-->
 *   <!--checklist:end-->
 *   ```
 *
 * Todo lo que esté entre dos marcadores (o en los extremos) se trata
 * como un bloque de texto markdown. Esto preserva la compatibilidad
 * hacia atrás: notas viejas (que son sólo texto) se cargan como un
 * único bloque [NoteBlock.Text]. Los items de checklist se guardan
 * en base64 para no chocar con el cierre `-->` del marcador si el
 * usuario teclea ese literal dentro del item.
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

    data class Checklist(
        override val id: String = UUID.randomUUID().toString(),
        val items: List<ChecklistItem>,
    ) : NoteBlock()
}

/**
 * Ítem de un [NoteBlock.Checklist]. El [id] es estable durante la
 * edición pero no se persiste; al recargar se generan ids nuevos.
 */
data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val checked: Boolean,
)

object NoteBlockSerializer {

    private val MEDIA_LINE = Regex("^<!--media:(image|video):(.+?)-->\\s*$")
    private val CHECKLIST_START = Regex("^<!--checklist:start-->\\s*$")
    private val CHECKLIST_END = Regex("^<!--checklist:end-->\\s*$")
    private val CHECKLIST_ITEM = Regex("^<!--checklist:item:([cu]):(.*?)-->\\s*$")

    private fun encodeText(text: String): String =
        Base64.encodeToString(text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    private fun decodeText(b64: String): String = runCatching {
        String(Base64.decode(b64, Base64.NO_WRAP), Charsets.UTF_8)
    }.getOrDefault("")

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
        var checklistItems: MutableList<ChecklistItem>? = null

        fun flushText() {
            // Quitamos un único '\n' final del buffer si existe (lo
            // agregamos como separador entre líneas).
            val text = textBuf.toString().trimEnd('\n')
            if (text.isNotEmpty() || (blocks.isEmpty() && checklistItems == null)) {
                blocks.add(NoteBlock.Text(markdown = text))
            }
            textBuf.clear()
        }

        for (line in lines) {
            // Los marcadores de checklist tienen prioridad: si estamos
            // dentro de un bloque checklist, todas las líneas se
            // interpretan como item / end.
            if (checklistItems != null) {
                val itemMatch = CHECKLIST_ITEM.matchEntire(line)
                if (itemMatch != null) {
                    val checked = itemMatch.groupValues[1] == "c"
                    val text = decodeText(itemMatch.groupValues[2])
                    checklistItems!!.add(ChecklistItem(text = text, checked = checked))
                    continue
                }
                if (CHECKLIST_END.matches(line)) {
                    val items = checklistItems!!.toList()
                    if (items.isNotEmpty()) {
                        blocks.add(NoteBlock.Checklist(items = items))
                    }
                    checklistItems = null
                    continue
                }
                // Línea inesperada dentro del checklist: cerramos el
                // bloque y la procesamos como texto normal.
                val items = checklistItems!!.toList()
                if (items.isNotEmpty()) {
                    blocks.add(NoteBlock.Checklist(items = items))
                }
                checklistItems = null
                // sigue al procesamiento normal abajo
            }
            if (CHECKLIST_START.matches(line)) {
                flushText()
                checklistItems = mutableListOf()
                continue
            }
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
        // Cierre por EOF de un checklist sin marcador end (defensivo).
        if (checklistItems != null) {
            val items = checklistItems!!.toList()
            if (items.isNotEmpty()) {
                blocks.add(NoteBlock.Checklist(items = items))
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
     * marcador HTML-comment. Los checklists se emiten como un bloque
     * de varias líneas: start / item × n / end.
     */
    fun encode(blocks: List<NoteBlock>): String {
        val sb = StringBuilder()
        for ((idx, b) in blocks.withIndex()) {
            when (b) {
                is NoteBlock.Text -> sb.append(b.markdown)
                is NoteBlock.Image -> sb.append("<!--media:image:${b.fileName}-->")
                is NoteBlock.Video -> sb.append("<!--media:video:${b.fileName}-->")
                is NoteBlock.Checklist -> {
                    sb.append("<!--checklist:start-->")
                    for (item in b.items) {
                        sb.append('\n')
                        val flag = if (item.checked) "c" else "u"
                        sb.append("<!--checklist:item:")
                        sb.append(flag)
                        sb.append(':')
                        sb.append(encodeText(item.text))
                        sb.append("-->")
                    }
                    sb.append('\n')
                    sb.append("<!--checklist:end-->")
                }
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
