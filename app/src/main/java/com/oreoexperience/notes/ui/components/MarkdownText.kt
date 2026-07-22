package com.oreoexperience.notes.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.ui.theme.OreoPalette

/**
 * Renderiza Markdown básico (###, **bold**, *italic*, - bullets, > quotes, [links](url)).
 * Sin librería externa — parser inline manual que genera Columnas de Text.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val lines = markdown.lines()

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        var inList = false
        for ((idx, raw) in lines.withIndex()) {
            val line = raw.trimEnd()

            when {
                // Encabezado ###
                line.startsWith("### ") -> {
                    inList = false
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = line.removePrefix("### "),
                        color = OreoPalette.OnSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                }
                // Encabezado ##
                line.startsWith("## ") -> {
                    inList = false
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = line.removePrefix("## "),
                        color = OreoPalette.OnSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                // Cita >
                line.startsWith("> ") -> {
                    inList = false
                    Row {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(14.dp)
                                .padding(top = 2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(OreoPalette.Accent),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            // La cita puede contener inline markup
                            val content = line.removePrefix("> ")
                            InlineMarkdownText(
                                text = content,
                                color = OreoPalette.OnSurfaceMuted,
                                fontSize = 13.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                // Línea vacía → separador entre párrafos
                line.isBlank() -> {
                    inList = false
                    Spacer(Modifier.height(8.dp))
                }
                // Bullet list: "- " o "* "
                (line.startsWith("- ") || line.startsWith("* ")) -> {
                    inList = true
                    val content = line.removePrefix("- ").removePrefix("* ")
                    Row(
                        modifier = Modifier.padding(start = 4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(OreoPalette.Accent),
                        )
                        Spacer(Modifier.width(10.dp))
                        InlineMarkdownText(
                            text = content,
                            color = OreoPalette.OnSurface,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
                // Línea después de una lista sin guión → párrafo normal
                else -> {
                    inList = false
                    InlineMarkdownText(
                        text = line,
                        color = OreoPalette.OnSurface,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

/**
 * Renderiza el texto inline con soporte para **bold**, *italic*,
 * y [links](url).
 */
@Composable
private fun InlineMarkdownText(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val annotated = buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                // Link: [texto](url)
                text[i] == '[' -> {
                    val closeBracket = text.indexOf(']', i)
                    val openParen = if (closeBracket > i) text.indexOf('(', closeBracket) else -1
                    val closeParen = if (openParen > closeBracket) text.indexOf(')', openParen) else -1

                    if (closeBracket > i && openParen == closeBracket + 1 && closeParen > openParen) {
                        val linkText = text.substring(i + 1, closeBracket)
                        val url = text.substring(openParen + 1, closeParen)
                        pushStringAnnotation("URL", url)
                        withStyle(SpanStyle(
                            color = OreoPalette.Accent,
                            textDecoration = TextDecoration.Underline,
                        )) {
                            append(linkText)
                        }
                        pop()
                        i = closeParen + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Bold: **texto**
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end > i + 2) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Italic: *texto* (pero no **)
                text[i] == '*' && i + 1 < text.length && text[i + 1] != '*' -> {
                    val end = text.indexOf('*', i + 1)
                    if (end > i + 1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Cursiva con guión bajo: _texto_
                text[i] == '_' -> {
                    val end = text.indexOf('_', i + 1)
                    if (end > i + 1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }

    Text(
        text = annotated,
        color = color,
        fontSize = fontSize,
        lineHeight = 22.sp,
        modifier = modifier,
    )

    // Click handler para links — fuera del buildAnnotatedString
    // porque necesitamos click en el Text
    // Los links se manejan con StringAnnotation de Compose
    // pero el click hay que ponerlo en el Text directamente.
    // Simplificamos: detectamos links y los hacemos tappeables.
    // En la práctica con AnnotatedString + ClickableText sería mejor,
    // pero para evitar dependencias extra usamos ClickableText de Compose.
}

/**
 * Versión con ClickableText para links funcionales.
 * Se usa en el cuerpo del diálogo de actualización.
 */
@Composable
fun ClickableMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    onLinkClick: (String) -> Unit,
) {
    val lines = markdown.lines()
    val ctx = LocalContext.current

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        for (line in lines) {
            val trimmed = line.trimEnd()
            when {
                trimmed.startsWith("### ") -> {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = trimmed.removePrefix("### "),
                        color = OreoPalette.OnSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                }
                trimmed.startsWith("## ") -> {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = trimmed.removePrefix("## "),
                        color = OreoPalette.OnSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                trimmed.startsWith("> ") -> {
                    Row {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(14.dp)
                                .padding(top = 2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(OreoPalette.Accent),
                        )
                        Spacer(Modifier.width(12.dp))
                        ClickableInlineText(
                            text = trimmed.removePrefix("> "),
                            color = OreoPalette.OnSurfaceMuted,
                            fontSize = 13.sp,
                            onLinkClick = onLinkClick,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
                trimmed.isBlank() -> Spacer(Modifier.height(8.dp))
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val content = trimmed.removePrefix("- ").removePrefix("* ")
                    Row(modifier = Modifier.padding(start = 4.dp)) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(OreoPalette.Accent),
                        )
                        Spacer(Modifier.width(10.dp))
                        ClickableInlineText(
                            text = content,
                            color = OreoPalette.OnSurface,
                            fontSize = 14.sp,
                            onLinkClick = onLinkClick,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
                else -> {
                    ClickableInlineText(
                        text = trimmed,
                        color = OreoPalette.OnSurface,
                        fontSize = 14.sp,
                        onLinkClick = onLinkClick,
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

/**
 * Texto inline con **bold**, *italic*, y [links](url) clickeables.
 * Los links abren el navegador por defecto (Intent.ACTION_VIEW).
 */
@Composable
private fun ClickableInlineText(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val annotated = buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text[i] == '[' -> {
                    val closeBracket = text.indexOf(']', i)
                    val openParen = if (closeBracket > i) text.indexOf('(', closeBracket) else -1
                    val closeParen = if (openParen > closeBracket) text.indexOf(')', openParen) else -1
                    if (closeBracket > i && openParen == closeBracket + 1 && closeParen > openParen) {
                        val linkText = text.substring(i + 1, closeBracket)
                        val url = text.substring(openParen + 1, closeParen)
                        pushStringAnnotation("URL", url)
                        withStyle(SpanStyle(
                            color = OreoPalette.Accent,
                            textDecoration = TextDecoration.Underline,
                        )) {
                            append(linkText)
                        }
                        pop()
                        i = closeParen + 1
                    } else {
                        append(text[i++])
                    }
                }
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end > i + 2) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(i + 2, end)) }
                        i = end + 2
                    } else { append(text[i++]) }
                }
                text[i] == '*' && i + 1 < text.length && text[i + 1] != '*' -> {
                    val end = text.indexOf('*', i + 1)
                    if (end > i + 1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, end)) }
                        i = end + 1
                    } else { append(text[i++]) }
                }
                text[i] == '_' -> {
                    val end = text.indexOf('_', i + 1)
                    if (end > i + 1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, end)) }
                        i = end + 1
                    } else { append(text[i++]) }
                }
                else -> append(text[i++])
            }
        }
    }

    androidx.compose.foundation.text.ClickableText(
        text = annotated,
        style = TextStyle(color = color, fontSize = fontSize, lineHeight = 22.sp),
        modifier = modifier,
        onClick = { offset ->
            annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let {
                onLinkClick(it.item)
            }
        },
    )
}
