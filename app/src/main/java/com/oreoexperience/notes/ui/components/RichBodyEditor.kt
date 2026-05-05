@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi::class,
)

package com.oreoexperience.notes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

/**
 * Editor de cuerpo de sección con barra de formato. **WYSIWYG** —
 * mientras escribís, ves el texto formateado (negritas en negrita,
 * resaltado con fondo, etc.) en lugar de los marcadores markdown
 * literales.
 *
 * Internamente usa la librería `compose-rich-editor` (MohamedRejeb)
 * y serializa el contenido como Markdown estándar para mantener
 * compatibilidad con discursos viejos y con los exports a `.md`.
 */
@Composable
fun RichBodyEditor(
    text: String,
    onTextChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val state = rememberRichTextState()

    // Cargamos el markdown inicial una sola vez por instancia. Si el texto
    // externo cambia (load del discurso), refrescamos.
    LaunchedEffect(text) {
        if (state.toMarkdown() != text) {
            state.setMarkdown(text)
        }
    }

    // Reportamos los cambios hacia afuera. Saltamos el primer emit para no
    // re-disparar onTextChange con el contenido recién cargado. La
    // comparación md != text en el primer LaunchedEffect evita el loop
    // cuando el padre nos pasa de vuelta el mismo markdown que acabamos
    // de generar.
    LaunchedEffect(state) {
        snapshotFlow { state.annotatedString }
            .drop(1)
            .distinctUntilChanged()
            .collect {
                onTextChange(state.toMarkdown())
            }
    }

    Column(modifier = modifier) {
        Toolbar(state = state)
        Spacer(Modifier.height(4.dp))
        OutlinedRichTextEditor(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp),
            placeholder = { Text(placeholder, color = OreoPalette.OnSurfaceMuted) },
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = OreoPalette.OnSurface,
            ),
        )
    }
}

private val HighlightSpan = SpanStyle(background = Color(0xFFFFD66B).copy(alpha = 0.55f))
private val HeadingSpan = SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold)

@Composable
private fun Toolbar(state: RichTextState) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ToolbarButton(
            icon = Icons.Outlined.FormatBold,
            description = "Negrita",
            active = state.currentSpanStyle.fontWeight == FontWeight.Bold,
            onClick = { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
        )
        ToolbarButton(
            icon = Icons.Outlined.FormatItalic,
            description = "Cursiva",
            active = state.currentSpanStyle.fontStyle == FontStyle.Italic,
            onClick = { state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
        )
        ToolbarButton(
            icon = Icons.Outlined.FormatUnderlined,
            description = "Subrayado",
            active = state.currentSpanStyle.textDecoration?.contains(TextDecoration.Underline) == true,
            onClick = { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) },
        )
        ToolbarButton(
            icon = Icons.Outlined.FormatStrikethrough,
            description = "Tachado",
            active = state.currentSpanStyle.textDecoration?.contains(TextDecoration.LineThrough) == true,
            onClick = { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) },
        )
        ToolbarButton(
            icon = Icons.Outlined.FormatColorFill,
            description = "Resaltar",
            active = state.currentSpanStyle.background == HighlightSpan.background,
            onClick = { state.toggleSpanStyle(HighlightSpan) },
        )
        Divider()
        ToolbarButton(
            icon = Icons.Outlined.Title,
            description = "Encabezado",
            active = state.currentSpanStyle.fontSize == HeadingSpan.fontSize,
            onClick = { state.toggleSpanStyle(HeadingSpan) },
        )
        ToolbarButton(
            icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
            description = "Lista",
            active = state.isUnorderedList,
            onClick = { state.toggleUnorderedList() },
        )
        ToolbarButton(
            icon = Icons.Outlined.FormatListNumbered,
            description = "Lista numerada",
            active = state.isOrderedList,
            onClick = { state.toggleOrderedList() },
        )
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (active) OreoPalette.Accent else OreoPalette.AccentSub,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .height(20.dp)
            .width(1.dp)
            .background(OreoPalette.OnSurfaceMuted.copy(alpha = 0.25f)),
    )
}

