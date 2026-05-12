@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.oreoexperience.notes.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.oreoexperience.notes.data.Discurso
import com.oreoexperience.notes.data.NoteBlock
import com.oreoexperience.notes.data.NoteBlockSerializer
import com.oreoexperience.notes.data.SortBy
import com.oreoexperience.notes.ui.LocalAppContainer
import com.oreoexperience.notes.ui.components.SwipeToDeleteRow
import com.oreoexperience.notes.ui.theme.OreoMotion
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
    onNew: (initialCategoryKey: String?) -> Unit,
    onOpen: (Long) -> Unit,
    onSettings: () -> Unit,
    onOpenServicio: () -> Unit,
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
    val activeTab by vm.activeTabState.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // Lista actualmente visible bajo la tab activa (sin papelera).
    // La usamos como input del exportador PDF.
    val visibleForExport = remember(state.items) { state.items }

    // SAF launcher: el usuario elige dónde guardar el .pdf.
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument("application/pdf"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        container.pdfExportManager.export(
                            discursos = visibleForExport,
                            title = activeTab.label,
                            outUri = uri,
                        )
                    }
                }.onSuccess { pages ->
                    Toast.makeText(
                        ctx,
                        "PDF exportado · $pages páginas",
                        Toast.LENGTH_LONG,
                    ).show()
                }.onFailure { err ->
                    Toast.makeText(
                        ctx,
                        "No se pudo exportar: ${err.message ?: "error"}",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    val onExportPdf: () -> Unit = exporter@{
        if (visibleForExport.isEmpty()) {
            Toast.makeText(
                ctx,
                "No hay notas en \"${activeTab.label}\" para exportar.",
                Toast.LENGTH_SHORT,
            ).show()
            return@exporter
        }
        val safeName = activeTab.label
            .lowercase()
            .replace(" ", "_")
            .replace("/", "-")
        pdfLauncher.launch("oreo_${safeName}.pdf")
    }

    val pinned = remember(state.items) { state.items.filter { it.pinned } }
    val unpinned = remember(state.items) { state.items.filter { !it.pinned } }
    val grouped = remember(unpinned) { groupByMonth(unpinned) }
    var pinnedExpanded by remember { mutableStateOf(true) }

    // FAB con press feedback bouncy + un giro de 12° para que se sienta
    // que el botón "se inclina" como un sello al apretarlo.
    val fabInteraction = remember { MutableInteractionSource() }
    val fabPressed by fabInteraction.collectIsPressedAsState()
    val fabScale by animateFloatAsState(
        targetValue = if (fabPressed) 0.86f else 1f,
        animationSpec = OreoMotion.SpringBouncy(),
        label = "fabPressScale",
    )
    val fabTilt by animateFloatAsState(
        targetValue = if (fabPressed) -12f else 0f,
        animationSpec = OreoMotion.SpringBouncy(),
        label = "fabTilt",
    )

    Scaffold(
        containerColor = OreoPalette.Bg0,
        floatingActionButton = {
            // Halo pulsante detrás del FAB al presionar — efecto "liquid":
            // un anillo Accent que se expande cuando se mantiene
            // presionado, simulando que la tinta va a soltarse.
            val haloScale by animateFloatAsState(
                targetValue = if (fabPressed) 1.55f else 1f,
                animationSpec = OreoMotion.SpringBouncy(),
                label = "fabHaloScale",
            )
            val haloAlpha by animateFloatAsState(
                targetValue = if (fabPressed) 0.35f else 0f,
                animationSpec = tween(180, easing = OreoMotion.EaseOut),
                label = "fabHaloAlpha",
            )
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .scale(haloScale)
                        .alpha(haloAlpha)
                        .background(OreoPalette.Accent, CircleShape),
                )
                FloatingActionButton(
                    onClick = { onNew(activeTab.category?.key) },
                    interactionSource = fabInteraction,
                    shape = CircleShape,
                    containerColor = OreoPalette.Accent,
                    contentColor = Color.White,
                    modifier = Modifier
                        .scale(fabScale)
                        .rotate(fabTilt),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Nueva nota",
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        },
        bottomBar = {
            // navigationBarsPadding() empuja el contador por encima de la
            // barra de gestos de Android (o de la nav bar tradicional).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp, top = 4.dp),
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
        // Liquid Notes: el large title colapsa al scrollear. Trackeamos
        // el primer ítem visible y su offset para derivar un progreso
        // 0..1 que escala/fadea el título.
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        val collapseProgress by remember {
            androidx.compose.runtime.derivedStateOf {
                val first = listState.firstVisibleItemIndex
                val off = listState.firstVisibleItemScrollOffset
                if (first > 0) 1f
                else (off / 220f).coerceIn(0f, 1f)
            }
        }
      Box(
          modifier = Modifier
              .fillMaxSize()
              .padding(padding)
              // Swipe horizontal entre tabs (Todos / Discursos / Consideraciones
              // / General) desde cualquier punto del cuerpo de la pantalla.
              // Usa awaitHorizontalTouchSlopOrCancellation para sólo consumir
              // los eventos cuando el gesto es claramente horizontal — los
              // verticales caen al LazyColumn y siguen scrolleando normal.
              .pointerInput(activeTab) {
                  val swipeThresholdPx = 64.dp.toPx()
                  awaitEachGesture {
                      val down = awaitFirstDown(requireUnconsumed = false)
                      var totalX = 0f
                      val started: PointerInputChange? =
                          awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                              totalX += over
                              change.consume()
                          }
                      if (started != null) {
                          horizontalDrag(started.id) { change ->
                              totalX += change.positionChange().x
                              change.consume()
                          }
                          val tabs = HomeTab.values()
                          val idx = tabs.indexOf(activeTab).coerceAtLeast(0)
                          if (totalX < -swipeThresholdPx && idx < tabs.lastIndex) {
                              vm.setActiveTab(tabs[idx + 1])
                          } else if (totalX > swipeThresholdPx && idx > 0) {
                              vm.setActiveTab(tabs[idx - 1])
                          }
                      }
                  }
              },
      ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(top = 0.dp, bottom = 88.dp),
        ) {
            // Header compacto: título + menú en una fila, buscador justo debajo.
            // El título colapsa al scrollear (alpha + escala), pero el menú
            // y el buscador permanecen siempre accesibles arriba.
            item {
                val titleAlpha = 1f - collapseProgress
                val titleScale = 1f - (collapseProgress * 0.08f)
                val titleRowHeight = ((1f - collapseProgress) * 44f).dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(titleRowHeight)
                            .padding(start = 18.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .alpha(titleAlpha)
                                .scale(titleScale),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            Text(
                                text = "·",
                                color = OreoPalette.AccentSub,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Notas",
                                color = OreoPalette.OnSurface,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        HeaderMenuButton(
                            sortBy = state.sortBy,
                            onSortBy = vm::setSortBy,
                            onSettings = onSettings,
                            onExportPdf = onExportPdf,
                            exportLabel = activeTab.label,
                        )
                    }
                    SearchBar(
                        value = query,
                        onChange = vm::setQuery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                    )
                }
            }

            // Tabs de categorías + acceso a Servicio del Campo.
            item {
                CategoryTabs(
                    activeTab = activeTab,
                    onTab = vm::setActiveTab,
                    onServicio = onOpenServicio,
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
                            enter = fadeIn(tween(160, easing = OreoMotion.EaseOut)) +
                                expandVertically(tween(260, easing = OreoMotion.EaseOut)),
                            exit = fadeOut(tween(120, easing = OreoMotion.EaseInOut)) +
                                shrinkVertically(tween(200, easing = OreoMotion.EaseInOut)),
                        ) {
                            GroupedCard(
                                items = pinned,
                                onOpen = onOpen,
                                onTrash = vm::trashNote,
                                onTogglePin = vm::togglePin,
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
                        )
                    }
                }
            }
        }

        // Liquid Notes: header flotante que aparece cuando el title
        // colapsa. Imita el "frosted glass" de iOS — un fondo Bg0
        // semitransparente con una borde sutil debajo.
        FloatingCollapsedHeader(
            visible = collapseProgress > 0.55f,
            title = activeTab.label,
        )
      }
    }
}

@Composable
private fun FloatingCollapsedHeader(visible: Boolean, title: String) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(220, easing = OreoMotion.EaseOut),
        label = "floatingHeaderAlpha",
    )
    if (alpha < 0.01f) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .alpha(alpha)
            .background(OreoPalette.Bg0.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            color = OreoPalette.OnSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(OreoPalette.Outline.copy(alpha = 0.4f))
                .align(Alignment.BottomCenter),
        )
    }
}

/**
 * Botón "···" inline para el header compacto: contiene los dropdowns de
 * Ordenar, Exportar PDF y Ajustes. Se usa pegado al título en la misma
 * fila para mantener todo el header en la parte superior de la pantalla.
 */
@Composable
private fun HeaderMenuButton(
    sortBy: SortBy,
    onSortBy: (SortBy) -> Unit,
    onSettings: () -> Unit,
    onExportPdf: () -> Unit,
    exportLabel: String,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuOpen = true }) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Ajustes y opciones",
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
                text = { Text("Exportar \"$exportLabel\" a PDF") },
                leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    onExportPdf()
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
                shape = RoundedCornerShape(18.dp),
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
        animationSpec = OreoMotion.SpringCrisp(),
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
        Icon(
            imageVector = Icons.Outlined.ExpandMore,
            contentDescription = if (expanded) "Colapsar" else "Expandir",
            tint = OreoPalette.Accent,
            modifier = Modifier
                .size(22.dp)
                .rotate(rotation),
        )
    }
}

@Composable
private fun GroupedCard(
    items: List<Discurso>,
    onOpen: (Long) -> Unit,
    onTrash: (Discurso) -> Unit,
    onTogglePin: (Discurso) -> Unit,
) {
    // Cada nota es una tarjeta individual redondeada (estilo cards
    // separadas) en lugar de filas unidas dentro de un solo rectángulo.
    // Así el listado deja de leerse "rectangular".
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = 280,
                    easing = LinearOutSlowInEasing,
                ),
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { d ->
            // Cada fila se envuelve en SwipeToDeleteRow para soportar
            // el gesto iOS de borrar deslizando.
            SwipeToDeleteRow(onDelete = { onTrash(d) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            color = OreoPalette.SurfaceCard,
                            shape = RoundedCornerShape(20.dp),
                        ),
                ) {
                    NoteRow(
                        d = d,
                        onClick = { onOpen(d.id) },
                        onTogglePin = { onTogglePin(d) },
                        onTrash = { onTrash(d) },
                    )
                }
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
) {
    val df = remember { SimpleDateFormat("d/MM/yy", Locale("es")) }
    val title = d.title.ifBlank { "Nota nueva" }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val (preview, thumbName) = remember(d.notes, d.pointsJson) { buildPreviewAndThumb(d) }
    var menuOpen by remember { mutableStateOf(false) }

    // Press feedback: scale 0.92 con un highlight overlay que hace
    // muy obvio el toque (sin esto la anim casi no se veía).
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = OreoMotion.SpringBouncy(),
        label = "rowPressScale",
    )
    val pressHighlight by androidx.compose.animation.animateColorAsState(
        targetValue = if (pressed) OreoPalette.Accent.copy(alpha = 0.10f)
        else Color.Transparent,
        animationSpec = tween(durationMillis = 140, easing = OreoMotion.EaseOut),
        label = "rowPressHighlight",
    )

    Row(
        modifier = Modifier
            .scale(pressScale)
            .clip(RoundedCornerShape(20.dp))
            .background(pressHighlight)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = { menuOpen = true },
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
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

/**
 * Tabs estilo "Liquid Notes" — una sola pill Accent **viaja** entre
 * las pestañas usando un spring suave, en vez de aparecer/desaparecer.
 *
 * La fila desliza horizontalmente cuando los chips no entran en
 * pantalla, garantizando que **todas las pestañas** —incluyendo
 * "Servicio del Campo"— sean alcanzables incluso en dispositivos
 * angostos.
 *
 * Implementación: cada chip reporta su (x, width) en píxeles via
 * `onGloballyPositioned`; un Box absoluto detrás de las labels usa
 * `animateDpAsState` sobre esos valores para deslizarse. El pill se
 * dibuja dentro del mismo contenedor scrolleable, así que viaja en
 * conjunto cuando el usuario hace scroll.
 *
 * Servicio del Campo se integra como un chip más (con accent dot)
 * que en vez de filtrar la lista navega a su propia pantalla.
 */
@Composable
private fun CategoryTabs(
    activeTab: HomeTab,
    onTab: (HomeTab) -> Unit,
    onServicio: () -> Unit,
) {
    val tabs = remember { HomeTab.values().toList() }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val scrollState = rememberScrollState()

    // Posiciones (x, w) en dp de cada tab principal. Indexado por enum.
    val tabRects = remember { mutableStateMapOf<HomeTab, Pair<androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp>>() }

    val target = tabRects[activeTab]
    val targetX = target?.first ?: 0.dp
    val targetW = target?.second ?: 0.dp

    val animX by androidx.compose.animation.core.animateDpAsState(
        targetValue = targetX,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.78f,
            stiffness = 340f,
        ),
        label = "tabPillX",
    )
    val animW by androidx.compose.animation.core.animateDpAsState(
        targetValue = targetW,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.78f,
            stiffness = 340f,
        ),
        label = "tabPillW",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(horizontal = 18.dp),
        ) {
            // Pill viajera detrás de los labels — gradiente Accent →
            // AccentSub + halo suave para sensación de profundidad.
            if (targetW > 0.dp) {
                Box(
                    modifier = Modifier
                        .offset(x = animX)
                        .size(width = animW, height = 38.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    OreoPalette.Accent,
                                    OreoPalette.AccentSub,
                                ),
                            ),
                            shape = RoundedCornerShape(20.dp),
                        ),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { tab ->
                    val active = tab == activeTab
                    CategoryTabChip(
                        label = tab.label,
                        icon = tab.icon,
                        active = active,
                        onClick = { onTab(tab) },
                        modifier = Modifier.onGloballyPositioned { coords ->
                            val xDp = with(density) { coords.positionInParent().x.toDp() }
                            val wDp = with(density) { coords.size.width.toDp() }
                            val previous = tabRects[tab]
                            if (previous?.first != xDp || previous.second != wDp) {
                                tabRects[tab] = xDp to wDp
                            }
                        },
                    )
                }
                // Servicio del Campo: chip "shortcut" con accent dot —
                // navega en vez de filtrar la lista.
                ServicioTabChip(onClick = onServicio)
            }
        }
        // Hairline divisor inferior — refuerza la jerarquía visual
        // entre la fila de categorías y la lista de notas.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(horizontal = 18.dp)
                .align(Alignment.BottomCenter)
                .background(OreoPalette.OutlineFaint),
        )
    }
}

/**
 * Tab chip principal con icono + label. El fondo de la pill activa
 * se dibuja por separado (la pill que viaja); este Box solo
 * renderiza contenido + ajusta color del texto según el estado.
 */
@Composable
private fun CategoryTabChip(
    label: String,
    icon: ImageVector?,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = OreoMotion.SpringBouncy(),
        label = "tabPressScale",
    )
    Row(
        modifier = modifier
            .scale(pressScale)
            .height(38.dp)
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onClick,
            )
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) Color.White else OreoPalette.OnSurfaceMuted,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = label,
            color = if (active) Color.White else OreoPalette.OnSurfaceMuted,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

/**
 * Chip de "Servicio del Campo": vive en la misma fila que las
 * categorías pero navega en lugar de filtrar. El accent dot lo
 * distingue como atajo a una pantalla aparte.
 */
@Composable
private fun ServicioTabChip(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = OreoMotion.SpringBouncy(),
        label = "servicioPressScale",
    )
    Row(
        modifier = Modifier
            .scale(pressScale)
            .height(38.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(OreoPalette.SurfaceCard)
            .border(
                width = 1.dp,
                color = OreoPalette.AccentSub.copy(alpha = 0.45f),
                shape = RoundedCornerShape(20.dp),
            )
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onClick,
            )
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Public,
            contentDescription = null,
            tint = OreoPalette.AccentSub,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "Servicio del Campo",
            color = OreoPalette.OnSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(OreoPalette.AccentSub, CircleShape),
        )
    }
}
