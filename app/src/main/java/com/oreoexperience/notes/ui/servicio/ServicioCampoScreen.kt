@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.oreoexperience.notes.ui.servicio

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import android.widget.Toast
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.oreoexperience.notes.data.RegistroCampo
import com.oreoexperience.notes.data.RegistroCampoRepository
import com.oreoexperience.notes.ui.LocalAppContainer
import com.oreoexperience.notes.ui.LocalOreoWindowSizeClass
import com.oreoexperience.notes.ui.components.EdgePanel
import com.oreoexperience.notes.ui.theme.OreoDuration
import com.oreoexperience.notes.ui.theme.OreoElevation
import com.oreoexperience.notes.ui.theme.OreoMotion
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ─── Staggered entry animation for LazyColumn items ──────────
@Composable
private fun StaggeredEntry(index: Int, content: @Composable () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val offset = remember { Animatable(12f) }
    LaunchedEffect(Unit) {
        delay(index * 60L)
        launch {
            alpha.animateTo(1f, tween(320, easing = OreoMotion.EaseOut))
        }
        launch {
            offset.animateTo(0f, OreoMotion.SpringNavElegant())
        }
    }
    Box(
        Modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = offset.value
        },
    ) {
        content()
    }
}

@Composable
fun ServicioCampoScreen(onBack: () -> Unit, onNavigateTo: (String) -> Unit = {}) {
    val container = LocalAppContainer.current
    val vm: ServicioCampoViewModel = viewModel(factory = viewModelFactory { initializer { ServicioCampoViewModel(container.registroCampoRepository) } })
    val state by vm.state.collectAsStateWithLifecycle()
    var showEditor by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val windowSize = LocalOreoWindowSizeClass.current
    val isTablet = !windowSize.isCompact
    val isToday = remember(state.month) {
        val today = MonthRef.today()
        state.month.year == today.year && state.month.month == today.month
    }

    // Goal configurable
    var goalHours by remember { mutableFloatStateOf(loadGoalHours(ctx)) }
    var showGoalDialog by remember { mutableStateOf(false) }

    val monthStartMillis = remember(state.month) {
        Calendar.getInstance().apply { clear(); set(state.month.year, state.month.month, 1, 0, 0, 0) }.timeInMillis
    }
    val monthLabel = remember(state.month) {
        SimpleDateFormat("LLLL_yyyy", Locale("es", "ES")).format(Date(monthStartMillis)).lowercase(Locale("es", "ES"))
    }

    val pdfLauncher = rememberLauncherForActivityResult(CreateDocument("application/pdf")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { container.pdfExportManager.exportRegistrosMensual(state.records, monthStartMillis, uri) }
            }.onSuccess { pages -> Toast.makeText(ctx, "PDF exportado · $pages página${if (pages > 1) "s" else ""}", Toast.LENGTH_LONG).show() }
                .onFailure { err -> Toast.makeText(ctx, "No se pudo exportar: ${err.message ?: "error"}", Toast.LENGTH_LONG).show() }
        }
    }

    val onExportPdf: () -> Unit = if (state.records.isEmpty()) {
        { Toast.makeText(ctx, "Este mes no tiene registros para exportar.", Toast.LENGTH_SHORT).show() }
    } else {
        { pdfLauncher.launch("servicio_campo_${monthLabel}.pdf") }
    }

    Box(Modifier.fillMaxSize().background(OreoPalette.Bg0)) {
        Scaffold(containerColor = Color.Transparent) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).imePadding()) {
                LazyColumn(
                    Modifier.fillMaxWidth().widthIn(max = if (isTablet) 480.dp else 600.dp),
                    contentPadding = PaddingValues(bottom = 120.dp),
                ) {
                    item { StaggeredEntry(0) { TopBar(onBack = onBack) } }
                    item { StaggeredEntry(1) { ServicioHeader() } }

                    // Calendario
                    item {
                        StaggeredEntry(2) {
                            CalendarCard(
                            month = state.month,
                            records = state.records,
                            selectedDayMillis = state.selectedDayMillis,
                            isToday = isToday,
                            onPrev = vm::previousMonth,
                            onNext = vm::nextMonth,
                            onGoToday = vm::goToCurrentMonth,
                            onSelectDay = vm::selectDay,
                            )
                        }
                    }

                    // Resumen
                    item {
                        StaggeredEntry(3) {
                            MonthSummaryCard(
                                state = state,
                                onExportPdf = onExportPdf,
                                goalHours = goalHours,
                                onGoalChange = { newGoal ->
                                    goalHours = newGoal
                                    saveGoalHours(ctx, newGoal)
                                },
                                showGoalDialog = showGoalDialog,
                                onShowGoalDialogChange = { showGoalDialog = it },
                            )
                        }
                    }
                }

                if (showEditor) {
                    val existing = vm.recordForDay(state.selectedDayMillis)
                    RegistroEditorDialog(
                        initialDayMillis = state.selectedDayMillis,
                        existing = existing,
                        onDismiss = { showEditor = false },
                        onSave = { rec -> vm.upsert(rec); showEditor = false },
                        onDelete = existing?.let { rec -> { vm.delete(rec); showEditor = false } },
                    )
                }
            }
        }

        // Edge Panel con lápiz
        EdgePanel(
            onNewNote = { showEditor = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 0.dp, bottom = 80.dp),
        )
    }
}

// ─── TopBar ────────────────────────────────────────────────────
@Composable
private fun TopBar(onBack: () -> Unit) {
    val i = remember { MutableInteractionSource() }
    val p by i.collectIsPressedAsState()
    val s by animateFloatAsState(if (p) 0.92f else 1f, OreoMotion.SpringPress(), label = "tb")
    val isTablet = !LocalOreoWindowSizeClass.current.isCompact

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 24.dp else 16.dp, vertical = 8.dp)
            .padding(top = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .scale(s)
                .clip(RoundedCornerShape(14.dp))
                .clickable(interactionSource = i, indication = null, onClick = onBack)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.ArrowBackIosNew, "Volver", tint = OreoPalette.AccentSub, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("Volver", color = OreoPalette.AccentSub, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ─── Header: icono con glow + título limpio ────────────────────
@Composable
private fun ServicioHeader() {
    val t = rememberInfiniteTransition(label = "sh")
    val isTablet = !LocalOreoWindowSizeClass.current.isCompact
    val glowAlpha by t.animateFloat(0.08f, 0.18f, infiniteRepeatable(tween(2500, easing = EaseInOut), RepeatMode.Reverse), label = "glow")
    val iconScale by t.animateFloat(1f, 1.04f, infiniteRepeatable(tween(2000, easing = EaseInOut), RepeatMode.Reverse), label = "iconScale")

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 24.dp else 20.dp, vertical = 8.dp)
            .padding(top = 12.dp, bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icono con glow difuso
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(if (isTablet) 64.dp else 56.dp)
                    .blur(24.dp)
                    .background(OreoPalette.Accent.copy(alpha = glowAlpha), CircleShape),
            )
            Box(
                Modifier
                    .size(if (isTablet) 56.dp else 48.dp)
                    .scale(iconScale)
                    .shadow(16.dp, RoundedCornerShape(16.dp), ambientColor = OreoPalette.Accent.copy(alpha = 0.25f))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(OreoPalette.Accent.copy(alpha = 0.15f), OreoPalette.AccentDeep.copy(alpha = 0.08f))))
                    .border(0.5.dp, OreoPalette.OutlineFaint, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Grass,
                    "Servicio del Campo",
                    tint = OreoPalette.AccentSub,
                    modifier = Modifier.size(if (isTablet) 28.dp else 24.dp),
                )
            }
        }
        Spacer(Modifier.size(16.dp))
        Column {
            Text(
                "Servicio del Campo",
                fontSize = if (isTablet) 28.sp else 24.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(brush = Brush.linearGradient(listOf(OreoPalette.AccentLight, OreoPalette.Accent, OreoPalette.SemanticBlue))),
                letterSpacing = (-0.02).sp,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                "Predicación · revisitas · cursos",
                color = OreoPalette.OnSurfaceMuted,
                fontSize = if (isTablet) 14.sp else 13.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}

// ─── Section Header ────────────────────────────────────────────
@Composable
private fun SectionHeader(title: String, count: Int = 0) {
    val isTablet = !LocalOreoWindowSizeClass.current.isCompact
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 24.dp else 18.dp)
            .padding(top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(OreoPalette.Accent),
        )
        Spacer(Modifier.size(10.dp))
        Text(
            title,
            style = TextStyle(brush = Brush.linearGradient(listOf(OreoPalette.AccentLight, OreoPalette.Accent))),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (count > 0) {
            Spacer(Modifier.size(8.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(OreoPalette.Accent.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    count.toString(),
                    color = OreoPalette.Accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ─── Calendario rediseñado ─────────────────────────────────────
@Composable
private fun CalendarCard(
    month: MonthRef,
    records: List<RegistroCampo>,
    selectedDayMillis: Long,
    isToday: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onGoToday: () -> Unit,
    onSelectDay: (Long) -> Unit,
) {
    val today = remember { RegistroCampoRepository.startOfDayMillis(System.currentTimeMillis()) }
    val recordedDays = remember(records) { records.map { it.dateMillis }.toHashSet() }
    val dayHoursMap = remember(records) { buildDayHoursMap(records) }
    val maxHours = remember(dayHoursMap) { dayHoursMap.values.maxOrNull() ?: 1.0 }
    val monthName = remember(month) {
        Calendar.getInstance().apply { clear(); set(month.year, month.month, 1) }
            .let { SimpleDateFormat("MMMM", Locale("es", "ES")).format(it.time).replaceFirstChar { c -> c.uppercase() } }
    }
    val yearStr = remember(month) { month.year.toString() }
    val isTablet = !LocalOreoWindowSizeClass.current.isCompact

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 20.dp else 14.dp)
            .shadow(OreoElevation.Low, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.1f))
            .clip(RoundedCornerShape(24.dp))
            .background(OreoPalette.SurfaceCard)
            .border(0.5.dp, OreoPalette.OutlineFaint, RoundedCornerShape(24.dp))
            .padding(16.dp),
    ) {
        // Navegación del mes
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CalNavArrow(onClick = onPrev, rotation = 0f)
            Spacer(Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(OreoPalette.Accent.copy(alpha = 0.08f))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(monthName, color = OreoPalette.Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(OreoPalette.SurfaceCardHi)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text(yearStr, color = OreoPalette.OnSurfaceFaint, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.weight(1f))
            CalNavArrow(onClick = onNext, rotation = 180f)
        }

        // Botón "Hoy" cuando no estamos en el mes actual
        if (!isToday) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(OreoPalette.Accent.copy(alpha = 0.08f))
                        .clickable { onGoToday() }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Outlined.CalendarMonth, null, tint = OreoPalette.Accent, modifier = Modifier.size(14.dp))
                    Text("Hoy", color = OreoPalette.Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Días de la semana
        Row(Modifier.fillMaxWidth()) {
            listOf("Lu", "Ma", "Mi", "Ju", "Vi", "Sa", "Do").forEach { d ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(d, color = OreoPalette.TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Grid del calendario con transición animada
        AnimatedContent(
            targetState = month,
            transitionSpec = {
                fadeIn(tween(OreoDuration.FAST, easing = OreoMotion.EaseOut)) togetherWith
                    fadeOut(tween(OreoDuration.FAST, easing = OreoMotion.EaseOut))
            },
            label = "calMonth",
        ) { m ->
            val cells = remember(m) { buildMonthCells(m.year, m.month) }
            Column {
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        week.forEach { cell ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (cell != null) {
                                    val isSel = cell.first == selectedDayMillis
                                    val isTod = cell.first == today
                                    val hasRecord = cell.first in recordedDays
                                    val isCurrentMonth = cell.second in 1..31
                                    val dayHours = dayHoursMap[cell.first] ?: 0.0
                                    val heatLevel = if (hasRecord && isCurrentMonth) calculateHeatLevel(dayHours) else 0

                                    val bg by animateColorAsState(
                                        when {
                                            isSel -> OreoPalette.Accent
                                            isTod && heatLevel == 0 -> OreoPalette.Accent.copy(alpha = 0.15f)
                                            heatLevel == 1 -> OreoPalette.Accent.copy(alpha = 0.10f)
                                            heatLevel == 2 -> OreoPalette.Accent.copy(alpha = 0.18f)
                                            heatLevel == 3 -> OreoPalette.Accent.copy(alpha = 0.28f)
                                            heatLevel == 4 -> OreoPalette.Accent.copy(alpha = 0.40f)
                                            else -> Color.Transparent
                                        },
                                        tween(OreoDuration.FAST, easing = OreoMotion.EaseOut),
                                        label = "dayBg",
                                    )
                                    val tc by animateColorAsState(
                                        when {
                                            isSel -> Color.White
                                            isTod -> OreoPalette.Accent
                                            !isCurrentMonth -> OreoPalette.TextTertiary.copy(alpha = 0.3f)
                                            heatLevel == 0 -> OreoPalette.OnSurfaceFaint
                                            heatLevel == 1 -> OreoPalette.OnSurfaceMuted
                                            heatLevel == 2 -> OreoPalette.AccentSub
                                            heatLevel == 3 -> OreoPalette.OnSurface
                                            heatLevel == 4 -> OreoPalette.AccentLight
                                            else -> OreoPalette.OnSurfaceFaint
                                        },
                                        tween(OreoDuration.FAST, easing = OreoMotion.EaseOut),
                                        label = "dayTc",
                                    )

                                    val i = remember { MutableInteractionSource() }
                                    val p by i.collectIsPressedAsState()
                                    val s by animateFloatAsState(if (p) 0.88f else 1f, OreoMotion.SpringPress(), label = "ds")

                                    Column(
                                        Modifier
                                            .fillMaxSize()
                                            .scale(s)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(bg)
                                            .clickable(interactionSource = i, indication = null, onClick = { onSelectDay(cell.first) }),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        // Día con checkmark
                                        if (hasRecord && isCurrentMonth) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    cell.second.toString(),
                                                    color = if (isSel) Color.White else tc,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textDecoration = if (!isSel) TextDecoration.LineThrough else null,
                                                )
                                                // Checkmark sutil arriba a la derecha
                                                if (!isSel) {
                                                    Box(
                                                        Modifier
                                                            .align(Alignment.TopEnd)
                                                            .offset(x = 1.dp, y = (-1).dp)
                                                    ) {
                                                        Text("✓", color = Color(0xFF22C55E), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        } else {
                                            Text(
                                                cell.second.toString(),
                                                color = if (isSel) Color.White else tc,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSel || isTod) FontWeight.Bold else FontWeight.Normal,
                                            )
                                        }
                                        // Horas debajo del día
                                        if (hasRecord && isCurrentMonth && !isSel) {
                                            Text(
                                                "%.1fh".format(dayHours),
                                                color = tc.copy(alpha = 0.7f),
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                lineHeight = 8.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                }
            }
        }
    }
}

// ─── Flecha de navegación con rotación ─────────────────────────
@Composable
private fun CalNavArrow(onClick: () -> Unit, rotation: Float) {
    val i = remember { MutableInteractionSource() }
    val p by i.collectIsPressedAsState()
    val s by animateFloatAsState(if (p) 0.85f else 1f, OreoMotion.SpringPress(), label = "calNav")

    Box(
        Modifier
            .size(36.dp)
            .scale(s)
            .clip(CircleShape)
            .background(OreoPalette.Accent.copy(alpha = 0.08f))
            .border(0.5.dp, OreoPalette.Accent.copy(alpha = 0.15f), CircleShape)
            .clickable(interactionSource = i, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.ArrowBackIosNew,
            null,
            tint = OreoPalette.AccentSub,
            modifier = Modifier
                .size(14.dp)
                .rotate(rotation),
        )
    }
}

// ─── Resumen del mes: diseño #1 profesional ────────────────────
@Composable
private fun MonthSummaryCard(
    state: ServicioCampoUiState,
    onExportPdf: () -> Unit,
    goalHours: Float,
    onGoalChange: (Float) -> Unit,
    showGoalDialog: Boolean,
    onShowGoalDialogChange: (Boolean) -> Unit,
) {
    val isTablet = !LocalOreoWindowSizeClass.current.isCompact

    val h by animateFloatAsState(state.totals.totalHours.toFloat(), tween(800, easing = OreoMotion.EaseOut), label = "h")
    val rev by animateFloatAsState(state.totals.totalRevisits.toFloat(), tween(800, easing = OreoMotion.EaseOut), label = "rev")
    val stu by animateFloatAsState(state.totals.totalStudies.toFloat(), tween(800, easing = OreoMotion.EaseOut), label = "stu")

    // Sparkline: últimos 14 días
    val sparklineData = remember(state.records) {
        val today = RegistroCampoRepository.startOfDayMillis(System.currentTimeMillis())
        val cal = Calendar.getInstance()
        (13 downTo 0).map { daysAgo ->
            cal.timeInMillis = today
            cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
            val dayStart = RegistroCampoRepository.startOfDayMillis(cal.timeInMillis)
            state.records.filter { it.dateMillis == dayStart }.sumOf { it.hours }.toFloat()
        }
    }
    val maxSparkline = remember(sparklineData) { sparklineData.maxOrNull()?.coerceAtLeast(1f) ?: 1f }

    // % vs mes anterior
    val vsLastMonth = remember(state.chartData) {
        val data = state.chartData
        if (data.size >= 2) {
            val current = data.last().totalHours
            val prev = data[data.size - 2].totalHours
            if (prev > 0) ((current - prev) / prev * 100).toInt() else null
        } else null
    }

    // Goal progress
    val goalPct = if (goalHours > 0) (h / goalHours).coerceIn(0f, 1f) else 0f
    val animatedGoalPct by animateFloatAsState(goalPct, tween(800, easing = OreoMotion.EaseOut), label = "goalPct")

    // Promedio diario
    val avgDaily = if (state.totals.totalDays > 0) h / state.totals.totalDays else 0f

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 20.dp else 14.dp)
            .padding(top = 8.dp)
            .shadow(OreoElevation.Medium, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.08f))
            .clip(RoundedCornerShape(24.dp))
            .background(OreoPalette.SurfaceCard)
            .border(0.5.dp, OreoPalette.OutlineFaint, RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        // Title + Badge
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Resumen del mes", color = OreoPalette.OnSurfaceMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            if (vsLastMonth != null) {
                val isPositive = vsLastMonth >= 0
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isPositive) Color(0xFF22C55E).copy(alpha = 0.1f) else Color(0xFFEF4444).copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        "${if (isPositive) "+" else ""}$vsLastMonth%",
                        color = if (isPositive) Color(0xFF22C55E) else Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Horas grandes
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "%.1f".format(Locale("es", "ES"), h.toDouble()),
                style = TextStyle(brush = Brush.linearGradient(listOf(OreoPalette.AccentLight, OreoPalette.Accent))),
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 52.sp,
                letterSpacing = (-2).sp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "horas",
                color = OreoPalette.OnSurfaceMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }

        Spacer(Modifier.height(14.dp))

        // Goal Progress Bar (plain, clickable)
        Column(
            Modifier
                .fillMaxWidth()
                .clickable { onShowGoalDialogChange(true) },
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Meta: ${goalHours.toInt()}h",
                    color = OreoPalette.OnSurfaceMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "%.0f%%".format(animatedGoalPct * 100),
                    color = OreoPalette.AccentSub,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(OreoPalette.Accent.copy(alpha = 0.08f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(animatedGoalPct)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Brush.linearGradient(listOf(OreoPalette.Accent, OreoPalette.AccentSub)))
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Sparkline (últimos 14 días)
        Row(
            Modifier
                .fillMaxWidth()
                .height(32.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            sparklineData.forEachIndexed { idx, hours ->
                val isToday = idx == 13
                val normalizedHeight = if (maxSparkline > 0) (hours / maxSparkline) else 0f
                val barHeight = if (hours > 0f) (normalizedHeight * 32).dp.coerceAtLeast(3.dp) else 3.dp

                Box(
                    Modifier
                        .weight(1f)
                        .height(barHeight)
                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                        .background(
                            when {
                                isToday -> Brush.verticalGradient(listOf(OreoPalette.AccentSub, OreoPalette.Accent))
                                hours > 0f -> Brush.verticalGradient(listOf(OreoPalette.AccentSub.copy(alpha = 0.5f), OreoPalette.Accent.copy(alpha = 0.25f)))
                                else -> Brush.verticalGradient(listOf(OreoPalette.Accent.copy(alpha = 0.06f), OreoPalette.Accent.copy(alpha = 0.03f)))
                            }
                        ),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Stats row (plain, no background)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SummaryMiniStat(value = "%.0f".format(rev), label = "revisitas", modifier = Modifier.weight(1f))
            SummaryMiniStat(value = "%.0f".format(stu), label = "cursos", modifier = Modifier.weight(1f))
            SummaryMiniStat(value = "%.1fh".format(avgDaily), label = "promedio", modifier = Modifier.weight(1f))
            SummaryMiniStat(value = "${state.totals.totalDays}", label = "días", modifier = Modifier.weight(1f))
        }
    }

    // Goal Dialog
    if (showGoalDialog) {
        GoalDialog(
            currentGoal = goalHours,
            onDismiss = { onShowGoalDialogChange(false) },
            onConfirm = { newGoal ->
                onGoalChange(newGoal)
                onShowGoalDialogChange(false)
            },
        )
    }
}

// ─── Stat mini para el resumen ─────────────────────────────────
@Composable
private fun SummaryMiniStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            color = OreoPalette.OnSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.size(2.dp))
        Text(label, color = OreoPalette.OnSurfaceMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

// ─── Goal Dialog ──────────────────────────────────────────────
@Composable
private fun GoalDialog(
    currentGoal: Float,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit,
) {
    var text by remember { mutableStateOf(currentGoal.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OreoPalette.SurfaceCard,
        title = {
            Text("Meta de horas", color = OreoPalette.OnSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text("Establece tu meta mensual de horas.", color = OreoPalette.OnSurfaceMuted, fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() } },
                    label = { Text("Horas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(OreoPalette.Accent)
                    .clickable {
                        val goal = text.toFloatOrNull() ?: 50f
                        onConfirm(goal.coerceIn(1f, 500f))
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text("Guardar", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(OreoPalette.Accent.copy(alpha = 0.1f))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text("Cancelar", color = OreoPalette.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

// ─── Botón exportar PDF ────────────────────────────────────────
@Composable
private fun PdfExportButton(onClick: () -> Unit) {
    val i = remember { MutableInteractionSource() }
    val p by i.collectIsPressedAsState()
    val s by animateFloatAsState(if (p) 0.97f else 1f, OreoMotion.SpringPress(), label = "pdf")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(s)
            .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = OreoPalette.Accent.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(OreoPalette.Accent, OreoPalette.AccentDeep)))
            .clickable(interactionSource = i, indication = null, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.PictureAsPdf, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.size(10.dp))
            Text("Exportar mes a PDF", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Empty state ───────────────────────────────────────────────
@Composable
private fun EmptyMonth() {
    val isTablet = !LocalOreoWindowSizeClass.current.isCompact
    val t = rememberInfiniteTransition(label = "empty")
    val pulse by t.animateFloat(0.8f, 1f, infiniteRepeatable(tween(2000, easing = EaseInOut), RepeatMode.Reverse), label = "pulse")

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 20.dp else 14.dp)
            .padding(vertical = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(OreoPalette.SurfaceCard)
            .border(0.5.dp, OreoPalette.OutlineFaint, RoundedCornerShape(24.dp))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .scale(pulse)
                .clip(CircleShape)
                .background(OreoPalette.Accent.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.EventBusy,
                null,
                tint = OreoPalette.Accent.copy(alpha = 0.4f),
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("Sin registros este mes", style = TextStyle(brush = Brush.linearGradient(listOf(OreoPalette.AccentLight, OreoPalette.Accent))), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("Toca + para añadir tu primer registro.", color = OreoPalette.OnSurfaceMuted, fontSize = 13.sp)
    }
}

// ─── Fila de registro ──────────────────────────────────────────
@Composable
private fun RegistroRow(record: RegistroCampo, selected: Boolean, onClick: () -> Unit) {
    val i = remember { MutableInteractionSource() }
    val p by i.collectIsPressedAsState()
    val s by animateFloatAsState(if (p) 0.98f else 1f, OreoMotion.SpringPress(), label = "rr")
    val bg by animateColorAsState(
        if (selected) OreoPalette.Accent.copy(alpha = 0.08f) else Color.Transparent,
        tween(OreoDuration.FAST, easing = OreoMotion.EaseOut),
        label = "rb",
    )
    val isTablet = !LocalOreoWindowSizeClass.current.isCompact

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 20.dp else 14.dp, vertical = 4.dp)
            .scale(s)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(
                0.5.dp,
                if (selected) OreoPalette.Accent.copy(alpha = 0.15f) else OreoPalette.OutlineFaint,
                RoundedCornerShape(18.dp),
            )
            .clickable(interactionSource = i, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DayBadge(record.dateMillis)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "%.1f h".format(Locale("es", "ES"), record.hours),
                    style = TextStyle(brush = Brush.linearGradient(listOf(OreoPalette.AccentLight, OreoPalette.Accent))),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (record.revisits > 0 || record.studies > 0) {
                    Spacer(Modifier.size(10.dp))
                    if (record.revisits > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Outlined.Groups, null, tint = OreoPalette.AccentSub, modifier = Modifier.size(13.dp))
                            Text(record.revisits.toString(), color = OreoPalette.AccentSub, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.size(8.dp))
                    }
                    if (record.studies > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.AutoMirrored.Outlined.MenuBook, null, tint = OreoPalette.AccentSub, modifier = Modifier.size(13.dp))
                            Text(record.studies.toString(), color = OreoPalette.AccentSub, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            if (record.notes.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    record.notes,
                    color = OreoPalette.OnSurfaceMuted,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ─── Badge del día ─────────────────────────────────────────────
@Composable
private fun DayBadge(dayMillis: Long) {
    val cal = remember(dayMillis) { Calendar.getInstance().apply { timeInMillis = dayMillis } }
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val mName = remember(dayMillis) {
        SimpleDateFormat("MMM", Locale("es", "ES")).format(Date(dayMillis)).uppercase().trimEnd('.')
    }

    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        OreoPalette.Accent.copy(alpha = 0.08f),
                        OreoPalette.Accent.copy(alpha = 0.02f),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(mName, color = OreoPalette.AccentSub, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Text(day.toString(), style = TextStyle(brush = Brush.linearGradient(listOf(OreoPalette.AccentLight, OreoPalette.Accent))), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Build month cells ─────────────────────────────────────────
private fun buildMonthCells(year: Int, month: Int): List<Pair<Long, Int>?> {
    val cal = Calendar.getInstance().apply { clear(); set(year, month, 1, 0, 0, 0) }
    val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val leading = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val cells = mutableListOf<Pair<Long, Int>?>()
    repeat(leading) { cells.add(null) }
    for (d in 1..days) {
        val c = Calendar.getInstance().apply { clear(); set(year, month, d, 0, 0, 0) }
        cells.add(c.timeInMillis to d)
    }
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}

// ─── Heatmap helpers ──────────────────────────────────────────
private fun buildDayHoursMap(records: List<RegistroCampo>): Map<Long, Double> =
    records.associate { it.dateMillis to it.hours }

private fun calculateHeatLevel(hours: Double): Int = when {
    hours <= 0.0 -> 0
    hours < 2.0  -> 1
    hours < 3.0  -> 2
    hours < 4.0  -> 3
    else         -> 4
}

// ─── SharedPreferences helpers ─────────────────────────────────
private const val PREFS_NAME = "servicio_campo_prefs"
private const val KEY_GOAL_HOURS = "meta_horas"

private fun loadGoalHours(context: Context): Float {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getFloat(KEY_GOAL_HOURS, 50f)
}

private fun saveGoalHours(context: Context, goal: Float) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putFloat(KEY_GOAL_HOURS, goal)
        .apply()
}
