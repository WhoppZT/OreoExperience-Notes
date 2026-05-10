@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.oreoexperience.notes.ui.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.data.Discurso
import com.oreoexperience.notes.data.NoteBlockSerializer
import com.oreoexperience.notes.ui.LocalAppContainer
import com.oreoexperience.notes.ui.components.InlineStatusMessage
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val THIRTY_DAYS_MS: Long = 30L * 24L * 60L * 60L * 1000L

/**
 * **Eliminadas recientemente** (papelera estilo iOS Notes):
 *
 *  - Lista de notas con [Discurso.deletedAt] no nulo, ordenadas por
 *    fecha de borrado descendente.
 *  - Cada fila muestra título + fecha de borrado + cuántos días le
 *    quedan antes de purgarse automáticamente (a los 30 días).
 *  - Tap → abre la nota en modo lectura (volver "restaurar" pinchando
 *    el ícono).
 *  - Acciones: **restaurar** (vuelve a Inicio) y **eliminar
 *    permanentemente** (borra archivo + fila Room).
 *  - Al entrar la pantalla, **purga automáticamente** las notas que
 *    superan los 30 días.
 */
@Composable
fun TrashScreen(
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()

    val items by remember {
        container.repository.observeTrashed()
            .stateIn(scope, SharingStarted.Eagerly, emptyList())
    }.collectAsState()

    var snack by remember { mutableStateOf<String?>(null) }

    // Purga automática al entrar (notas con > 30 días).
    LaunchedEffect(Unit) {
        val cutoff = System.currentTimeMillis() - THIRTY_DAYS_MS
        val purged = container.repository.purgeOlderThan(cutoff)
        purged.forEach { d ->
            val blocks = NoteBlockSerializer.decode(d.notes)
            NoteBlockSerializer.mediaFiles(blocks).forEach { name ->
                container.mediaStorage.deleteIfExists(name)
            }
        }
    }

    Scaffold(containerColor = OreoPalette.Bg0) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item { TopBar(onBack) }
            item {
                Column(modifier = Modifier.padding(start = 18.dp, top = 4.dp, bottom = 16.dp)) {
                    Text(
                        text = "Eliminadas",
                        color = OreoPalette.OnSurface,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Las notas se eliminan automáticamente después de 30 días.",
                        color = OreoPalette.OnSurfaceMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp, end = 18.dp),
                    )
                }
            }

            if (items.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp, start = 18.dp, end = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Sin notas eliminadas",
                            color = OreoPalette.OnSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "La papelera está vacía.",
                            color = OreoPalette.OnSurfaceMuted,
                            fontSize = 14.sp,
                        )
                    }
                }
            } else {
                item {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .background(
                                color = OreoPalette.SurfaceCard,
                                shape = RoundedCornerShape(20.dp),
                            ),
                    ) {
                        items.forEachIndexed { index, d ->
                            TrashRow(
                                d = d,
                                onOpen = { onOpen(d.id) },
                                onRestore = {
                                    scope.launch {
                                        container.repository.restore(d)
                                        snack = "Nota restaurada"
                                    }
                                },
                                onDelete = {
                                    scope.launch {
                                        val blocks = NoteBlockSerializer.decode(d.notes)
                                        NoteBlockSerializer.mediaFiles(blocks).forEach { name ->
                                            container.mediaStorage.deleteIfExists(name)
                                        }
                                        container.repository.delete(d)
                                        snack = "Nota eliminada"
                                    }
                                },
                            )
                            if (index < items.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 18.dp)
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(OreoPalette.OutlineFaint),
                                )
                            }
                        }
                    }
                }
            }

            snack?.let {
                item {
                    InlineStatusMessage(
                        text = it,
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Outlined.ArrowBackIosNew,
                contentDescription = "Volver",
                tint = OreoPalette.Accent,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            "Volver",
            color = OreoPalette.Accent,
            fontSize = 16.sp,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(end = 12.dp),
        )
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun TrashRow(
    d: Discurso,
    onOpen: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    val df = remember { SimpleDateFormat("d/MM/yy", Locale("es")) }
    val daysLeft = remember(d.deletedAt) {
        val deleted = d.deletedAt ?: return@remember 30
        val elapsed = System.currentTimeMillis() - deleted
        val left = ((THIRTY_DAYS_MS - elapsed) / (24L * 60L * 60L * 1000L)).toInt()
        left.coerceAtLeast(0)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                d.title.ifBlank { "Nota nueva" },
                color = OreoPalette.OnSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Eliminada ${df.format(Date(d.deletedAt ?: 0L))} · ${daysLeft}d para purgar",
                color = OreoPalette.OnSurfaceFaint,
                fontSize = 12.sp,
            )
        }
        IconButton(onClick = onRestore) {
            Icon(
                Icons.Outlined.RestoreFromTrash,
                contentDescription = "Restaurar",
                tint = OreoPalette.Accent,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.DeleteForever,
                contentDescription = "Eliminar permanentemente",
                tint = OreoPalette.DangerFill,
            )
        }
    }
}
