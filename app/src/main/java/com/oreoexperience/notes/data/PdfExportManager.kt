package com.oreoexperience.notes.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.TextPaint
import android.text.StaticLayout
import android.text.Layout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Generador simple de PDFs a partir de una lista de [Discurso].
 *
 * Diseño A4-ish (595 x 842 pt) con márgenes generosos, tipografía
 * sans-serif y separadores entre cada nota. Cada nota imprime:
 *
 *   1. Título grande + fecha de creación.
 *   2. Escrituras y tags (si existen).
 *   3. Cada "Punto" como bloque (título en bold + body en regular).
 *   4. Sección de "Notas" al final si la nota tiene contenido libre.
 *
 * Se evita embeber imágenes para mantener el archivo liviano (las
 * notas con imagen muestran un placeholder textual).
 */
class PdfExportManager(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))

    /**
     * Genera un PDF a partir de [discursos] y lo escribe en [outUri].
     * Devuelve la cantidad de páginas escritas. Pensado para correr
     * fuera del hilo principal — el bloque de IO se delega a
     * [Dispatchers.IO] internamente.
     */
    suspend fun export(
        discursos: List<Discurso>,
        title: String,
        outUri: Uri,
    ): Int = withContext(Dispatchers.IO) {
        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val marginX = 48f
        val marginTop = 56f
        val marginBottom = 56f
        val contentWidth = (pageWidth - marginX * 2).toInt()

        // Paints reutilizables
        val titlePaint = TextPaint().apply {
            color = Color.parseColor("#111111")
            textSize = 22f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = TextPaint().apply {
            color = Color.parseColor("#666666")
            textSize = 11f
            typeface = Typeface.SANS_SERIF
            isAntiAlias = true
        }
        val sectionPaint = TextPaint().apply {
            color = Color.parseColor("#7C3AED")
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val pointTitlePaint = TextPaint().apply {
            color = Color.parseColor("#222222")
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = TextPaint().apply {
            color = Color.parseColor("#333333")
            textSize = 12f
            typeface = Typeface.SANS_SERIF
            isAntiAlias = true
        }
        val rulePaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 1f
        }
        val headerPaint = TextPaint().apply {
            color = Color.parseColor("#999999")
            textSize = 9f
            typeface = Typeface.SANS_SERIF
            isAntiAlias = true
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page: PdfDocument.Page = pdf.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        var y = marginTop

        fun drawHeader() {
            canvas.drawText(
                "OreoExperience · $title",
                marginX,
                32f,
                headerPaint,
            )
        }
        drawHeader()

        fun ensureSpace(needed: Float) {
            if (y + needed > pageHeight - marginBottom) {
                pdf.finishPage(page)
                pageNumber += 1
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdf.startPage(pageInfo)
                canvas = page.canvas
                y = marginTop
                drawHeader()
            }
        }

        fun drawWrappedText(text: String, paint: TextPaint, extraSpacing: Float = 4f) {
            if (text.isBlank()) return
            val layout: StaticLayout = StaticLayout.Builder
                .obtain(text, 0, text.length, paint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.15f)
                .setIncludePad(false)
                .build()
            // si no entra en lo que queda, hacemos page break
            ensureSpace(layout.height.toFloat() + extraSpacing)
            canvas.save()
            canvas.translate(marginX, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + extraSpacing
        }

        // Portada simple — encabezado de la sección
        canvas.drawText(title, marginX, y + 28, titlePaint.apply { textSize = 28f })
        y += 36f
        canvas.drawText(
            "${discursos.size} nota(s) · Exportado ${dateFmt.format(Date())}",
            marginX,
            y + 8,
            subtitlePaint,
        )
        y += 18f
        canvas.drawLine(marginX, y, pageWidth - marginX, y, rulePaint)
        y += 16f

        // restauramos titlePaint a tamaño normal para los siguientes títulos
        titlePaint.textSize = 18f

        discursos.forEachIndexed { idx, d ->
            ensureSpace(64f)

            // Título
            drawWrappedText(d.title.ifBlank { "(Sin título)" }, titlePaint, extraSpacing = 2f)

            // Fecha + categoría
            val cat = NoteCategory.fromKey(d.category).label
            val fecha = dateFmt.format(Date(d.createdAt))
            drawWrappedText("$cat · $fecha", subtitlePaint, extraSpacing = 6f)

            // Escrituras
            if (d.scriptures.isNotBlank()) {
                drawWrappedText("Textos bíblicos", sectionPaint, extraSpacing = 2f)
                drawWrappedText(d.scriptures, bodyPaint, extraSpacing = 8f)
            }

            // Tags
            if (d.tags.isNotBlank()) {
                drawWrappedText("Etiquetas", sectionPaint, extraSpacing = 2f)
                drawWrappedText(d.tags, bodyPaint, extraSpacing = 8f)
            }

            // Puntos
            val puntos = runCatching {
                json.decodeFromString<List<Punto>>(d.pointsJson)
                    .map { it.migrateLegacy() }
            }.getOrDefault(emptyList())

            if (puntos.isNotEmpty()) {
                drawWrappedText("Desarrollo", sectionPaint, extraSpacing = 4f)
                puntos.forEach { p ->
                    if (p.text.isNotBlank()) {
                        drawWrappedText("• ${p.text}", pointTitlePaint, extraSpacing = 2f)
                    }
                    if (p.body.isNotBlank()) {
                        drawWrappedText(p.body, bodyPaint, extraSpacing = 6f)
                    }
                }
            }

            // Notas libres
            if (d.notes.isNotBlank()) {
                drawWrappedText("Notas", sectionPaint, extraSpacing = 2f)
                drawWrappedText(d.notes, bodyPaint, extraSpacing = 8f)
            }

            // Separador entre notas
            if (idx < discursos.size - 1) {
                ensureSpace(20f)
                canvas.drawLine(marginX, y + 4f, pageWidth - marginX, y + 4f, rulePaint)
                y += 20f
            }
        }

        pdf.finishPage(page)

        context.contentResolver.openOutputStream(outUri)?.use { out ->
            pdf.writeTo(out)
        } ?: error("No se pudo abrir el destino para escritura")

        pdf.close()
        pageNumber
    }

    /**
     * Exporta los [registros] de un mes específico ([monthStartMillis]
     * es el primer día del mes a las 00:00). El PDF rinde:
     *
     *   1. Cabecera con título "Servicio del Campo · {Mes Año}".
     *   2. Banner de totales en una línea.
     *   3. Mini calendario impreso (L M M J V S D) con los días que
     *      tienen registro pintados en Accent.
     *   4. Tabla "Detalle por día" con columnas Día, Horas, Rev,
     *      Cur, Pub, Vid, Notas. Filas zebra. Fila TOTAL al final
     *      en negrita.
     */
    suspend fun exportRegistrosMensual(
        registros: List<RegistroCampo>,
        monthStartMillis: Long,
        outUri: Uri,
    ): Int = withContext(Dispatchers.IO) {
        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val marginX = 40f
        val marginTop = 56f
        val marginBottom = 56f
        val contentWidth = (pageWidth - marginX * 2).toInt()

        // --- Paints ---
        val titlePaint = TextPaint().apply {
            color = Color.parseColor("#111111")
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = TextPaint().apply {
            color = Color.parseColor("#666666")
            textSize = 11f
            typeface = Typeface.SANS_SERIF
            isAntiAlias = true
        }
        val totalsBannerPaint = TextPaint().apply {
            color = Color.parseColor("#7C3AED")
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val sectionPaint = TextPaint().apply {
            color = Color.parseColor("#7C3AED")
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val cellPaint = TextPaint().apply {
            color = Color.parseColor("#222222")
            textSize = 11f
            typeface = Typeface.SANS_SERIF
            isAntiAlias = true
        }
        val cellHeaderPaint = TextPaint().apply {
            color = Color.parseColor("#555555")
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val cellTotalPaint = TextPaint().apply {
            color = Color.parseColor("#111111")
            textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val rulePaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 1f
        }
        val zebraPaint = Paint().apply {
            color = Color.parseColor("#F6F4FB")
            style = Paint.Style.FILL
        }
        val calendarOnPaint = Paint().apply {
            color = Color.parseColor("#7C3AED")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val calendarOnTextPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val calendarOffTextPaint = TextPaint().apply {
            color = Color.parseColor("#444444")
            textSize = 11f
            typeface = Typeface.SANS_SERIF
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val calendarHeaderPaint = TextPaint().apply {
            color = Color.parseColor("#999999")
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val headerPaint = TextPaint().apply {
            color = Color.parseColor("#999999")
            textSize = 9f
            typeface = Typeface.SANS_SERIF
            isAntiAlias = true
        }

        // --- Paginación ---
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page: PdfDocument.Page = pdf.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        var y = marginTop

        val monthLabelFmt = SimpleDateFormat("LLLL yyyy", Locale("es", "ES"))
        val monthLabel = monthLabelFmt.format(Date(monthStartMillis))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }

        fun drawHeader() {
            canvas.drawText(
                "OreoExperience · Servicio del Campo · $monthLabel",
                marginX,
                32f,
                headerPaint,
            )
        }
        drawHeader()

        fun ensureSpace(needed: Float) {
            if (y + needed > pageHeight - marginBottom) {
                pdf.finishPage(page)
                pageNumber += 1
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdf.startPage(pageInfo)
                canvas = page.canvas
                y = marginTop
                drawHeader()
            }
        }

        // --- 1. Título grande ---
        canvas.drawText(monthLabel, marginX, y + 28, titlePaint)
        y += 38f

        // --- 2. Banner totales ---
        val totalHours = registros.sumOf { it.hours }
        val totalRev = registros.sumOf { it.revisits }
        val totalCur = registros.sumOf { it.studies }
        val totalPub = registros.sumOf { it.publications }
        val totalVid = registros.sumOf { it.videos }
        val totalDays = registros.size

        val totalsLine = "%.1f hrs · %d rev · %d cur · %d pub · %d vid · %d días"
            .format(Locale("es", "ES"), totalHours, totalRev, totalCur, totalPub, totalVid, totalDays)
        canvas.drawText(totalsLine, marginX, y + 4, totalsBannerPaint)
        y += 16f
        canvas.drawLine(marginX, y, pageWidth - marginX, y, rulePaint)
        y += 18f

        // --- 3. Mini calendario ---
        canvas.drawText("Días con registro", marginX, y, sectionPaint)
        y += 14f

        val cal = Calendar.getInstance().apply {
            timeInMillis = monthStartMillis
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        // shift 0..6 donde 0=Lun, 6=Dom
        val rawDow = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun..7=Sat
        val leading = (rawDow + 5) % 7

        val daysWithRecord = registros.map { reg ->
            val c = Calendar.getInstance().apply { timeInMillis = reg.dateMillis }
            c.get(Calendar.DAY_OF_MONTH)
        }.toSet()

        val cellSize = 28f
        val cellGap = 4f
        val gridX0 = marginX
        // Encabezados días
        val dayHeaders = listOf("L", "M", "M", "J", "V", "S", "D")
        for (i in 0..6) {
            val cx = gridX0 + (cellSize + cellGap) * i + cellSize / 2f
            canvas.drawText(dayHeaders[i], cx, y, calendarHeaderPaint)
        }
        y += 8f

        var cellIdx = 0
        var dayCounter = 0
        while (dayCounter < daysInMonth) {
            // ensureSpace al inicio de cada fila
            ensureSpace(cellSize + cellGap + 4f)
            for (col in 0..6) {
                val cx = gridX0 + (cellSize + cellGap) * col
                val cy = y
                if (cellIdx >= leading && dayCounter < daysInMonth) {
                    val dayNumber = dayCounter + 1
                    val rect = RectF(cx, cy, cx + cellSize, cy + cellSize)
                    val hasRecord = dayNumber in daysWithRecord
                    if (hasRecord) {
                        canvas.drawRoundRect(rect, 6f, 6f, calendarOnPaint)
                        canvas.drawText(
                            dayNumber.toString(),
                            cx + cellSize / 2f,
                            cy + cellSize / 2f + 4f,
                            calendarOnTextPaint,
                        )
                    } else {
                        canvas.drawText(
                            dayNumber.toString(),
                            cx + cellSize / 2f,
                            cy + cellSize / 2f + 4f,
                            calendarOffTextPaint,
                        )
                    }
                    dayCounter += 1
                }
                cellIdx += 1
            }
            y += cellSize + cellGap
        }

        y += 12f
        canvas.drawLine(marginX, y, pageWidth - marginX, y, rulePaint)
        y += 14f

        // --- 4. Tabla detallada ---
        canvas.drawText("Detalle por día", marginX, y, sectionPaint)
        y += 14f

        // Columnas: Día(80) Horas(55) Rev(40) Cur(40) Pub(45) Vid(40) Notas(resto)
        val colDayW = 80f
        val colHrsW = 55f
        val colRevW = 40f
        val colCurW = 40f
        val colPubW = 45f
        val colVidW = 40f
        val colNotW = contentWidth - (colDayW + colHrsW + colRevW + colCurW + colPubW + colVidW)
        val xDay = marginX
        val xHrs = xDay + colDayW
        val xRev = xHrs + colHrsW
        val xCur = xRev + colRevW
        val xPub = xCur + colCurW
        val xVid = xPub + colPubW
        val xNot = xVid + colVidW

        val rowHeight = 22f

        // Cabecera de tabla
        canvas.drawText("Día", xDay, y, cellHeaderPaint)
        canvas.drawText("Horas", xHrs, y, cellHeaderPaint)
        canvas.drawText("Rev", xRev, y, cellHeaderPaint)
        canvas.drawText("Cur", xCur, y, cellHeaderPaint)
        canvas.drawText("Pub", xPub, y, cellHeaderPaint)
        canvas.drawText("Vid", xVid, y, cellHeaderPaint)
        canvas.drawText("Notas", xNot, y, cellHeaderPaint)
        y += 4f
        canvas.drawLine(marginX, y, pageWidth - marginX, y, rulePaint)
        y += 12f

        val rowDateFmt = SimpleDateFormat("EEE d", Locale("es", "ES"))
        val sorted = registros.sortedBy { it.dateMillis }

        sorted.forEachIndexed { idx, reg ->
            ensureSpace(rowHeight + 4f)
            if (idx % 2 == 0) {
                canvas.drawRect(marginX - 4f, y - 12f, pageWidth - marginX + 4f, y + 10f, zebraPaint)
            }
            val label = rowDateFmt.format(Date(reg.dateMillis))
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }
            canvas.drawText(label, xDay, y, cellPaint)
            canvas.drawText("%.1f".format(Locale("es", "ES"), reg.hours), xHrs, y, cellPaint)
            canvas.drawText(reg.revisits.toString(), xRev, y, cellPaint)
            canvas.drawText(reg.studies.toString(), xCur, y, cellPaint)
            canvas.drawText(reg.publications.toString(), xPub, y, cellPaint)
            canvas.drawText(reg.videos.toString(), xVid, y, cellPaint)
            // Notas (truncadas a una línea)
            if (reg.notes.isNotBlank()) {
                val maxChars = (colNotW / 5f).toInt().coerceAtLeast(10)
                val truncated = if (reg.notes.length > maxChars) reg.notes.take(maxChars - 1) + "…" else reg.notes
                canvas.drawText(truncated, xNot, y, cellPaint)
            }
            y += rowHeight
        }

        // Fila TOTAL
        ensureSpace(rowHeight + 8f)
        canvas.drawLine(marginX, y - 12f, pageWidth - marginX, y - 12f, rulePaint)
        y += 4f
        canvas.drawText("TOTAL", xDay, y, cellTotalPaint)
        canvas.drawText("%.1f".format(Locale("es", "ES"), totalHours), xHrs, y, cellTotalPaint)
        canvas.drawText(totalRev.toString(), xRev, y, cellTotalPaint)
        canvas.drawText(totalCur.toString(), xCur, y, cellTotalPaint)
        canvas.drawText(totalPub.toString(), xPub, y, cellTotalPaint)
        canvas.drawText(totalVid.toString(), xVid, y, cellTotalPaint)
        canvas.drawText("$totalDays días", xNot, y, cellTotalPaint)

        // Footer fecha de exportación
        ensureSpace(40f)
        y = pageHeight - marginBottom + 16f
        canvas.drawText(
            "Generado ${dateFmt.format(Date())}",
            marginX,
            y,
            subtitlePaint,
        )

        pdf.finishPage(page)

        context.contentResolver.openOutputStream(outUri)?.use { out ->
            pdf.writeTo(out)
        } ?: error("No se pudo abrir el destino para escritura")

        pdf.close()
        pageNumber
    }
}
