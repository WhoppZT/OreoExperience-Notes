package com.oreoexperience.notes.ui.editor

import com.oreoexperience.notes.data.NoteBlock

/**
 * Representa una coincidencia encontrada en un bloque de texto o checklist.
 * Los offsets [plainStart] y [plainEnd] son sobre el texto plano del
 * RichTextState (annotatedString.text), NO sobre el HTML guardado.
 */
data class FindMatch(
    val blockId: String,
    val itemId: String? = null,
    val plainStart: Int,
    val plainEnd: Int,
)

/**
 * Estado de UI del panel de Find & Replace.
 */
data class FindUiState(
    val open: Boolean = false,
    val query: String = "",
    val replacement: String = "",
    val caseSensitive: Boolean = false,
    val useRegex: Boolean = false,
    val regexError: Boolean = false,
    val matches: List<FindMatch> = emptyList(),
    val currentIndex: Int = 0,
) {
    val matchCount: Int get() = matches.size
}

/**
 * Motor de búsqueda y reemplazo para notas basadas en bloques.
 *
 * Trabaja sobre el texto plano expuesto por `RichTextState.annotatedString.text`,
 * no sobre el HTML, para que los offsets coincidan con `richState.selection`.
 */
object FindReplaceEngine {

    /**
     * Calcula todas las coincidencias del query en los bloques de la nota.
     * Si [useRegex] es true, [query] se interpreta como regex; si no,
     * se escapa automáticamente para búsqueda literal.
     */
    fun computeMatches(
        blocks: List<NoteBlock>,
        blockTexts: Map<String, String>,
        query: String,
        caseSensitive: Boolean,
        useRegex: Boolean,
    ): Pair<List<FindMatch>, Boolean> {
        if (query.isBlank()) return Pair(emptyList(), false)

        val regexOpts = if (!caseSensitive) setOf(RegexOption.IGNORE_CASE) else emptySet()

        val pattern = runCatching {
            if (useRegex) Regex(query, regexOpts)
            else Regex(Regex.escape(query), regexOpts)
        }.getOrElse {
            return Pair(emptyList(), true)
        }

        val results = mutableListOf<FindMatch>()

        for (block in blocks) {
            when (block) {
                is NoteBlock.Text -> {
                    val plain = blockTexts[block.id] ?: continue
                    pattern.findAll(plain).forEach { m ->
                        results.add(
                            FindMatch(
                                blockId = block.id,
                                plainStart = m.range.first,
                                plainEnd = m.range.last + 1,
                            ),
                        )
                    }
                }
                is NoteBlock.Checklist -> {
                    for (item in block.items) {
                        pattern.findAll(item.text).forEach { m ->
                            results.add(
                                FindMatch(
                                    blockId = block.id,
                                    itemId = item.id,
                                    plainStart = m.range.first,
                                    plainEnd = m.range.last + 1,
                                ),
                            )
                        }
                    }
                }
                is NoteBlock.Image, is NoteBlock.Video -> {}
            }
        }

        return Pair(results, false)
    }

    /**
     * Navega al siguiente match (wrap-around).
     */
    fun nextIndex(current: Int, size: Int): Int {
        if (size == 0) return 0
        return (current + 1) % size
    }

    /**
     * Navega al match anterior (wrap-around).
     */
    fun prevIndex(current: Int, size: Int): Int {
        if (size == 0) return 0
        return (current - 1 + size) % size
    }

    /**
     * Reemplaza la coincidencia actual en el texto plano.
     * Devuelve el nuevo texto.
     *
     * Compromiso: el texto insertado hereda el SpanStyle del primer
     * carácter del rango original (como Notion / Google Docs).
     */
    fun replaceOne(
        plainText: String,
        match: FindMatch,
        replacement: String,
    ): String {
        return plainText.substring(0, match.plainStart) +
            replacement +
            plainText.substring(match.plainEnd)
    }

    /**
     * Reemplaza una coincidencia dentro del HTML del bloque, preservando
     * las etiquetas de formato (<strong>, <em>, <span>, etc.).
     *
     * Recorre el HTML carácter por carácter manejando correctamente la
     * correspondencia entre el texto plano del RichTextState
     * (annotatedString.text, que usa \n para saltos) y el HTML generado
     * por toHtml() (que usa <br>, </p>, \n literal, etc.).
     *
     * Las etiquetas <br> y </p> representan caracteres \n en el texto
     * plano, pero no aparecen como texto en el HTML, por lo que se
     * incrementa plainIdx al encontrarlas.
     */
    fun replaceInHtml(
        html: String,
        matchText: String,
        replacement: String,
        plainStart: Int,
        escapeHtml: Boolean = true,
    ): String {
        val safeReplacement = if (escapeHtml) {
            replacement
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
        } else {
            replacement
        }

        val result = StringBuilder()
        var plainIdx = 0
        var htmlIdx = 0
        val targetPlainEnd = plainStart + matchText.length
        var replacementEmitted = false

        while (htmlIdx < html.length) {
            if (html[htmlIdx] == '<') {
                val tagEnd = html.indexOf('>', htmlIdx)
                if (tagEnd == -1) {
                    result.append(html, htmlIdx, html.length)
                    break
                }
                val tagLower = html.substring(htmlIdx + 1, tagEnd).lowercase()
                val isLineBreak = tagLower.startsWith("br")
                val isParaEnd = tagLower.startsWith("/p")

                val insideMatch = plainIdx in plainStart until targetPlainEnd
                if (!insideMatch) {
                    result.append(html, htmlIdx, tagEnd + 1)
                } else if (!replacementEmitted) {
                    result.append(safeReplacement)
                    replacementEmitted = true
                }
                htmlIdx = tagEnd + 1

                if (isLineBreak || isParaEnd) {
                    if (insideMatch && !replacementEmitted) {
                        result.append(safeReplacement)
                        replacementEmitted = true
                    }
                    plainIdx++
                }
            } else if (html[htmlIdx] == '\n') {
                // Newline literal entre tags (ej: </p>\n<p>)
                val insideMatch = plainIdx in plainStart until targetPlainEnd
                if (!insideMatch) {
                    result.append(html[htmlIdx])
                } else if (!replacementEmitted) {
                    result.append(safeReplacement)
                    replacementEmitted = true
                }
                plainIdx++
                htmlIdx++
            } else if (plainIdx in plainStart until targetPlainEnd) {
                // Dentro del rango del match
                if (!replacementEmitted) {
                    result.append(safeReplacement)
                    replacementEmitted = true
                }
                plainIdx++
                htmlIdx++
            } else {
                result.append(html[htmlIdx])
                plainIdx++
                htmlIdx++
            }
        }

        return result.toString()
    }

    /**
     * Reemplaza todas las coincidencias en un bloque, de derecha a
     * izquierda para no invalidar los offsets.
     *
     * Devuelve el nuevo texto plano.
     */
    fun replaceAllInBlock(
        plainText: String,
        blockMatches: List<FindMatch>,
        replacement: String,
    ): String {
        val sorted = blockMatches.sortedByDescending { it.plainStart }
        var result = plainText
        for (m in sorted) {
            result = result.substring(0, m.plainStart) + replacement + result.substring(m.plainEnd)
        }
        return result
    }

    /**
     * Reemplaza todas las coincidencias dentro del HTML del bloque,
     * preservando las etiquetas de formato. Procesa de derecha a
     * izquierda para no invalidar offsets.
     */
    fun replaceAllInHtml(
        html: String,
        matchText: String,
        replacement: String,
        matches: List<FindMatch>,
    ): String {
        val sorted = matches.sortedByDescending { it.plainStart }
        var result = html
        for (m in sorted) {
            result = replaceInHtml(result, matchText, replacement, m.plainStart)
        }
        return result
    }
}
