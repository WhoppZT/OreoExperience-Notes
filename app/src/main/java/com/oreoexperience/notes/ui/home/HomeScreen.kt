@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.oreoexperience.notes.ui.home

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.oreoexperience.notes.data.Discurso
import com.oreoexperience.notes.ui.LocalAppContainer
import com.oreoexperience.notes.ui.theme.OreoPalette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Pantalla principal estilo **iOS Notes**:
 *
 *   - Fondo negro puro.
 *   - Large title "Notas" arriba (no animado por ahora — sin
 *     CollapsingTopAppBar para mantener simple la implementación).
 *   - Buscador rounded debajo del título.
 *   - Lista plana agrupada por mes ("Mayo 2026", "Abril 2026", etc.).
 *   - Cada fila: título (bold blanco) + preview (gris muted) + fecha.
 *   - FAB amarillo redondo abajo a la derecha con ícono de lápiz.
 *   - Footer "X notas" centrado abajo.
 */
@Composable
fun HomeScreen(
    onNew: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer { HomeViewModel(container.repository) }
        }
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val query by vm.queryState.collectAsStateWithLifecycle()
    val grouped = remember(state.items) { groupByMonth(state.items) }

    Scaffold(
        containerColor = OreoPalette.Bg0,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNew,
                shape = CircleShape,
                containerColor = OreoPalette.Accent,
                contentColor = Color.Black,
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = "Nueva nota")
            }
        },
        bottomBar = {
            // Footer iOS-like "X notas"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = pluralize(state.items.size, "nota", "notas"),
                    color = OreoPalette.OnSurfaceFaint,
                    fontSize = 12.sp,
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(top = 0.dp, bottom = 88.dp),
        ) {
            // Top bar (header con "..." a la derecha)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { /* placeholder menú */ }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreHoriz,
                            contentDescription = null,
                            tint = OreoPalette.Accent,
                        )
                    }
                }
            }

            // Large title
            item {
                Text(
                    text = "Notas",
                    color = OreoPalette.OnSurface,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp),
                )
            }

            // Search bar
            item {
                SearchBar(
                    value = query,
                    onChange = vm::setQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }

            if (state.items.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                grouped.forEach { (label, items) ->
                    item(key = "section-$label") {
                        SectionHeader(label)
                    }
                    item(key = "group-$label") {
                        GroupedCard(items = items, onOpen = onOpen)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                color = OreoPalette.SurfaceCard,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = OreoPalette.OnSurfaceMuted,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    color = OreoPalette.OnSurface,
                    fontSize = 16.sp,
                ),
                cursorBrush = SolidColor(OreoPalette.Accent),
                keyboardOptions = KeyboardOptions(),
                keyboardActions = KeyboardActions(),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                "Buscar",
                                color = OreoPalette.OnSurfaceFaint,
                                fontSize = 16.sp,
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label,
        color = OreoPalette.OnSurface,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 18.dp, top = 18.dp, bottom = 8.dp),
    )
}

@Composable
private fun GroupedCard(items: List<Discurso>, onOpen: (Long) -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .background(
                color = OreoPalette.SurfaceCard,
                shape = RoundedCornerShape(14.dp),
            ),
    ) {
        items.forEachIndexed { index, d ->
            NoteRow(d = d, onClick = { onOpen(d.id) })
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

@Composable
private fun NoteRow(d: Discurso, onClick: () -> Unit) {
    val df = remember { SimpleDateFormat("d/MM/yy", Locale("es")) }
    val title = d.title.ifBlank { "Nota nueva" }
    val preview = remember(d.notes, d.pointsJson) { buildPreview(d) }

    Row(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = OreoPalette.OnSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = df.format(Date(d.updatedAt)),
                    color = OreoPalette.OnSurfaceFaint,
                    fontSize = 13.sp,
                )
                if (preview.isNotBlank()) {
                    Text(
                        text = "  ${preview}",
                        color = OreoPalette.OnSurfaceMuted,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp, start = 18.dp, end = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Sin notas",
            color = OreoPalette.OnSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Tocá el lápiz para crear tu primera nota.",
            color = OreoPalette.OnSurfaceMuted,
            fontSize = 14.sp,
        )
    }
}

private fun pluralize(count: Int, singular: String, plural: String): String =
    if (count == 1) "1 $singular" else "$count $plural"

/**
 * Agrupa las notas por mes/año con label en español ("Mayo 2026", "Abril
 * 2026"). El año actual se omite si todas las notas son del año en curso
 * (replicando el comportamiento de iOS).
 */
private fun groupByMonth(items: List<Discurso>): List<Pair<String, List<Discurso>>> {
    if (items.isEmpty()) return emptyList()
    val sdfFull = SimpleDateFormat("MMMM yyyy", Locale("es"))
    val sdfShort = SimpleDateFormat("MMMM", Locale("es"))
    val cal = Calendar.getInstance()
    val nowYear = cal.get(Calendar.YEAR)

    return items
        .groupBy { d ->
            cal.time = Date(d.updatedAt)
            val y = cal.get(Calendar.YEAR)
            val label = if (y == nowYear) sdfShort.format(cal.time) else sdfFull.format(cal.time)
            label.replaceFirstChar { it.titlecase(Locale("es")) }
        }
        .toList()
}

private fun buildPreview(d: Discurso): String {
    val raw = d.notes.ifBlank { "" }
    return raw
        .replace("\n", " ")
        .replace(Regex("[*_`#>~\\[\\]]"), "")
        .replace(Regex("<[^>]+>"), "")
        .trim()
        .take(80)
}
