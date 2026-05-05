package com.oreoexperience.notes.util

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.oreoexperience.notes.data.Discurso
import com.oreoexperience.notes.data.Punto
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.util.Date

/**
 * Exportador PDF muy simple basado en [PdfDocument]. Sin dependencias externas;
 * suficiente para entregar una versión imprimible del bosquejo. El layout es
 * single-column con paginación automática y wrap por palabra.
 */
object PdfExporter {

    private const val PAGE_W = 595   // A4 @72dpi (puntos)
    private const val PAGE_H = 842
    private const val MARGIN = 48
    private const val LINE_GAP = 4

    suspend fun export(context: Context, d: Discurso, points: List<Punto>): Uri {
        val doc = PdfDocument()
        val title = Paint().apply {
            color = 0xFF1F0E5A.toInt()
            textSize = 22f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val sub = Paint().apply {
            color = 0xFF555555.toInt()
            textSize = 11f
            isAntiAlias = true
        }
        val h2 = Paint().apply {
            color = 0xFF1A0B33.toInt()
            textSize = 16f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val body = Paint().apply {
            color = 0xFF222222.toInt()
            textSize = 12f
            isAntiAlias = true
        }
        val mono = Paint().apply {
            color = 0xFF222222.toInt()
            textSize = 12f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }

        val maxWidth = (PAGE_W - 2 * MARGIN).toFloat()
        var pageNo = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
        var canvas = page.canvas
        var y = MARGIN.toFloat()

        fun newPage() {
            doc.finishPage(page)
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            canvas = page.canvas
            y = MARGIN.toFloat()
        }

        fun ensure(space: Float) {
            if (y + space > PAGE_H - MARGIN) newPage()
        }

        fun drawWrapped(text: String, paint: Paint, indent: Float = 0f) {
            val words = text.split(' ', '\n')
            val lineH = paint.textSize + LINE_GAP
            val sb = StringBuilder()
            for (w in words) {
                val candidate = if (sb.isEmpty()) w else sb.toString() + " " + w
                if (paint.measureText(candidate) > (maxWidth - indent)) {
                    ensure(lineH)
                    canvas.drawText(sb.toString(), MARGIN + indent, y + paint.textSize, paint)
                    y += lineH
                    sb.setLength(0)
                    sb.append(w)
                } else {
                    if (sb.isNotEmpty()) sb.append(' ')
                    sb.append(w)
                }
            }
            if (sb.isNotEmpty()) {
                ensure(lineH)
                canvas.drawText(sb.toString(), MARGIN + indent, y + paint.textSize, paint)
                y += lineH
            }
        }

        // Header
        drawWrapped(d.title.ifBlank { "(sin título)" }, title)
        if (d.scriptures.isNotBlank()) drawWrapped(d.scriptures, sub)
        if (d.tags.isNotBlank()) drawWrapped("Etiquetas: ${d.tags}", sub)
        drawWrapped("Actualizado: ${DateFormat.getDateTimeInstance().format(Date(d.updatedAt))}", sub)
        y += 8f

        if (points.isNotEmpty()) {
            ensure(20f); drawWrapped("Puntos del discurso", h2); y += 6f
            points.forEach { p ->
                if (p.text.isNotBlank()) {
                    drawWrapped(p.text, h2)
                }
                if (p.body.isNotBlank()) {
                    drawWrapped(p.body, body)
                }
                y += 6f
            }
        }

        if (d.notes.isNotBlank()) {
            // Compatibilidad con discursos viejos.
            ensure(20f); drawWrapped("Notas", h2); y += 4f
            drawWrapped(d.notes, mono)
        }

        doc.finishPage(page)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safe = d.title.ifBlank { "discurso" }
            .replace(Regex("[^A-Za-z0-9._-]+"), "_").take(48)
        val file = File(dir, "$safe.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()

        return FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
    }
}
