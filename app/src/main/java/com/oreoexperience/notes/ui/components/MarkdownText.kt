package com.oreoexperience.notes.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.oreoexperience.notes.ui.theme.OreoPalette

/**
 * Renderizador de markdown muy simple, escrito a mano para no depender
 * de ninguna librería externa. Soporta el subconjunto que usa nuestra
 * toolbar:
 *
 * Inline:
 *  - `**negrita**`
 *  - `*cursiva*`
 *  - `<u>subrayado</u>` (HTML inline; markdown estándar no tiene)
 *  - `~~tachado~~`
 *  - `==resaltado==` (extensión común — fondo amarillo)
 *
 * Bloque (línea entera):
 *  - `# Heading`, `## Heading`, `### Heading`
 *  - `- item` (lista con viñeta)
 *  - `1. item` (lista numerada)
 *  - `- [ ] item` / `- [x] item` (checklist)
 *
 * Cualquier otra línea queda como párrafo normal con formato inline.
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    highlightColor: Color = Color(0xFFFFD66B).copy(alpha = 0.55f),
) {
    val lines = text.split('\n')
    Column(modifier = modifier) {
        lines.forEachIndexed { idx, raw ->
            val trimmed = raw
            when {
                trimmed.isEmpty() -> {
                    // Línea vacía → respiro vertical (1/2 línea de altura).
                    Spacer(Modifier.height((style.fontSize.value / 2).dp))
                }
                trimmed.startsWith("### ") -> HeadingLine(trimmed.removePrefix("### "), style, color, level = 3, highlightColor)
                trimmed.startsWith("## ") -> HeadingLine(trimmed.removePrefix("## "), style, color, level = 2, highlightColor)
                trimmed.startsWith("# ") -> HeadingLine(trimmed.removePrefix("# "), style, color, level = 1, highlightColor)
                trimmed.startsWith("- [ ] ") -> ChecklistLine(
                    trimmed.removePrefix("- [ ] "), checked = false, style, color, highlightColor)
                trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ") ->
                    ChecklistLine(trimmed.substring(6), checked = true, style, color, highlightColor)
                trimmed.startsWith("- ") -> BulletLine(trimmed.removePrefix("- "), style, color, highlightColor)
                NUMBERED_REGEX.matchEntire(trimmed) != null -> {
                    val match = NUMBERED_REGEX.matchEntire(trimmed)!!
                    val number = match.groupValues[1]
                    val content = match.groupValues[2]
                    NumberedLine(number, content, style, color, highlightColor)
                }
                else -> {
                    Text(
                        text = parseInline(trimmed, highlightColor),
                        style = style,
                        color = color,
                    )
                }
            }
            if (idx != lines.lastIndex && trimmed.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

private val NUMBERED_REGEX = Regex("""^(\d+)\.\s+(.*)$""")

@Composable
private fun HeadingLine(content: String, style: TextStyle, color: Color, level: Int, highlightColor: Color) {
    val scale = when (level) {
        1 -> 1.5f
        2 -> 1.3f
        else -> 1.15f
    }
    val headingStyle = style.copy(
        fontWeight = FontWeight.Bold,
        fontSize = style.fontSize * scale,
    )
    Text(
        text = parseInline(content, highlightColor),
        style = headingStyle,
        color = if (color == Color.Unspecified) OreoPalette.Accent else color,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun BulletLine(content: String, style: TextStyle, color: Color, highlightColor: Color) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "•",
            style = style,
            color = if (color == Color.Unspecified) OreoPalette.AccentSub else color,
            modifier = Modifier.padding(end = 8.dp, start = 4.dp),
        )
        Text(
            text = parseInline(content, highlightColor),
            style = style,
            color = color,
        )
    }
}

@Composable
private fun NumberedLine(number: String, content: String, style: TextStyle, color: Color, highlightColor: Color) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "$number.",
            style = style,
            color = if (color == Color.Unspecified) OreoPalette.AccentSub else color,
            modifier = Modifier.padding(end = 8.dp, start = 4.dp),
        )
        Text(
            text = parseInline(content, highlightColor),
            style = style,
            color = color,
        )
    }
}

@Composable
private fun ChecklistLine(content: String, checked: Boolean, style: TextStyle, color: Color, highlightColor: Color) {
    val box = 18.dp
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(
            modifier = Modifier
                .padding(end = 8.dp, top = 2.dp)
                .size(box)
                .clip(RoundedCornerShape(4.dp))
                .then(
                    if (checked) Modifier
                    else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Borde + fondo manuales para no depender del CheckBox de M3
            // (que ocupa más, tiene padding y trae splash táctil).
            androidx.compose.foundation.Canvas(modifier = Modifier.size(box)) {
                val stroke = 1.5.dp.toPx()
                val r = 4.dp.toPx()
                if (checked) {
                    drawRoundRect(
                        color = OreoPalette.Accent,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                    )
                } else {
                    drawRoundRect(
                        color = OreoPalette.OnSurfaceMuted.copy(alpha = 0.7f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
                    )
                }
            }
            if (checked) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color(0xFF1A0B33),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Text(
            text = parseInline(content, highlightColor),
            style = if (checked) style.copy(textDecoration = TextDecoration.LineThrough) else style,
            color = if (checked && color == Color.Unspecified) OreoPalette.OnSurfaceMuted
            else color,
        )
    }
}

/**
 * Formato inline. Implementación con state-machine simple:
 * busca el marcador de apertura, encuentra el de cierre, aplica el
 * estilo a lo del medio, y sigue. Si no encuentra cierre, trata el
 * marcador como texto literal.
 *
 * No soporta anidación (ej. **_x_** no funciona como bold-italic).
 * Para el caso de uso (notas de discurso) es más que suficiente.
 */
fun parseInline(text: String, highlightColor: Color): AnnotatedString = buildAnnotatedString {
    val patterns = listOf(
        InlinePattern("**", "**", SpanStyle(fontWeight = FontWeight.Bold)),
        InlinePattern("__", "__", SpanStyle(fontWeight = FontWeight.Bold)),
        InlinePattern("*", "*", SpanStyle(fontStyle = FontStyle.Italic)),
        InlinePattern("_", "_", SpanStyle(fontStyle = FontStyle.Italic)),
        InlinePattern("~~", "~~", SpanStyle(textDecoration = TextDecoration.LineThrough)),
        InlinePattern("==", "==", SpanStyle(background = highlightColor)),
        InlinePattern("<u>", "</u>", SpanStyle(textDecoration = TextDecoration.Underline)),
    )

    var i = 0
    while (i < text.length) {
        // Probamos cada patrón en el orden definido (los marcadores más
        // largos primero — `**` antes que `*` para que no confunda).
        val match = patterns.firstOrNull { p -> text.startsWith(p.open, i) }
        if (match != null) {
            val contentStart = i + match.open.length
            val end = text.indexOf(match.close, contentStart)
            // Para que `*` no cierre un `**`, validamos que no sea otro
            // marcador más largo del mismo tipo (ej. apertura `*` con
            // cierre `**`).
            val validClose = end >= 0 && !patterns.any { other ->
                other.open != match.open
                    && other.open.startsWith(match.open)
                    && text.startsWith(other.open, end - (other.open.length - match.close.length).coerceAtLeast(0))
            }
            if (validClose) {
                withStyle(match.style) {
                    append(text.substring(contentStart, end))
                }
                i = end + match.close.length
                continue
            }
        }
        append(text[i])
        i++
    }
}

private data class InlinePattern(val open: String, val close: String, val style: SpanStyle)
