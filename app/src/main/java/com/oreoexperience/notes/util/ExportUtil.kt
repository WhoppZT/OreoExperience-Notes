package com.oreoexperience.notes.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.oreoexperience.notes.data.Discurso
import com.oreoexperience.notes.data.Punto
import java.io.File
import java.text.DateFormat
import java.util.Date

object ExportUtil {

    fun toPlain(d: Discurso, points: List<Punto>): String = buildString {
        appendLine(d.title.ifBlank { "(sin título)" })
        appendLine("=".repeat(d.title.length.coerceAtLeast(8)))
        if (d.scriptures.isNotBlank()) {
            appendLine()
            appendLine("Texto / escrituras: ${d.scriptures}")
        }
        if (d.tags.isNotBlank()) {
            appendLine("Etiquetas: ${d.tags}")
        }
        appendLine("Última actualización: ${DateFormat.getDateTimeInstance().format(Date(d.updatedAt))}")

        if (points.isNotEmpty()) {
            appendLine()
            appendLine("Bosquejo")
            appendLine("--------")
            points.forEachIndexed { idx, p ->
                appendLine("${idx + 1}. ${p.text}")
                p.subpoints.filter { it.isNotBlank() }.forEach { sub ->
                    appendLine("    - $sub")
                }
            }
        }

        if (d.notes.isNotBlank()) {
            appendLine()
            appendLine("Notas")
            appendLine("-----")
            appendLine(d.notes)
        }
    }

    fun toMarkdown(d: Discurso, points: List<Punto>): String = buildString {
        appendLine("# ${d.title.ifBlank { "(sin título)" }}")
        appendLine()
        if (d.scriptures.isNotBlank()) appendLine("**Texto base:** ${d.scriptures}")
        if (d.tags.isNotBlank()) appendLine("**Etiquetas:** ${d.tags}")
        appendLine("_Actualizado: ${DateFormat.getDateTimeInstance().format(Date(d.updatedAt))}_")
        if (points.isNotEmpty()) {
            appendLine()
            appendLine("## Bosquejo")
            points.forEachIndexed { idx, p ->
                appendLine("${idx + 1}. **${p.text}**")
                p.subpoints.filter { it.isNotBlank() }.forEach { sub ->
                    appendLine("    - $sub")
                }
            }
        }
        if (d.notes.isNotBlank()) {
            appendLine()
            appendLine("## Notas")
            appendLine()
            appendLine(d.notes)
        }
    }

    fun writeTextToCache(context: Context, fileName: String, content: String): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(content, Charsets.UTF_8)
        return FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
    }

    fun shareIntent(context: Context, uri: Uri, mime: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.let { Intent.createChooser(it, "Compartir") }
    }
}
