package com.oreoexperience.notes.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.text.Layout
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Generador de PDFs a partir de una lista de [Discurso]. La versión
 * actual respeta el modelo basado en bloques: cada nota se renderiza
 * iterando sus [NoteBlock] (texto con estilos, imagen, video, checklist)
 * en lugar de aplastarlos a string plano.
 *
 * Diseño A4 (595 × 842 pt) con márgenes generosos. Cada nota imprime:
 *
 *   1. Título + fecha + categoría.
 *   2. Cuerpo iterado por bloques:
 *      - Texto: HTML del editor parseado a `Spanned` con estilos
 *        (negrita, cursiva, subrayado, tachado, color, highlight,
 *        monospace, headings).
 *      - Imagen: bitmap downsampleado cargado desde `MediaStorage`.
 *      - Video: thumbnail extraído con `MediaMetadataRetriever` +
 *        leyenda "Video".
 *      - Checklist: filas con cuadrito vacío / marcado.
 *
 * El header dinámico muestra el título de la nota actual y el número
 * de página. El footer una marca tenue con la fecha de exportación.
 */
class PdfExportManager(
    private val context: Context,
    private val mediaStorage: MediaStorage? = null,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))

    /**
     * Genera un PDF a partir de [discursos] y lo escribe en [outUri].
     * Devuelve la cantidad de páginas escritas.
     *
     * Hace **doble pasada**:
     *   1. Render en un PDF temporal para conocer el total de páginas.
     *   2. Render final con la numeración "X de Y" correcta en el header.
     *
     * Es más caro que una pasada simple pero es la única forma honesta
     * de tener numeración total sin estimaciones.
     */
    suspend fun export(
        discursos: List<Discurso>,
        title: String,
        outUri: Uri,
        settings: PdfExportSettings = PdfExportSettings(),
    ): Int = withContext(Dispatchers.IO) {
        // Pasada 1: contar páginas con un PDF descartable.
        val countingPdf = PdfDocument()
        val totalPages = renderInto(
            pdf = countingPdf,
            discursos = discursos,
            title = title,
            settings = settings,
            totalPagesHint = null,
        )
        countingPdf.close()

        // Pasada 2: render real con header "X de Y".
        val finalPdf = PdfDocument()
        renderInto(
            pdf = finalPdf,
            discursos = discursos,
            title = title,
            settings = settings,
            totalPagesHint = totalPages,
        )

        context.contentResolver.openOutputStream(outUri)?.use { out ->
            finalPdf.writeTo(out)
        } ?: error("No se pudo abrir el destino para escritura")

        finalPdf.close()
        totalPages
    }

    /**
     * Variante de [export] que escribe a un [File] local en lugar de
     * un Content URI. La usamos para "Compartir": guardamos en
     * `cache/exports/` y exponemos via FileProvider.
     */
    suspend fun exportToFile(
        discursos: List<Discurso>,
        title: String,
        outFile: File,
        settings: PdfExportSettings = PdfExportSettings(),
    ): Int = withContext(Dispatchers.IO) {
        // Pasada 1: contar páginas con un PDF descartable.
        val countingPdf = PdfDocument()
        val totalPages = renderInto(
            pdf = countingPdf,
            discursos = discursos,
            title = title,
            settings = settings,
            totalPagesHint = null,
        )
        countingPdf.close()

        val finalPdf = PdfDocument()
        renderInto(
            pdf = finalPdf,
            discursos = discursos,
            title = title,
            settings = settings,
            totalPagesHint = totalPages,
        )
        outFile.parentFile?.mkdirs()
        outFile.outputStream().use { out -> finalPdf.writeTo(out) }
        finalPdf.close()
        totalPages
    }

    /**
     * Renderiza una única página (la primera) como [Bitmap] para usar
     * en una vista previa. Genera el PDF completo en un archivo
     * temporal y rasteriza la página 1 con [PdfRenderer].
     *
     * El bitmap se devuelve a una resolución de ~1.5x la página A4 en
     * píxeles base, suficiente para verse nítido en pantallas modernas
     * sin consumir memoria innecesaria.
     */
    suspend fun renderPreviewBitmap(
        discursos: List<Discurso>,
        title: String,
        settings: PdfExportSettings = PdfExportSettings(),
    ): Bitmap = withContext(Dispatchers.IO) {
        // Escribimos un PDF temporal en cache.
        val cacheDir = File(context.cacheDir, "pdf_preview").apply { mkdirs() }
        val tempFile = File(cacheDir, "preview_${System.currentTimeMillis()}.pdf")
        val pdf = PdfDocument()
        renderInto(
            pdf = pdf,
            discursos = discursos,
            title = title,
            settings = settings,
            totalPagesHint = null,
        )
        tempFile.outputStream().use { pdf.writeTo(it) }
        pdf.close()

        try {
            android.os.ParcelFileDescriptor.open(
                tempFile,
                android.os.ParcelFileDescriptor.MODE_READ_ONLY,
            ).use { pfd ->
                android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                    val page = renderer.openPage(0)
                    val scale = 1.5f
                    val bitmap = Bitmap.createBitmap(
                        (page.width * scale).toInt(),
                        (page.height * scale).toInt(),
                        Bitmap.Config.ARGB_8888,
                    )
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(
                        bitmap,
                        null,
                        null,
                        android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                    )
                    page.close()
                    bitmap
                }
            }
        } finally {
            // No bloqueamos si falla el delete — el cache se limpia solo.
            tempFile.delete()
        }
    }

    /**
     * Renderiza todo el documento dentro de [pdf]. Devuelve el número
     * de páginas escritas. Si [totalPagesHint] no es null, el header
     * imprime "X de Y"; si es null, sólo "X".
     */
    private fun renderInto(
        pdf: PdfDocument,
        discursos: List<Discurso>,
        title: String,
        settings: PdfExportSettings,
        totalPagesHint: Int?,
    ): Int {
        val pageWidth = 595
        val pageHeight = 842
        val marginX = settings.margin.horizontal
        val marginTop = settings.margin.vertical
        val marginBottom = settings.margin.vertical
        val contentWidth = (pageWidth - marginX * 2).toInt()

        val theme = pdfTheme(settings)
        val pageTitlePaint = TextPaint().apply {
            color = theme.onSurface
            textSize = settings.density.titleSizeSp + 6f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val noteTitlePaint = TextPaint().apply {
            color = theme.onSurface
            textSize = settings.density.titleSizeSp
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = TextPaint().apply {
            color = theme.onSurfaceMuted
            textSize = settings.density.bodySizeSp - 1f
            typeface = Typeface.SANS_SERIF
            isAntiAlias = true
        }
        val sectionPaint = TextPaint().apply {
            color = theme.accent
            textSize = settings.density.bodySizeSp + 1f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = TextPaint().apply {
            color = theme.onSurface
            textSize = settings.density.bodySizeSp
            typeface = Typeface.SANS_SERIF
            isAntiAlias = true
        }
        val captionPaint = TextPaint().apply {
            color = theme.onSurfaceFaint
            textSize = settings.density.bodySizeSp - 2f
            typeface = Typeface.SANS_SERIF
            isAntiAlias = true
        }
        val rulePaint = Paint().apply {
            color = theme.divider
            strokeWidth = 1f
        }
        val headerPaint = TextPaint().apply {
            color = theme.onSurfaceFaint
            textSize = 9f
            typeface = Typeface.SANS_SERIF
            isAntiAlias = true
        }
        val checkboxStrokePaint = Paint().apply {
            color = theme.accent
            strokeWidth = 1.4f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val checkboxFillPaint = Paint().apply {
            color = theme.accent
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Estado de paginación mutable encapsulado en un holder para
        // poder pasarlo a funciones internas sin lambdas anidadas.
        val ctx = RenderCtx(
            pdf = pdf,
            pageWidth = pageWidth,
            pageHeight = pageHeight,
            marginX = marginX,
            marginTop = marginTop,
            marginBottom = marginBottom,
            contentWidth = contentWidth,
            headerPaint = headerPaint,
            captionPaint = captionPaint,
            currentNoteTitle = title,
            totalPagesHint = totalPagesHint,
            watermark = settings.watermark,
        )
        ctx.startFirstPage()

        if (settings.includeCover) {
            // ----- Portada / cabecera -----
            ctx.canvas.drawText(title, marginX, ctx.y + 28, pageTitlePaint)
            ctx.y += 40f
            ctx.canvas.drawText(
                "${discursos.size} nota${if (discursos.size != 1) "s" else ""} · Exportado ${dateFmt.format(Date())}",
                marginX,
                ctx.y + 8,
                subtitlePaint,
            )
            ctx.y += 18f
            ctx.canvas.drawLine(marginX, ctx.y, pageWidth - marginX, ctx.y, rulePaint)
            ctx.y += 16f
        }

        discursos.forEachIndexed { idx, d ->
            ctx.currentNoteTitle = d.title.ifBlank { "Nota" }
            ctx.ensureSpace(80f)

            // Título de la nota
            drawSpannedText(
                ctx,
                spanned = HtmlCompat.fromHtml(
                    "<b>${escapeHtml(d.title.ifBlank { "(Sin título)" })}</b>",
                    HtmlCompat.FROM_HTML_MODE_LEGACY,
                ),
                paint = noteTitlePaint,
                extraSpacing = 4f,
            )

            // Sub-línea: categoría · fecha de creación
            val cat = NoteCategory.fromKey(d.category).label
            val fecha = dateFmt.format(Date(d.createdAt))
            drawSpannedText(
                ctx,
                spanned = HtmlCompat.fromHtml(escapeHtml("$cat · $fecha"), HtmlCompat.FROM_HTML_MODE_LEGACY),
                paint = subtitlePaint,
                extraSpacing = 8f,
            )

            // Escrituras y tags (campos legacy que muchas notas viejas
            // siguen teniendo poblados — los respetamos cuando hay).
            if (d.scriptures.isNotBlank()) {
                drawSpannedText(ctx, plainSpanned("Textos bíblicos"), sectionPaint, extraSpacing = 2f)
                drawSpannedText(ctx, plainSpanned(d.scriptures), bodyPaint, extraSpacing = 8f)
            }
            if (d.tags.isNotBlank()) {
                drawSpannedText(ctx, plainSpanned("Etiquetas"), sectionPaint, extraSpacing = 2f)
                drawSpannedText(ctx, plainSpanned(d.tags), bodyPaint, extraSpacing = 8f)
            }

            // Cuerpo principal: bloques (la migración legacy de
            // `pointsJson` ya está embebida en `notes` via
            // `buildLegacyBody`, no hace falta render aparte).
            val blocks = NoteBlockSerializer.decode(d.notes)
            // Aplicamos el mismo merge legacy que el editor: si la
            // nota es vieja y tiene `pointsJson`, los anteponemos.
            val legacySections = legacySectionsAsBlocks(d)
            val finalBlocks = if (legacySections.isNotEmpty()) {
                legacySections + blocks
            } else {
                blocks
            }
            val hasContent = finalBlocks.any { b ->
                when (b) {
                    is NoteBlock.Text -> b.markdown.isNotBlank()
                    is NoteBlock.Image, is NoteBlock.Video -> true
                    is NoteBlock.Checklist -> b.items.isNotEmpty()
                }
            }
            if (hasContent) {
                finalBlocks.forEach { block ->
                    when (block) {
                        is NoteBlock.Text -> {
                            if (block.markdown.isNotBlank()) {
                                val spanned = HtmlCompat.fromHtml(
                                    block.markdown,
                                    HtmlCompat.FROM_HTML_MODE_LEGACY,
                                )
                                drawSpannedText(ctx, spanned, bodyPaint, extraSpacing = 6f)
                            }
                        }
                        is NoteBlock.Image -> {
                            drawImageBlock(ctx, block.fileName)
                        }
                        is NoteBlock.Video -> {
                            drawVideoBlock(ctx, block.fileName)
                        }
                        is NoteBlock.Checklist -> {
                            drawChecklistBlock(
                                ctx = ctx,
                                items = block.items,
                                bodyPaint = bodyPaint,
                                checkboxStrokePaint = checkboxStrokePaint,
                                checkboxFillPaint = checkboxFillPaint,
                            )
                        }
                    }
                }
            }

            // Separador entre notas
            if (idx < discursos.size - 1) {
                ctx.ensureSpace(20f)
                ctx.canvas.drawLine(marginX, ctx.y + 4f, pageWidth - marginX, ctx.y + 4f, rulePaint)
                ctx.y += 20f
            }
        }

        ctx.finish()
        return ctx.pageNumber
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

    // -----------------------------------------------------------------
    // Helpers privados de renderizado por bloques
    // -----------------------------------------------------------------

    /**
     * Tema de colores derivado de los settings. Si `colored=false`
     * (escala de grises) reemplazamos el accent por gris oscuro.
     */
    private data class PdfTheme(
        val accent: Int,
        val onSurface: Int,
        val onSurfaceMuted: Int,
        val onSurfaceFaint: Int,
        val divider: Int,
    )

    private fun pdfTheme(settings: PdfExportSettings): PdfTheme {
        return if (settings.colored) {
            PdfTheme(
                accent = Color.parseColor("#7C3AED"),
                onSurface = Color.parseColor("#111111"),
                onSurfaceMuted = Color.parseColor("#555555"),
                onSurfaceFaint = Color.parseColor("#999999"),
                divider = Color.parseColor("#E0E0E0"),
            )
        } else {
            PdfTheme(
                accent = Color.parseColor("#2A2A2A"),
                onSurface = Color.parseColor("#111111"),
                onSurfaceMuted = Color.parseColor("#555555"),
                onSurfaceFaint = Color.parseColor("#999999"),
                divider = Color.parseColor("#E0E0E0"),
            )
        }
    }

    private fun escapeHtml(text: String): String =
        text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

    private fun plainSpanned(text: String): Spanned =
        HtmlCompat.fromHtml(escapeHtml(text), HtmlCompat.FROM_HTML_MODE_LEGACY)

    /**
     * Para notas legacy: si la nota tiene `pointsJson` con contenido,
     * lo convertimos a bloques de texto markdown que se anteponen a
     * los bloques actuales. Replica el mismo formato que el editor
     * usa al cargar una nota vieja (`buildLegacyBody`).
     */
    private fun legacySectionsAsBlocks(d: Discurso): List<NoteBlock> {
        if (d.pointsJson.isBlank() || d.pointsJson == "[]") return emptyList()
        val puntos = runCatching {
            json.decodeFromString<List<Punto>>(d.pointsJson)
                .map { it.migrateLegacy() }
        }.getOrDefault(emptyList())
        if (puntos.isEmpty()) return emptyList()
        val md = puntos.joinToString(separator = "\n\n") { p ->
            when {
                p.text.isBlank() -> p.body
                p.body.isBlank() -> "## ${p.text}"
                else -> "## ${p.text}\n\n${p.body}"
            }
        }
        return if (md.isBlank()) emptyList()
        else listOf(NoteBlock.Text(markdown = md))
    }

    /**
     * Dibuja un [Spanned] con `StaticLayout` respetando los spans
     * (negrita, cursiva, color, background, fontSize relativo, etc.).
     * Si el contenido no entra en la página actual, paginamos.
     */
    private fun drawSpannedText(
        ctx: RenderCtx,
        spanned: CharSequence,
        paint: TextPaint,
        extraSpacing: Float = 4f,
    ) {
        if (spanned.isBlank()) return
        // Construimos el StaticLayout una vez para conocer la altura
        // total. Si no entra, partimos en sub-layouts.
        val layout = StaticLayout.Builder
            .obtain(spanned, 0, spanned.length, paint, ctx.contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f, 1.18f)
            .setIncludePad(false)
            .build()

        val available = ctx.availableHeight()
        if (layout.height.toFloat() + extraSpacing <= available) {
            // Cabe entero — dibujo directo.
            ctx.canvas.save()
            ctx.canvas.translate(ctx.marginX, ctx.y)
            layout.draw(ctx.canvas)
            ctx.canvas.restore()
            ctx.y += layout.height.toFloat() + extraSpacing
            return
        }

        // No cabe: dibujamos las líneas que entran y paginamos en
        // medio. Esto preserva el estilo porque cada `range` se vuelve
        // a layoutear con el mismo paint.
        var lineStart = 0
        while (lineStart < layout.lineCount) {
            val avail = ctx.availableHeight()
            // Cuántas líneas caben en la página actual.
            var fit = 0
            while (lineStart + fit < layout.lineCount) {
                val nextHeight = (
                    layout.getLineBottom(lineStart + fit) -
                        layout.getLineTop(lineStart)
                    ).toFloat()
                if (nextHeight > avail) break
                fit++
            }
            if (fit == 0) {
                // Ninguna línea entra: forzamos page break y reintento.
                ctx.newPage()
                continue
            }
            val charStart = layout.getLineStart(lineStart)
            val charEnd = layout.getLineEnd(lineStart + fit - 1)
            val sub = spanned.subSequence(charStart, charEnd)
            val subLayout = StaticLayout.Builder
                .obtain(sub, 0, sub.length, paint, ctx.contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2f, 1.18f)
                .setIncludePad(false)
                .build()
            ctx.canvas.save()
            ctx.canvas.translate(ctx.marginX, ctx.y)
            subLayout.draw(ctx.canvas)
            ctx.canvas.restore()
            ctx.y += subLayout.height.toFloat()
            lineStart += fit
            if (lineStart < layout.lineCount) {
                ctx.newPage()
            }
        }
        ctx.y += extraSpacing
    }

    /**
     * Carga un bitmap del [MediaStorage] downsampleado a un máximo de
     * `MAX_IMAGE_SIDE_PX` y lo dibuja escalado al `contentWidth`.
     * Si no hay storage configurado o el archivo no existe, dibuja un
     * placeholder.
     */
    private fun drawImageBlock(ctx: RenderCtx, fileName: String) {
        val file = mediaStorage?.fileFor(fileName)
        if (file == null || !file.exists()) {
            drawSpannedText(
                ctx,
                plainSpanned("[imagen no disponible: $fileName]"),
                ctx.captionPaint,
                extraSpacing = 8f,
            )
            return
        }
        val bitmap = decodeDownsampled(file, MAX_IMAGE_SIDE_PX) ?: run {
            drawSpannedText(
                ctx,
                plainSpanned("[imagen ilegible: $fileName]"),
                ctx.captionPaint,
                extraSpacing = 8f,
            )
            return
        }
        try {
            drawBitmapFitted(ctx, bitmap)
        } finally {
            // No reciclamos manualmente — al salir de scope el GC lo
            // libera. PdfDocument mantiene los datos serializados de
            // todas formas, así que no es necesario `bitmap.recycle()`.
        }
    }

    /**
     * Extrae un thumbnail del video con [MediaMetadataRetriever] y
     * lo dibuja con un caption "Video". Si falla, dibuja sólo el
     * caption.
     */
    private fun drawVideoBlock(ctx: RenderCtx, fileName: String) {
        val file = mediaStorage?.fileFor(fileName)
        if (file == null || !file.exists()) {
            drawSpannedText(
                ctx,
                plainSpanned("[video no disponible: $fileName]"),
                ctx.captionPaint,
                extraSpacing = 8f,
            )
            return
        }
        val thumb = runCatching {
            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(file.absolutePath)
                // Tomamos un frame cualquiera (1 segundo, o el primero
                // si el video es más corto).
                mmr.getFrameAtTime(1_000_000L) ?: mmr.getFrameAtTime(0L)
            } finally {
                runCatching { mmr.release() }
            }
        }.getOrNull()
        if (thumb != null) {
            drawBitmapFitted(ctx, thumb)
        }
        drawSpannedText(
            ctx,
            plainSpanned("▶ Video — $fileName"),
            ctx.captionPaint,
            extraSpacing = 8f,
        )
    }

    /**
     * Dibuja una lista de checklist como filas con cuadritos
     * (vacío / marcado con check). Cada item es una "línea" que puede
     * envolver si es muy larga.
     */
    private fun drawChecklistBlock(
        ctx: RenderCtx,
        items: List<ChecklistItem>,
        bodyPaint: TextPaint,
        checkboxStrokePaint: Paint,
        checkboxFillPaint: Paint,
    ) {
        val checkboxSize = 11f
        val gap = 6f
        val textLeftPad = checkboxSize + gap
        val itemContentWidth = (ctx.contentWidth - textLeftPad).toInt()

        items.forEach { item ->
            // Pintamos primero el texto (calculamos altura para alinear
            // verticalmente el cuadrito al primer renglón).
            val displayText = if (item.text.isBlank()) " " else item.text
            val htmlEscaped = escapeHtml(displayText)
            val markup = if (item.checked) "<s>$htmlEscaped</s>" else htmlEscaped
            val spanned = HtmlCompat.fromHtml(markup, HtmlCompat.FROM_HTML_MODE_LEGACY)
            val textLayout = StaticLayout.Builder
                .obtain(spanned, 0, spanned.length, bodyPaint, itemContentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2f, 1.15f)
                .setIncludePad(false)
                .build()
            ctx.ensureSpace(textLayout.height.toFloat() + 6f)

            // Cuadrito
            val cbTop = ctx.y + 3f
            val cbRect = RectF(ctx.marginX, cbTop, ctx.marginX + checkboxSize, cbTop + checkboxSize)
            if (item.checked) {
                ctx.canvas.drawRoundRect(cbRect, 2f, 2f, checkboxFillPaint)
                // Tick
                val tickPaint = Paint(checkboxStrokePaint).apply {
                    color = Color.WHITE
                    strokeWidth = 1.6f
                }
                ctx.canvas.drawLine(
                    cbRect.left + 2f, cbRect.top + checkboxSize / 2f,
                    cbRect.left + checkboxSize / 2f - 0.5f, cbRect.bottom - 2f,
                    tickPaint,
                )
                ctx.canvas.drawLine(
                    cbRect.left + checkboxSize / 2f - 0.5f, cbRect.bottom - 2f,
                    cbRect.right - 1.5f, cbRect.top + 2f,
                    tickPaint,
                )
            } else {
                ctx.canvas.drawRoundRect(cbRect, 2f, 2f, checkboxStrokePaint)
            }

            // Texto
            ctx.canvas.save()
            ctx.canvas.translate(ctx.marginX + textLeftPad, ctx.y)
            textLayout.draw(ctx.canvas)
            ctx.canvas.restore()
            ctx.y += textLayout.height.toFloat() + 6f
        }
        ctx.y += 4f
    }

    /**
     * Dibuja [bitmap] centrado horizontalmente, escalado para que el
     * lado largo ocupe `contentWidth`. Si la altura escalada supera
     * lo que queda en la página, paginamos primero. Si supera el alto
     * disponible incluso en una página vacía, se reescala más para
     * que entre completo.
     */
    private fun drawBitmapFitted(ctx: RenderCtx, bitmap: Bitmap) {
        val targetW = ctx.contentWidth.toFloat()
        val ratio = bitmap.height.toFloat() / bitmap.width.toFloat()
        var targetH = targetW * ratio

        val pageContentHeight = ctx.pageHeight - ctx.marginTop - ctx.marginBottom
        if (targetH > pageContentHeight) {
            // No entra ni en una página completa: reducimos para que sí.
            targetH = pageContentHeight - 8f
        }
        val effectiveTargetW = targetH / ratio

        ctx.ensureSpace(targetH + 8f)
        val left = ctx.marginX + (ctx.contentWidth - effectiveTargetW) / 2f
        val rect = RectF(left, ctx.y, left + effectiveTargetW, ctx.y + targetH)
        ctx.canvas.drawBitmap(bitmap, null, rect, null)
        ctx.y += targetH + 8f
    }

    /** Decodifica un archivo de imagen con downsampling para no
     *  consumir memoria innecesaria. */
    private fun decodeDownsampled(file: File, maxSidePx: Int): Bitmap? {
        // Primer pase: solo leer dimensiones.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching { BitmapFactory.decodeFile(file.absolutePath, bounds) }
            .getOrElse { return null }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (longest / sample > maxSidePx) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return runCatching { BitmapFactory.decodeFile(file.absolutePath, opts) }
            .getOrNull()
    }

    /**
     * Holder con el estado de paginación + paints comunes que las
     * funciones de render necesitan. Evita pasar 10 parámetros por
     * llamada.
     */
    private class RenderCtx(
        val pdf: PdfDocument,
        val pageWidth: Int,
        val pageHeight: Int,
        val marginX: Float,
        val marginTop: Float,
        val marginBottom: Float,
        val contentWidth: Int,
        val headerPaint: TextPaint,
        val captionPaint: TextPaint,
        var currentNoteTitle: String,
        val totalPagesHint: Int? = null,
        val watermark: Boolean = true,
    ) {
        var pageNumber: Int = 0
        lateinit var page: PdfDocument.Page
        lateinit var canvas: Canvas
        var y: Float = 0f

        fun startFirstPage() {
            pageNumber = 1
            val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdf.startPage(info)
            canvas = page.canvas
            y = marginTop
            drawHeader()
        }

        fun newPage() {
            pdf.finishPage(page)
            pageNumber += 1
            val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdf.startPage(info)
            canvas = page.canvas
            y = marginTop
            drawHeader()
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > pageHeight - marginBottom) newPage()
        }

        fun availableHeight(): Float = pageHeight - marginBottom - y

        fun finish() {
            // Footer en la última página antes de cerrarla.
            if (watermark) {
                canvas.drawText(
                    "OreoExperience Notes",
                    marginX,
                    pageHeight - marginBottom + 18f,
                    headerPaint,
                )
            }
            val pageStr = pageNumberLabel()
            canvas.drawText(
                pageStr,
                pageWidth - marginX - headerPaint.measureText(pageStr),
                pageHeight - marginBottom + 18f,
                headerPaint,
            )
            pdf.finishPage(page)
        }

        private fun drawHeader() {
            val left = "OreoExperience · ${currentNoteTitle.take(60)}"
            canvas.drawText(left, marginX, 32f, headerPaint)
            val right = pageNumberLabel()
            val rightX = pageWidth - marginX - headerPaint.measureText(right)
            canvas.drawText(right, rightX, 32f, headerPaint)
        }

        private fun pageNumberLabel(): String {
            return if (totalPagesHint != null) {
                "Página $pageNumber de $totalPagesHint"
            } else {
                "Página $pageNumber"
            }
        }
    }

    companion object {
        /** Lado más largo en píxeles al que reescalamos las imágenes
         *  antes de embeberlas. ~200 dpi a ancho de página completo. */
        private const val MAX_IMAGE_SIDE_PX = 1500
    }
}
