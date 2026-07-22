@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.oreoexperience.notes.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ripple
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.oreoexperience.notes.R
import com.oreoexperience.notes.data.Discurso
import com.oreoexperience.notes.data.NoteBlock
import com.oreoexperience.notes.data.NoteBlockSerializer
import com.oreoexperience.notes.data.NoteCategory
import com.oreoexperience.notes.data.Punto
import com.oreoexperience.notes.data.SortBy
import kotlinx.serialization.json.Json
import com.oreoexperience.notes.ui.LocalAppContainer
import com.oreoexperience.notes.ui.LocalOreoWindowSizeClass
import com.oreoexperience.notes.ui.components.ConfirmDeleteDialog
import com.oreoexperience.notes.ui.components.MainTab
import com.oreoexperience.notes.ui.components.OreoBottomNavBar
import com.oreoexperience.notes.ui.components.SwipeRevealRow
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.graphics.graphicsLayer
import com.oreoexperience.notes.ui.components.OreoContextMenu
import com.oreoexperience.notes.ui.components.ContextMenuItem
import com.oreoexperience.notes.ui.components.AnimatedGradientHeader
import com.oreoexperience.notes.ui.components.MorphingFAB
import com.oreoexperience.notes.ui.components.EmptyStateIllustration
import com.oreoexperience.notes.ui.components.OreoSpinner
import com.oreoexperience.notes.ui.components.pinnedGlow
import com.oreoexperience.notes.ui.components.staggeredEntry
import kotlinx.coroutines.delay
import com.oreoexperience.notes.ui.theme.OreoDuration
import com.oreoexperience.notes.ui.theme.OreoElevation
import com.oreoexperience.notes.ui.theme.OreoMotion
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun formatRelativeDate(timestamp: Long): String {
    val now = Calendar.getInstance()
    val date = Calendar.getInstance().apply { time = Date(timestamp) }
    val df = SimpleDateFormat("d MMM", Locale("es"))

    return when {
        now.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR) -> "Hoy"
        
        now.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) - date.get(Calendar.DAY_OF_YEAR) == 1 -> "Ayer"
        
        else -> df.format(Date(timestamp))
    }
}

@Composable
private fun AnimatedEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyStateIllustration()
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Sin notas aún",
            style = androidx.compose.ui.text.TextStyle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        OreoPalette.AccentSub,
                        OreoPalette.Accent,
                        OreoPalette.AccentLight,
                    ),
                ),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            OreoPalette.Accent.copy(alpha = 0.45f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Tocá el lápiz para crear tu primera\nnota y empezar a organizar tus ideas.",
            color = OreoPalette.OnSurfaceMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
    }
}

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

    val pinned = remember(state.items) { state.items.filter { it.pinned } }
    val unpinned = remember(state.items) { state.items.filter { !it.pinned } }
    val grouped = remember(unpinned) { groupByMonth(unpinned) }
    var pinnedExpanded by remember { mutableStateOf(true) }
    var mainTab by remember { mutableStateOf(MainTab.NOTAS) }
    val snackbarHostState = remember { SnackbarHostState() }
    var revealedCardId by remember { mutableStateOf<Long?>(null) }
    var confirmDeleteNote by remember { mutableStateOf<Discurso?>(null) }
    var anyContextMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = OreoPalette.Bg1,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = OreoPalette.SurfaceOverlay,
                    contentColor = OreoPalette.OnSurface,
                    actionContentColor = OreoPalette.Accent,
                    shape = RoundedCornerShape(16.dp),
                )
            }
        },
        floatingActionButton = {
            if (mainTab == MainTab.NOTAS) {
                MorphingFAB(
                    onClick = { onNew(activeTab.category?.key) },
                    modifier = Modifier.padding(bottom = 20.dp, end = 4.dp),
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        bottomBar = {
            OreoBottomNavBar(
                activeTab = mainTab,
                onTabSelected = { mainTab = it },
                noteCount = state.items.size,
            )
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
      // Pull-to-refresh: cuando el usuario tira para abajo desde arriba
      // de la lista, aparece una galletita Oreo girando. El "refresh" es
      // visual (los datos ya vienen reactivos), pero da la sensación
      // satisfactoria de actualizar.
      val pullState = rememberPullToRefreshState()
      var refreshing by remember { mutableStateOf(false) }
      LaunchedEffect(refreshing) {
          if (refreshing) {
              delay(700)
              refreshing = false
          }
      }
      PullToRefreshBox(
          isRefreshing = refreshing,
          onRefresh = { refreshing = true },
          state = pullState,
          modifier = Modifier
              .fillMaxSize()
              .padding(padding),
          indicator = {
              // Galletita Oreo en lugar del indicador circular estándar.
              // Aparece a medida que se tira hacia abajo y gira cuando se
              // dispara el refresh.
              val fraction = pullState.distanceFraction.coerceIn(0f, 1.4f)
              val alphaFactor = (fraction * 1.2f).coerceIn(0f, 1f)
              Box(
                  modifier = Modifier
                      .align(Alignment.TopCenter)
                      .padding(top = 18.dp)
                      .graphicsLayer {
                          translationY = fraction * 56.dp.toPx()
                          alpha = if (refreshing) 1f else alphaFactor
                      },
              ) {
                  OreoSpinner(
                      size = 36.dp,
                      spinning = refreshing,
                  )
              }
          },
      ) {
      Box(
          modifier = Modifier
              .fillMaxSize()
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
            contentPadding = PaddingValues(top = 0.dp, bottom = 96.dp),
        ) {
            // Header rediseñado: título elegante + buscador + selector de categoría.
            // El título colapsa al scrollear (alpha + escala), pero el buscador
            // y el selector permanecen siempre accesibles.
            item {
                val titleAlpha = 1f - collapseProgress
                val titleScale = 1f - (collapseProgress * 0.08f)
                val titleRowHeight = ((1f - collapseProgress) * 52f).dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 0.dp),
                ) {
                    // Título con gradiente animado
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 4.dp),
                    ) {
                        AnimatedGradientHeader(
                            title = "OreoExperience",
                            subtitle = when (mainTab) {
                                MainTab.NOTAS -> "Notas"
                                MainTab.SERVICIO -> "Servicio"
                                MainTab.AJUSTES -> "Ajustes"
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 22.dp)
                                .alpha(titleAlpha)
                                .scale(titleScale),
                        )
                    }

                    // Buscador con diseño refinado
                    AnimatedVisibility(
                        visible = mainTab == MainTab.NOTAS,
                        enter = fadeIn(tween(OreoDuration.FAST)) + expandVertically(tween(OreoDuration.NORMAL)),
                        exit = fadeOut(tween(OreoDuration.FAST)) + shrinkVertically(tween(OreoDuration.FAST)),
                    ) {
                        Column {
                            SearchBar(
                                value = query,
                                onChange = vm::setQuery,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            )

                            // Selector de categorías
                            CategoryChips(
                                activeTab = activeTab,
                                onTab = vm::setActiveTab,
                                categoryCounts = state.categoryCounts,
                            )
                        }
                    }
                }
            }

            item(key = "tab-content-${mainTab.name}") {
                AnimatedContent(
                    targetState = mainTab,
                    transitionSpec = {
                        fadeIn(tween(OreoDuration.FAST, easing = OreoMotion.EaseOut)) togetherWith
                            fadeOut(tween(OreoDuration.FAST, easing = OreoMotion.EaseOut))
                    },
                    label = "tabContent",
                ) { tab ->
                    when (tab) {
                        MainTab.NOTAS -> {
                            if (state.items.isEmpty()) {
                                AnimatedEmptyState()
                            } else {
                                Column {
                                    if (pinned.isNotEmpty()) {
                                        CollapsibleHeader(
                                            label = "Fijadas",
                                            count = pinned.size,
                                            expanded = pinnedExpanded,
                                            onToggle = { pinnedExpanded = !pinnedExpanded },
                                        )
                                        AnimatedVisibility(
                                            visible = pinnedExpanded,
                                            enter = fadeIn(tween(160, easing = OreoMotion.EaseOut)) +
                                                expandVertically(tween(260, easing = OreoMotion.EaseOut)),
                                            exit = fadeOut(tween(120, easing = OreoMotion.EaseInOut)) +
                                                shrinkVertically(tween(200, easing = OreoMotion.EaseInOut)),
                                        ) {
                                            PinnedHorizontalRow(
                                                items = pinned,
                                                onOpen = onOpen,
                                                onTrash = vm::trashNote,
                                                onTogglePin = vm::togglePin,
                                                onMenuToggle = { anyContextMenuOpen = it },
                                            )
                                        }
                                        if (unpinned.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                                    .height(1.dp)
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            colors = listOf(
                                                                OreoPalette.Accent.copy(alpha = 0.4f),
                                                                Color.Transparent,
                                                            ),
                                                        ),
                                                    ),
                                            )
                                        }
                                    }
                                    var cumulativeIndex = if (pinned.isNotEmpty()) pinned.size else 0
                                    grouped.forEach { (label, items) ->
                                        SectionHeader(label, items.size)
                                        GroupedCard(
                                            items = items,
                                            onOpen = onOpen,
                                            onTrash = vm::trashNote,
                                            onTogglePin = vm::togglePin,
                                            revealedCardId = revealedCardId,
                                            onRevealChanged = { revealedCardId = it },
                                            onRequestDelete = { confirmDeleteNote = it },
                                            onMenuToggle = { anyContextMenuOpen = it },
                                            triggerKey = items,
                                            startIndex = cumulativeIndex,
                                        )
                                        cumulativeIndex += items.size
                                    }
                                }
                            }
                        }
                        MainTab.SERVICIO -> {
                            ServicioInline(
                                onOpenFull = onOpenServicio,
                            )
                        }
                        MainTab.AJUSTES -> {
                            SettingsInline(
                                onOpenFull = onSettings,
                            )
                        }
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

        // Scrim overlay cuando un menú contextual está abierto
        AnimatedVisibility(
            visible = anyContextMenuOpen,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(200)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(8.dp)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { anyContextMenuOpen = false },
                    ),
            )
        }
      }
      } // cierra PullToRefreshBox
    }

    // Diálogo de confirmación de borrado
    confirmDeleteNote?.let { d ->
        ConfirmDeleteDialog(
            discurso = d,
            onDismiss = { confirmDeleteNote = null },
            onConfirm = {
                revealedCardId = null
                confirmDeleteNote = null
                vm.trashNote(d)
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = "\"${d.title.ifBlank { "Nota nueva" }}\" movida a la papelera",
                        actionLabel = "Deshacer",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        vm.restoreNote(d)
                    }
                }
            },
        )
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
            .background(OreoPalette.SurfaceNavBar),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            color = OreoPalette.OnSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        // Bottom border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(OreoPalette.BorderSubtle)
                .align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun SearchBar(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusGlowAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.18f else 0f,
        animationSpec = tween(200, easing = OreoMotion.EaseOut),
        label = "searchFocusGlow",
    )
    val focusBorderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.45f else 0.22f,
        animationSpec = tween(200, easing = OreoMotion.EaseOut),
        label = "searchFocusBorder",
    )
    val searchIconColor by animateColorAsState(
        targetValue = if (isFocused) OreoPalette.Accent else OreoPalette.TextTertiary,
        animationSpec = tween(200, easing = OreoMotion.EaseOut),
        label = "searchIconColor",
    )

    Box(
        modifier = modifier
            .height(46.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.4f))
            .clip(RoundedCornerShape(16.dp))
            .background(OreoPalette.SurfaceOverlay) // rgba(18,16,30,0.94)
            .border(1.dp, OreoPalette.BorderSubtle.copy(alpha = focusBorderAlpha), RoundedCornerShape(16.dp)),
    ) {
        // Glow behind on focus
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(OreoPalette.Accent.copy(alpha = focusGlowAlpha)),
        )
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = searchIconColor,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.size(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    color = OreoPalette.OnSurface,
                    fontSize = 14.sp,
                ),
                cursorBrush = SolidColor(OreoPalette.Accent),
                keyboardOptions = KeyboardOptions(),
                keyboardActions = KeyboardActions(),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                "Buscar notas...",
                                color = OreoPalette.TextTertiary,
                                fontSize = 14.sp,
                            )
                        }
                        inner()
                    }
                },
            )
        }
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

    val notesText = blocks.filterIsInstance<NoteBlock.Text>()
        .map { it.markdown }
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .replace(Regex("<[^>]+>"), "")
        .replace("&oacute;", "ó").replace("&eacute;", "é").replace("&iacute;", "í")
        .replace("&uacute;", "ú").replace("&aacute;", "á").replace("&ntilde;", "ñ")
        .replace("&uuml;", "ü").replace("&amp;", "&").replace("&lt;", "<")
        .replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
        .replace("&nbsp;", " ").replace("&period;", ".").replace("&comma;", ",")
        .replace(Regex("&\\w+;"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

    val pointsText = try {
        val puntos = Json { ignoreUnknownKeys = true; isLenient = true }
            .decodeFromString<List<Punto>>(d.pointsJson)
        puntos.flatMap { listOf(it.text, it.body) }
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .replace(Regex("<[^>]+>"), "")
            .replace("&oacute;", "ó").replace("&eacute;", "é").replace("&iacute;", "í")
            .replace("&uacute;", "ú").replace("&aacute;", "á").replace("&ntilde;", "ñ")
            .replace("&uuml;", "ü").replace("&amp;", "&").replace("&lt;", "<")
            .replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
            .replace("&nbsp;", " ").replace("&period;", ".").replace("&comma;", ",")
            .replace(Regex("&\\w+;"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    } catch (_: Exception) { "" }

    val allText = listOf(pointsText, notesText)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .take(400)

    return allText to firstImage?.fileName
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
            is NoteBlock.Text -> b.markdown.replace(Regex("<[^>]*>|<[^>]+"), "")
            is NoteBlock.Image -> "[imagen: ${b.fileName}]"
            is NoteBlock.Video -> "[video: ${b.fileName}]"
            is NoteBlock.Checklist -> b.items.joinToString("\n") { item ->
                val box = if (item.checked) "[x]" else "[ ]"
                "$box ${item.text}"
            }
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
 * Chips horizontales scrolleables para filtrar por categoría.
 * Usa LazyRow para un desplazamiento fluido y natural.
 * Incluye auto-scroll al chip activo y fade edges en los bordes.
 */
@Composable
private fun CategoryChips(
    activeTab: HomeTab,
    onTab: (HomeTab) -> Unit,
    categoryCounts: Map<String, Int>,
) {
    val listState = rememberLazyListState()
    val oreoWc = LocalOreoWindowSizeClass.current
    val isTablet = !oreoWc.isCompact

    LaunchedEffect(activeTab) {
        listState.animateScrollToItem(activeTab.ordinal)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isTablet) Alignment.Center else Alignment.CenterStart,
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier.widthIn(max = if (isTablet) 500.dp else 9999.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(HomeTab.values(), key = { it.ordinal }) { tab ->
                val count = if (tab.category == null) {
                    categoryCounts.values.sum()
                } else {
                    categoryCounts[tab.category.key] ?: 0
                }
                CategoryChip(
                    tab = tab,
                    active = tab == activeTab,
                    count = count,
                    onClick = { onTab(tab) },
                )
            }
            item { Spacer(Modifier.width(8.dp)) }
        }

        // Fade edges
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(16.dp)
                .height(40.dp)
                .background(Brush.horizontalGradient(listOf(OreoPalette.Bg1, Color.Transparent))),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(16.dp)
                .height(40.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, OreoPalette.Bg1))),
        )
    }
}

@Composable
private fun CategoryChip(
    tab: HomeTab,
    active: Boolean,
    count: Int,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }

    val chipColor = when (tab.category?.key) {
        NoteCategory.DISCURSO.key -> OreoPalette.CategoryDiscursoso
        NoteCategory.CONSIDERACION.key -> OreoPalette.CategoryConsideracion
        NoteCategory.GENERAL.key -> OreoPalette.CategoryGeneral
        else -> OreoPalette.Accent
    }

    val bgColor by animateColorAsState(
        targetValue = if (active) chipColor else OreoPalette.SurfaceChipBadge,
        animationSpec = tween(250, easing = OreoMotion.EaseOut),
        label = "chipBg",
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (active) 0f else 0.4f,
        animationSpec = tween(250),
        label = "chipBorder",
    )
    val iconTint by animateColorAsState(
        targetValue = if (active) Color.White else OreoPalette.OnSurfaceMuted,
        animationSpec = tween(250, easing = OreoMotion.EaseOut),
        label = "chipIconTint",
    )
    val textColor by animateColorAsState(
        targetValue = if (active) Color.White else OreoPalette.OnSurface,
        animationSpec = tween(250, easing = OreoMotion.EaseOut),
        label = "chipText",
    )

    Box(
        modifier = Modifier
            .staggeredEntry(index = tab.ordinal, triggerKey = Unit)
            .widthIn(min = 80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .then(
                if (borderAlpha > 0.01f) {
                    Modifier.border(
                        width = 1.dp,
                        color = OreoPalette.Outline.copy(alpha = borderAlpha),
                        shape = RoundedCornerShape(12.dp),
                    )
                } else Modifier
            )
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .semantics {
                selected = active
                contentDescription = "${tab.label}${if (count > 0) ", $count notas" else ""}"
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = tab.label,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            )
            if (count > 0) {
                Text(
                    text = "$count",
                    color = if (active) Color.White.copy(alpha = 0.9f) else OreoPalette.OnSurfaceMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .background(
                            color = if (active) Color.White.copy(alpha = 0.2f) else OreoPalette.SurfaceChipBadge,
                            shape = CircleShape,
                        )
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String, count: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = OreoPalette.Accent,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.02).sp,
            )
            Box(
                modifier = Modifier
                    .background(
                        OreoPalette.Accent.copy(alpha = 0.12f),
                        RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "$count notas",
                    color = OreoPalette.TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            OreoPalette.Accent.copy(alpha = 0.6f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun CollapsibleHeader(
    label: String,
    count: Int = 0,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        animationSpec = OreoMotion.SpringPress(),
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
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = OreoPalette.OnSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (count > 0) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .background(
                        OreoPalette.Accent.copy(alpha = 0.12f),
                        RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "$count",
                    color = OreoPalette.Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.ExpandMore,
            contentDescription = if (expanded) "Colapsar" else "Expandir",
            tint = OreoPalette.Accent,
            modifier = Modifier
                .size(20.dp)
                .rotate(rotation),
        )
    }
}

@Composable
private fun PinnedHorizontalRow(
    items: List<Discurso>,
    onOpen: (Long) -> Unit,
    onTrash: (Discurso) -> Unit,
    onTogglePin: (Discurso) -> Unit,
    onMenuToggle: (Boolean) -> Unit = {},
) {
    LazyRow(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items, key = { it.id }) { d ->
            PinnedCard(
                d = d,
                onClick = { onOpen(d.id) },
                onTogglePin = { onTogglePin(d) },
                onTrash = { onTrash(d) },
                onMenuToggle = onMenuToggle,
            )
        }
    }
}

@Composable
private fun PinnedCard(
    d: Discurso,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onTrash: () -> Unit,
    onMenuToggle: (Boolean) -> Unit = {},
) {
    val df = remember { SimpleDateFormat("d MMM", Locale("es")) }
    val title = d.title.ifBlank { "Nota nueva" }
    val (preview, thumbName) = remember(d.notes, d.pointsJson) { buildPreviewAndThumb(d) }
    var menuOpen by remember { mutableStateOf(false) }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = OreoMotion.SpringPress(),
        label = "pinnedCardPress",
    )

    val categoryColor = when (d.category) {
        NoteCategory.DISCURSO.key -> OreoPalette.CategoryDiscursoso
        NoteCategory.CONSIDERACION.key -> OreoPalette.CategoryConsideracion
        NoteCategory.GENERAL.key -> OreoPalette.CategoryGeneral
        else -> OreoPalette.Accent
    }

    Box(
        modifier = Modifier
            .width(180.dp)
            .scale(pressScale)
            .shadow(OreoElevation.Medium, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.12f))
            .clip(RoundedCornerShape(16.dp))
            .background(OreoPalette.SurfaceOverlay)
            .border(0.5.dp, OreoPalette.OutlineFaint, RoundedCornerShape(16.dp))
            .pinnedGlow()
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    menuOpen = true
                    onMenuToggle(true)
                },
            )
            .padding(14.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .background(categoryColor, CircleShape)
                        .align(Alignment.Top)
                        .padding(top = 5.dp),
                )
                Text(
                    text = title,
                    color = OreoPalette.OnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(6.dp))
            if (preview.isNotBlank()) {
                Text(
                    text = preview,
                    color = OreoPalette.TextTertiary,
                    fontSize = 12.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatRelativeDate(d.updatedAt),
                    color = OreoPalette.TextTertiary,
                    fontSize = 11.sp,
                )
                if (thumbName != null) {
                    val container = LocalAppContainer.current
                    val file = remember(thumbName) {
                        File(container.mediaStorage.dir, thumbName)
                    }
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(file).build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
    }

    Box {
        OreoContextMenu(
            items = listOf(
                ContextMenuItem(
                    icon = Icons.Outlined.PushPin,
                    label = "Quitar fijado",
                    onClick = { onTogglePin() },
                ),
                ContextMenuItem(
                    icon = Icons.Outlined.Delete,
                    label = "Eliminar",
                    isDestructive = true,
                    onClick = { onTrash() },
                ),
            ),
            expanded = menuOpen,
            onDismiss = {
                menuOpen = false
                onMenuToggle(false)
            },
        )
    }
}

@Composable
private fun GroupedCard(
    items: List<Discurso>,
    onOpen: (Long) -> Unit,
    onTrash: (Discurso) -> Unit,
    onTogglePin: (Discurso) -> Unit,
    revealedCardId: Long?,
    onRevealChanged: (Long?) -> Unit,
    onRequestDelete: (Discurso) -> Unit,
    onMenuToggle: (Boolean) -> Unit = {},
    triggerKey: Any? = null,
    startIndex: Int = 0,
) {
    val oreoWc = LocalOreoWindowSizeClass.current
    val columns = when {
        oreoWc.isExpanded -> 3
        else -> 2
    }

    // Grid estilo Google Keep - 2 columnas independientes
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val leftItems = items.filterIndexed { i, _ -> i % columns == 0 }
        val rightItems = items.filterIndexed { i, _ -> i % columns == 1 }
        val extraItems = if (columns >= 3) items.filterIndexed { i, _ -> i % columns == 2 } else emptyList()

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            leftItems.forEachIndexed { colIdx, d ->
                val idx = startIndex + colIdx * columns
                Box(
                    modifier = Modifier
                        .staggeredEntry(index = idx, triggerKey = triggerKey),
                ) {
                    SwipeRevealRow(
                        pinned = d.pinned,
                        cardId = d.id,
                        revealedId = revealedCardId,
                        onRevealChanged = { onRevealChanged(it) },
                        onRequestDelete = { onRequestDelete(d) },
                    ) {
                        NoteCard(
                            d = d,
                            onClick = { onOpen(d.id) },
                            onTogglePin = { onTogglePin(d) },
                            onTrash = { onTrash(d) },
                            onMenuToggle = onMenuToggle,
                        )
                    }
                }
            }
        }
        if (columns >= 2) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rightItems.forEachIndexed { colIdx, d ->
                    val idx = startIndex + colIdx * columns + 1
                    Box(
                        modifier = Modifier
                            .staggeredEntry(index = idx, triggerKey = triggerKey),
                    ) {
                        SwipeRevealRow(
                            pinned = d.pinned,
                            cardId = d.id,
                            revealedId = revealedCardId,
                            onRevealChanged = { onRevealChanged(it) },
                            onRequestDelete = { onRequestDelete(d) },
                        ) {
                            NoteCard(
                                d = d,
                                onClick = { onOpen(d.id) },
                                onTogglePin = { onTogglePin(d) },
                                onTrash = { onTrash(d) },
                                onMenuToggle = onMenuToggle,
                            )
                        }
                    }
                }
            }
        }
        if (columns >= 3) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                extraItems.forEachIndexed { colIdx, d ->
                    val idx = startIndex + colIdx * columns + 2
                    Box(
                        modifier = Modifier
                            .staggeredEntry(index = idx, triggerKey = triggerKey),
                    ) {
                        SwipeRevealRow(
                            pinned = d.pinned,
                            cardId = d.id,
                            revealedId = revealedCardId,
                            onRevealChanged = { onRevealChanged(it) },
                            onRequestDelete = { onRequestDelete(d) },
                        ) {
                            NoteCard(
                                d = d,
                                onClick = { onOpen(d.id) },
                                onTogglePin = { onTogglePin(d) },
                                onTrash = { onTrash(d) },
                                onMenuToggle = onMenuToggle,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteCard(
    d: Discurso,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onTrash: () -> Unit,
    onMenuToggle: (Boolean) -> Unit = {},
) {
    val title = d.title.ifBlank { "Nota nueva" }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val (preview, thumbName) = remember(d.notes, d.pointsJson) { buildPreviewAndThumb(d) }
    var menuOpen by remember { mutableStateOf(false) }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = OreoMotion.SpringPress(),
        label = "cardPressScale",
    )

    val categoryColor = when (d.category) {
        NoteCategory.DISCURSO.key -> OreoPalette.CategoryDiscursoso
        NoteCategory.CONSIDERACION.key -> OreoPalette.CategoryConsideracion
        NoteCategory.GENERAL.key -> OreoPalette.CategoryGeneral
        else -> OreoPalette.Accent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .defaultMinSize(minHeight = 80.dp)
            .scale(pressScale)
            .shadow(OreoElevation.Low, RoundedCornerShape(12.dp), ambientColor = Color.Black.copy(alpha = 0.15f))
            .background(OreoPalette.SurfaceCard, RoundedCornerShape(12.dp))
            .border(0.5.dp, OreoPalette.OutlineFaint, RoundedCornerShape(12.dp))
            .border(1.5.dp, categoryColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    menuOpen = true
                    onMenuToggle(true)
                },
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            // Category Badge + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryBadge(categoryKey = d.category, color = categoryColor)
                Text(
                    text = formatRelativeDate(d.updatedAt),
                    style = LocalTextStyle.current.copy(
                        color = OreoPalette.OnSurfaceFaint,
                        fontSize = 10.sp,
                    ),
                )
            }

            Spacer(Modifier.height(6.dp))

            // Title
            Text(
                text = title,
                style = LocalTextStyle.current.copy(
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(4.dp))

            // Preview - siempre visible
            Text(
                text = preview.ifBlank { "Sin contenido" },
                style = LocalTextStyle.current.copy(
                    color = OreoPalette.OnSurfaceFaint,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                ),
                maxLines = 10,
                overflow = TextOverflow.Ellipsis,
            )

            // Thumbnail
            if (thumbName != null) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    val container = LocalAppContainer.current
                    val file = remember(thumbName) {
                        File(container.mediaStorage.dir, thumbName)
                    }
                    AsyncImage(
                        model = ImageRequest.Builder(ctx).data(file).build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
    }

    Box {
        OreoContextMenu(
            items = listOf(
                ContextMenuItem(
                    icon = Icons.Outlined.PushPin,
                    label = if (d.pinned) "Quitar fijado" else "Fijar al tope",
                    onClick = { onTogglePin() },
                ),
                ContextMenuItem(
                    icon = Icons.Outlined.Share,
                    label = "Compartir",
                    onClick = { scope.launch { shareNoteAsText(ctx, d) } },
                ),
                ContextMenuItem(
                    icon = Icons.Outlined.Delete,
                    label = "Eliminar",
                    isDestructive = true,
                    onClick = { onTrash() },
                ),
            ),
            expanded = menuOpen,
            onDismiss = {
                menuOpen = false
                onMenuToggle(false)
            },
        )
    }
}

@Composable
private fun CategoryBadge(categoryKey: String, color: Color) {
    val pair: Pair<ImageVector, String> = when (categoryKey) {
        NoteCategory.DISCURSO.key -> Icons.Outlined.Description to "Discurso"
        NoteCategory.CONSIDERACION.key -> Icons.Outlined.Check to "Consideración"
        NoteCategory.GENERAL.key -> Icons.Outlined.Edit to "General"
        else -> Icons.Outlined.Description to "Nota"
    }
    val icon = pair.first
    val label = pair.second

    Row(
        modifier = Modifier
            .background(
                color.copy(alpha = 0.18f),
                RoundedCornerShape(8.dp),
            )
            .border(0.5.dp, color.copy(alpha = 0.38f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color.copy(alpha = 0.85f),
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = label,
            color = color.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ─── Inline tab content: Servicio ──────────────────────────────
@Composable
private fun ServicioInline(onOpenFull: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(OreoPalette.Accent.copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Public,
                contentDescription = null,
                tint = OreoPalette.Accent,
                modifier = Modifier.size(36.dp),
            )
        }
        Text(
            text = "Servicio de Campo",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OreoPalette.OnSurface,
        )
        Text(
            text = "Registra y gestiona tus actividades de servicio.",
            fontSize = 14.sp,
            color = OreoPalette.OnSurfaceMuted,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(OreoPalette.Accent.copy(alpha = 0.12f))
                .clickable { onOpenFull() }
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text(
                text = "Abrir Servicio",
                color = OreoPalette.Accent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
    }
}

// ─── Inline tab content: Ajustes ───────────────────────────────
@Composable
private fun SettingsInline(onOpenFull: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(OreoPalette.Accent.copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = null,
                tint = OreoPalette.Accent,
                modifier = Modifier.size(36.dp),
            )
        }
        Text(
            text = "Ajustes",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OreoPalette.OnSurface,
        )
        Text(
            text = "Personaliza la apariencia, exporta PDF y gestiona tu cuenta.",
            fontSize = 14.sp,
            color = OreoPalette.OnSurfaceMuted,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(OreoPalette.Accent.copy(alpha = 0.12f))
                .clickable { onOpenFull() }
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text(
                text = "Abrir Ajustes",
                color = OreoPalette.Accent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
    }
}


