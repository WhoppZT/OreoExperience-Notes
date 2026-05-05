package com.oreoexperience.notes.ui.viewer

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
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
import com.oreoexperience.notes.ui.LocalAppContainer
import com.oreoexperience.notes.ui.components.GlassCard
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
    val points = remember(discurso?.pointsJson) {
        discurso?.let { container.repository.decodePoints(it.pointsJson) } ?: emptyList()
    }

    var menuOpen by remember { mutableStateOf(false) }

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
        ViewerBody(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            discurso = discurso,
            points = points,
        )
    }
}

@Composable
private fun ViewerBody(
    modifier: Modifier,
    discurso: Discurso,
    points: List<Punto>,
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
                        style = MaterialTheme.typography.headlineSmall,
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
                    text = "Bosquejo",
                    style = MaterialTheme.typography.titleSmall,
                    color = OreoPalette.OnSurfaceMuted,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )
            }
            points.forEachIndexed { idx, p ->
                item(key = "vp$idx") {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "${idx + 1}. ${p.text}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = OreoPalette.OnSurface,
                            )
                            p.subpoints.filter { it.isNotBlank() }.forEach { sub ->
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "• $sub",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = OreoPalette.OnSurfaceMuted,
                                    modifier = Modifier.padding(start = 12.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (discurso.notes.isNotBlank()) {
            item {
                Text(
                    text = "Notas",
                    style = MaterialTheme.typography.titleSmall,
                    color = OreoPalette.OnSurfaceMuted,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )
            }
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = discurso.notes,
                        style = MaterialTheme.typography.bodyLarge,
                        color = OreoPalette.OnSurface,
                        modifier = Modifier.padding(16.dp),
                    )
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
