@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.oreoexperience.notes.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.oreoexperience.notes.data.Discurso
import com.oreoexperience.notes.data.NoteBlock
import com.oreoexperience.notes.data.NoteBlockSerializer
import com.oreoexperience.notes.data.SortBy
import com.oreoexperience.notes.ui.LocalAppContainer
import com.oreoexperience.notes.ui.components.FrostedTopBar
import com.oreoexperience.notes.ui.components.SwipeToDeleteRow
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Pantalla principal estilo **iOS Notes** con identidad
 * **OreoExperience Aurora**:
 *
 *   - Top bar con menú "..." (Sort By / Settings).
 *   - Large title "OreoExperience · Notas".
 *   - Buscador rounded.
 *   - **Sección Pinned** colapsable arriba (chevron animado).
 *   - Lista plana agrupada por mes ("Mayo", "Abril 2025", etc).
 *   - Cada fila: título + preview + fecha + thumbnail si la nota
 *     incluye una imagen.
 *   - **Long-press** sobre una fila → menú contextual (pin, share,
 *     delete a papelera).
 *   - **Swipe-to-delete**: deslizá una fila a la izquierda para
 *     mandarla a la papelera.
 *   - FAB violeta con ícono de lápiz.
 *   - Footer "X notas" centrado.
 */
@Composable
fun HomeScreen(
    onNew: () -> Unit,
    onOpen: (Long) -> Unit,
    onSettings: () -> Unit,
    onOpenReader: (Long) -> Unit = {},
) {
    val container = LocalAppContainer.current
    val vm: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    repository = container.repository,
                    mediaStorage = container.mediaStorage,
                    userPreferences = container.userPreferences,
                )
            }
        }
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val query by vm.queryState.collectAsStateWithLifecycle()
    val pinned = remember(state.items) { state.items.filter { it.pinned } }
    val unpinned = remember(state.items) { state.items.filter { !it.pinned } }
    // Si el orden es semántico, agrupamos por palabras clave
    // compartidas (sin agrupar por mes). Para el resto se mantiene el
    // grouping clásico por mes/año.
    val grouped = remember(unpinned, state.sortBy) {
        if (state.sortBy == SortBy.SEMANTIC) {
            SemanticGrouper.group(unpinned)
        } else {
            groupByMonth(unpinned)
        }
    }
    var pinnedExpanded by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    // "Scrolled" = el primer item ya no está al tope. Activa el
    // efecto cristal del topbar.
    val scrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > 4
        }
    }

    Scaffold(
        containerColor = OreoPalette.Bg0,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNew,
                shape = CircleShape,
                containerColor = OreoPalette.Accent,
                contentColor = Color.White,
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = "Nueva nota")
            }
        },
        bottomBar = {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 0.dp, bottom = 88.dp),
        ) {
            // Hueco a la altura del topbar cristal para que el primer
            // contenido (Large Title) quede por debajo y el scroll lo
            // pase por debajo del cristal.
            item {
                Spacer(
                    Modifier
                        .statusBarsPadding()
                        .height(48.dp),
                )
            }

            // Large title con gradient violeta sutil en "OreoExperience".
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "OreoExperience",
                        style = LocalTextStyle.current.copy(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    OreoPalette.AccentSub,
                                    OreoPalette.Accent,
                                ),
                            ),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Text(
                        text = "Notas",
                        color = OreoPalette.OnSurface,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
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
                item { EmptyState() }
            } else {
                // Sección Pinned (si hay alguna nota pinned).
                if (pinned.isNotEmpty()) {
                    item(key = "section-pinned") {
                        CollapsibleHeader(
                            label = "Fijadas",
                            expanded = pinnedExpanded,
                            onToggle = { pinnedExpanded = !pinnedExpanded },
                        )
                    }
                    item(key = "group-pinned") {
                        AnimatedVisibility(
                            visible = pinnedExpanded,
                            enter = fadeIn(tween(220)) + expandVertically(tween(240)),
                            exit = fadeOut(tween(180)) + shrinkVertically(tween(220)),
                        ) {
                            GroupedCard(
                                items = pinned,
                                onOpen = onOpen,
                                onTrash = vm::trashNote,
                                onTogglePin = vm::togglePin,
                                onReader = onOpenReader,
                            )
                        }
                    }
                }

                grouped.forEach { (label, items) ->
                    item(key = "section-$label") { SectionHeader(label) }
                    item(key = "group-$label") {
                        GroupedCard(
                            items = items,
                            onOpen = onOpen,
                            onTrash = vm::trashNote,
                            onTogglePin = vm::togglePin,
                            onReader = onOpenReader,
                        )
                    }
                }
            }
        }

        // Topbar cristal — *sticky* superpuesto al LazyColumn. El
        // alpha del fondo crece cuando el primer item ya no está al
        // tope; en Android 12+ se aplica un blur real sobre el
        // contenido del propio bar.
        FrostedTopBar(
            scrolled = scrolled,
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Spacer(Modifier.weight(1f))
            TopBarActions(
                sortBy = state.sortBy,
                onSortBy = vm::setSortBy,
                onSettings = onSettings,
            )
        }
        }
    }
}

@Composable
private fun TopBarActions(
    sortBy: SortBy,
    onSortBy: (SortBy) -> Unit,
    onSettings: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        IconButton(onClick = { menuOpen = true }) {
            Icon(
                imageVector = Icons.Outlined.MoreHoriz,
                contentDescription = "Menú",
                tint = OreoPalette.Accent,
            )
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
        ) {
            DropdownMenuItem(
                text = { Text("Ordenar por…") },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    sortOpen = true
                },
            )
            DropdownMenuItem(
                text = { Text("Ajustes") },
                leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    onSettings()
                },
            )
        }
        DropdownMenu(
            expanded = sortOpen,
            onDismissRequest = { sortOpen = false },
        ) {
            SortBy.values().forEach { option ->
                val mark = if (option == sortBy) "✓ " else "    "
                DropdownMenuItem(
                    text = { Text("$mark${option.label}") },
                    onClick = {
                        sortOpen = false
                        onSortBy(option)
                    },
                )
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
private fun CollapsibleHeader(
    label: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 600f,
        ),
        label = "chevronRotation",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle,
            )
            .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = OreoPalette.OnSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "˅",
            color = OreoPalette.Accent,
            fontSize = 20.sp,
            modifier = Modifier.rotate(rotation),
        )
    }
}

@Composable
private fun GroupedCard(
    items: List<Discurso>,
    onOpen: (Long) -> Unit,
    onTrash: (Discurso) -> Unit,
    onTogglePin: (Discurso) -> Unit,
    onReader: (Long) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .background(
                color = OreoPalette.SurfaceCard,
                shape = RoundedCornerShape(14.dp),
            ),
    ) {
        items.forEachIndexed { index, d ->
            // Cada fila se envuelve en SwipeToDeleteRow para soportar
            // el gesto iOS de borrar deslizando.
            SwipeToDeleteRow(onDelete = { onTrash(d) }) {
                Box(modifier = Modifier.background(OreoPalette.SurfaceCard)) {
                    NoteRow(
                        d = d,
                        onClick = { onOpen(d.id) },
                        onTogglePin = { onTogglePin(d) },
                        onTrash = { onTrash(d) },
                        onReader = { onReader(d.id) },
                    )
                }
            }
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
private fun NoteRow(
    d: Discurso,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onTrash: () -> Unit,
    onReader: () -> Unit = {},
) {
    val df = remember { SimpleDateFormat("d/MM/yy", Locale("es")) }
    val title = d.title.ifBlank { "Nota nueva" }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val (preview, thumbName) = remember(d.notes, d.pointsJson) { buildPreviewAndThumb(d) }
    var menuOpen by remember { mutableStateOf(false) }

    // Press feedback: scale 0.97 con spring critically-damped.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 480f,
        ),
        label = "rowPressScale",
    )

    Row(
        modifier = Modifier
            .scale(pressScale)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = { menuOpen = true },
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (d.pinned) {
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = null,
                        tint = OreoPalette.AccentSub,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.size(4.dp))
                }
                Text(
                    text = title,
                    color = OreoPalette.OnSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
        if (thumbName != null) {
            val container = LocalAppContainer.current
            val file = remember(thumbName) {
                File(container.mediaStorage.dir, thumbName)
            }
            Spacer(Modifier.size(10.dp))
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(OreoPalette.SurfaceCardHi),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(file).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        Box {
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
            ) {
                DropdownMenuItem(
                    text = { Text(if (d.pinned) "Quitar fijado" else "Fijar al tope") },
                    leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onTogglePin()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Modo lectura") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onReader()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Compartir") },
                    leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        scope.launch { shareNoteAsText(ctx, d) }
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            "Eliminar",
                            color = OreoPalette.DangerFill,
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onTrash()
                    },
                )
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

/**
 * Devuelve un par (preview, thumbnailFileName?) construido a partir
 * de los bloques de la nota. El preview es texto plano con los
 * marcadores de media removidos. El thumbnail es el filename del
 * primer bloque de imagen encontrado.
 */
private fun buildPreviewAndThumb(d: Discurso): Pair<String, String?> {
    val blocks = NoteBlockSerializer.decode(d.notes)
    val firstImage = blocks.firstOrNull { it is NoteBlock.Image } as? NoteBlock.Image
    val text = blocks
        .filterIsInstance<NoteBlock.Text>()
        .joinToString(" ") { it.markdown }
        .replace("\n", " ")
        .replace(Regex("[*_`#>~\\[\\]]"), "")
        .replace(Regex("<[^>]+>"), "")
        .trim()
        .take(80)
    return text to firstImage?.fileName
}

/**
 * Comparte el contenido de la nota como texto plano, usando un
 * intent ACTION_SEND. El receptor (Drive, Gmail, WhatsApp, etc.) lo
 * trata como texto.
 */
private fun shareNoteAsText(
    ctx: android.content.Context,
    d: Discurso,
) {
    val title = d.title.ifBlank { "Nota" }
    val blocks = NoteBlockSerializer.decode(d.notes)
    val body = blocks.joinToString("\n\n") { b ->
        when (b) {
            is NoteBlock.Text -> b.markdown
            is NoteBlock.Image -> "[imagen: ${b.fileName}]"
            is NoteBlock.Video -> "[video: ${b.fileName}]"
        }
    }
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_SUBJECT, title)
        putExtra(android.content.Intent.EXTRA_TEXT, "$title\n\n$body")
    }
    ctx.startActivity(
        android.content.Intent.createChooser(intent, "Compartir nota"),
    )
}
