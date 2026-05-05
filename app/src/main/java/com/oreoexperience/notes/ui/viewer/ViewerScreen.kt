package com.oreoexperience.notes.ui.viewer

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oreoexperience.notes.R
import com.oreoexperience.notes.data.Discurso
import com.oreoexperience.notes.data.Punto
import com.oreoexperience.notes.data.UserPreferences
import androidx.compose.foundation.layout.Box
import com.oreoexperience.notes.ui.LocalAppContainer
import com.oreoexperience.notes.ui.components.FloatingTimerWindow
import com.oreoexperience.notes.ui.components.GlassCard
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText
import com.oreoexperience.notes.ui.theme.OreoPalette
import com.oreoexperience.notes.util.ExportUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    discursoId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val discursoState = container.repository.observeById(discursoId)
        .collectAsStateWithLifecycle(initialValue = null)
    val discurso = discursoState.value
    // Decodificamos puntos y aplicamos la migración legacy:
    //   - puntos viejos con `subpoints` se convierten a un body unificado
    //   - si el discurso tiene un `notes` legacy, se añade como sección al final
    val points = remember(discurso?.pointsJson, discurso?.notes) {
        if (discurso == null) {
            emptyList()
        } else {
            val raw = container.repository.decodePoints(discurso.pointsJson)
                .map { it.migrateLegacy() }
                .toMutableList()
            if (discurso.notes.isNotBlank()) {
                raw += Punto(text = "Notas", body = discurso.notes)
            }
            raw
        }
    }

    var menuOpen by remember { mutableStateOf(false) }
    var fontMenuOpen by remember { mutableStateOf(false) }
    val fontScale by UserPreferences.fontScale

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                title = {
                    Text(
                        text = discurso?.title ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = OreoPalette.OnSurface,
                    )
                },
                actions = {
                    Box {
                        IconButton(onClick = { fontMenuOpen = true }) {
                            Icon(
                                Icons.Outlined.FormatSize,
                                contentDescription = "Tamaño de letra",
                                tint = OreoPalette.OnSurfaceMuted,
                            )
                        }
                        FontSizeMenu(
                            expanded = fontMenuOpen,
                            currentScale = fontScale,
                            onScaleChange = { UserPreferences.setFontScale(context, it) },
                            onDismiss = { fontMenuOpen = false },
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.viewer_edit), tint = OreoPalette.Accent)
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.viewer_share))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.viewer_export_txt)) },
                            leadingIcon = { Icon(Icons.Outlined.TextSnippet, null) },
                            onClick = {
                                menuOpen = false
                                discurso?.let {
                                    scope.launch {
                                        val uri = ExportUtil.writeTextToCache(
                                            context, "${it.title.fileSafe()}.txt", ExportUtil.toPlain(it, points))
                                        context.startActivity(ExportUtil.shareIntent(context, uri, "text/plain"))
                                    }
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.viewer_export_md)) },
                            leadingIcon = { Icon(Icons.Outlined.TextSnippet, null) },
                            onClick = {
                                menuOpen = false
                                discurso?.let {
                                    scope.launch {
                                        val uri = ExportUtil.writeTextToCache(
                                            context, "${it.title.fileSafe()}.md", ExportUtil.toMarkdown(it, points))
                                        context.startActivity(ExportUtil.shareIntent(context, uri, "text/markdown"))
                                    }
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.viewer_export_pdf)) },
                            leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, null) },
                            onClick = {
                                menuOpen = false
                                discurso?.let {
                                    scope.launch {
                                        val uri = com.oreoexperience.notes.util.PdfExporter.export(context, it, points)
                                        context.startActivity(ExportUtil.shareIntent(context, uri, "application/pdf"))
                                    }
                                }
                            },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        }
    ) { padding ->
        if (discurso == null) return@Scaffold
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ViewerBody(
                modifier = Modifier.fillMaxSize(),
                discurso = discurso,
                points = points,
                fontScale = fontScale,
            )
            // Mini ventana flotante — queda encima del contenido y
            // se puede arrastrar a cualquier parte de la pantalla.
            FloatingTimerWindow(targetSec = discurso.targetDurationSec)
        }
    }
}

@Composable
private fun ViewerBody(
    modifier: Modifier,
    discurso: Discurso,
    points: List<Punto>,
    fontScale: Float,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = discurso.title.ifBlank { "(sin título)" },
                        style = MaterialTheme.typography.headlineSmall.scaled(fontScale),
                        fontWeight = FontWeight.Bold,
                        color = OreoPalette.OnSurface,
                    )
                    if (discurso.scriptures.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = discurso.scriptures,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OreoPalette.AccentSub,
                        )
                    }
                    if (discurso.tags.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            discurso.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(8).forEach { tag ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(tag) },
                                    shape = RoundedCornerShape(50),
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = OreoPalette.GlassFillStrong,
                                        labelColor = OreoPalette.OnSurface,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (points.isNotEmpty()) {
            item {
                Text(
                    text = "Puntos del discurso",
                    style = MaterialTheme.typography.titleSmall,
                    color = OreoPalette.OnSurfaceMuted,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )
            }
            points.forEachIndexed { idx, p ->
                item(key = "vp$idx") {
                    // Cada sección usa el glass "frosted" — fondo más opaco
                    // para que el texto resalte cuando estás dando el discurso.
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        fill = OreoPalette.GlassFillFrosted,
                        borderAlpha = 0.6f,
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Título de la sección en violeta accent. Si no
                            // hay título, mostramos sólo el cuerpo (sin
                            // numeración ni placeholder).
                            if (p.text.isNotBlank()) {
                                Text(
                                    text = p.text,
                                    style = MaterialTheme.typography.titleLarge.scaled(fontScale),
                                    fontWeight = FontWeight.SemiBold,
                                    color = OreoPalette.Accent,
                                )
                                if (p.body.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                            if (p.body.isNotBlank()) {
                                MarkdownBody(
                                    markdown = p.body,
                                    style = MaterialTheme.typography.bodyLarge.scaled(fontScale),
                                    color = OreoPalette.OnSurface,
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}

private fun String.fileSafe(): String =
    ifBlank { "discurso" }
        .replace(Regex("[^A-Za-z0-9._-]+"), "_")
        .take(64)

/**
 * Renderiza un fragmento de markdown usando el mismo motor que el editor
 * WYSIWYG. Esto garantiza que lo que ves en la nota coincida exactamente
 * con lo que se guardó (incluyendo `<u>...</u>`, `~~...~~`, listas, etc.)
 * sin tener que mantener nuestro propio parser.
 *
 * Cada llamada crea un [com.mohamedrejeb.richeditor.model.RichTextState]
 * propio y setea el markdown una sola vez. La key `markdown` asegura
 * que cuando el contenido cambia (por ejemplo, al volver del editor)
 * el state se vuelve a poblar.
 */
@Composable
private fun MarkdownBody(
    markdown: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
) {
    val state = rememberRichTextState()
    androidx.compose.runtime.LaunchedEffect(markdown) {
        state.setMarkdown(markdown)
    }
    RichText(
        state = state,
        style = style,
        color = color,
    )
}

/**
 * Multiplica el [TextStyle.fontSize] del estilo recibido por [scale].
 * Si el style no tiene fontSize especificado, devuelve el style intacto.
 */
private fun androidx.compose.ui.text.TextStyle.scaled(scale: Float): androidx.compose.ui.text.TextStyle {
    if (scale == 1.0f) return this
    val fs = this.fontSize
    return if (fs == androidx.compose.ui.unit.TextUnit.Unspecified) this
    else this.copy(fontSize = fs * scale)
}

/**
 * Popup compacto para subir/bajar la escala global de fuente del visor.
 *
 * Aparece como un [DropdownMenu] anclado al icono "FormatSize" de la
 * top bar. Tiene tres celdas: A−, porcentaje actual, A+. El cambio se
 * aplica al instante porque [UserPreferences.fontScale] es un State
 * observado desde Compose.
 */
@Composable
private fun FontSizeMenu(
    expanded: Boolean,
    currentScale: Float,
    onScaleChange: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = { onScaleChange((currentScale - 0.1f).coerceAtLeast(0.8f)) }) {
                Text(
                    text = "A",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OreoPalette.OnSurfaceMuted,
                )
            }
            Text(
                text = "${(currentScale * 100).toInt()}%",
                style = MaterialTheme.typography.titleSmall,
                color = OreoPalette.Accent,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .widthIn(min = 48.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(onClick = { onScaleChange((currentScale + 0.1f).coerceAtMost(2.0f)) }) {
                Text(
                    text = "A",
                    style = MaterialTheme.typography.titleLarge,
                    color = OreoPalette.OnSurface,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = "Restablecer",
                style = MaterialTheme.typography.labelMedium,
                color = OreoPalette.AccentSub,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clickable { onScaleChange(1.0f) }
                    .padding(8.dp),
            )
        }
    }
}
