@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.oreoexperience.notes.ui.servicio

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.oreoexperience.notes.data.RegistroCampo
import com.oreoexperience.notes.data.RegistroCampoRepository
import com.oreoexperience.notes.ui.LocalAppContainer
import com.oreoexperience.notes.ui.theme.OreoMotion
import com.oreoexperience.notes.ui.theme.OreoPalette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Pantalla **Servicio del Campo** — pensada para el ministerio
 * cristiano (jw.org):
 *
 *   - **Mini calendario** mensual con marcas en los días que ya tienen
 *     registro y pill animada para el día seleccionado.
 *   - **Resumen del mes** con total de horas, revisitas, publicaciones,
 *     videos y cursos.
 *   - **Lista cronológica** de los días registrados.
 *   - **FAB** que abre un editor inline para agregar o actualizar el
 *     registro del día seleccionado.
 */
@Composable
fun ServicioCampoScreen(
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: ServicioCampoViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ServicioCampoViewModel(container.registroCampoRepository) }
        }
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var showEditor by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // Computamos el inicio del mes (00:00 del día 1) a partir del MonthRef
    // visible, para pasárselo al PdfExportManager.
    val monthStartMillis = remember(state.month) {
        Calendar.getInstance().apply {
            clear()
            set(state.month.year, state.month.month, 1, 0, 0, 0)
        }.timeInMillis
    }
    val monthLabel = remember(state.month) {
        val fmt = SimpleDateFormat("LLLL_yyyy", Locale("es", "ES"))
        fmt.format(Date(monthStartMillis)).lowercase(Locale("es", "ES"))
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument("application/pdf"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        container.pdfExportManager.exportRegistrosMensual(
                            registros = state.records,
                            monthStartMillis = monthStartMillis,
                            outUri = uri,
                        )
                    }
                }.onSuccess { pages ->
                    Toast.makeText(
                        ctx,
                        "PDF exportado · $pages página${if (pages > 1) "s" else ""}",
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
        if (state.records.isEmpty()) {
            Toast.makeText(
                ctx,
                "Este mes no tiene registros para exportar.",
                Toast.LENGTH_SHORT,
            ).show()
            return@exporter
        }
        pdfLauncher.launch("servicio_campo_${monthLabel}.pdf")
    }

    Scaffold(
        containerColor = OreoPalette.Bg0,
        floatingActionButton = {
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val pressScale by animateFloatAsState(
                targetValue = if (pressed) 0.88f else 1f,
                animationSpec = OreoMotion.SpringBouncy(),
                label = "addRegistroScale",
            )
            FloatingActionButton(
                onClick = { showEditor = true },
                interactionSource = interaction,
                shape = CircleShape,
                containerColor = OreoPalette.Accent,
                contentColor = Color.White,
                modifier = Modifier.scale(pressScale),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Agregar registro",
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                TopBar(
                    onBack = onBack,
                    onToday = vm::goToCurrentMonth,
                    onExportPdf = onExportPdf,
                )
            }
            item {
                Text(
                    text = "Servicio del Campo",
                    color = OreoPalette.OnSurface,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 18.dp, top = 4.dp, bottom = 4.dp),
                )
            }
            item {
                Text(
                    text = "Predicación · revisitas · cursos",
                    color = OreoPalette.OnSurfaceMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 0.dp),
                )
            }
            item { Spacer(Modifier.height(14.dp)) }
            item {
                MiniCalendar(
                    month = state.month,
                    records = state.records,
                    selectedDayMillis = state.selectedDayMillis,
                    onPrev = vm::previousMonth,
                    onNext = vm::nextMonth,
                    onSelectDay = vm::selectDay,
                )
            }
            item { Spacer(Modifier.height(12.dp)) }
            item { MonthSummaryCard(state) }
            item { Spacer(Modifier.height(10.dp)) }
            item {
                Text(
                    text = "Registros del mes".uppercase(Locale.getDefault()),
                    color = OreoPalette.OnSurfaceFaint,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 22.dp, top = 8.dp, bottom = 6.dp),
                )
            }
            if (state.records.isEmpty()) {
                item { EmptyMonth() }
            } else {
                val ordered = state.records.sortedByDescending { it.dateMillis }
                items(ordered, key = { it.id }) { r ->
                    RegistroRow(
                        record = r,
                        selected = r.dateMillis == state.selectedDayMillis,
                        onClick = {
                            vm.selectDay(r.dateMillis)
                            showEditor = true
                        },
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
                onSave = { rec ->
                    vm.upsert(rec)
                    showEditor = false
                },
                onDelete = existing?.let { rec ->
                    {
                        vm.delete(rec)
                        showEditor = false
                    }
                },
            )
        }
    }
}

@Composable
private fun TopBar(
    onBack: () -> Unit,
    onToday: () -> Unit,
    onExportPdf: () -> Unit,
) {
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
            text = "Volver",
            color = OreoPalette.Accent,
            fontSize = 16.sp,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(end = 12.dp),
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onExportPdf) {
            Icon(
                imageVector = Icons.Outlined.PictureAsPdf,
                contentDescription = "Exportar mes a PDF",
                tint = OreoPalette.Accent,
            )
        }
        IconButton(onClick = onToday) {
            Icon(
                imageVector = Icons.Outlined.Today,
                contentDescription = "Mes actual",
                tint = OreoPalette.Accent,
            )
        }
    }
}

@Composable
private fun MonthSummaryCard(state: ServicioCampoUiState) {
    Column(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .background(
                color = OreoPalette.SurfaceCard,
                shape = RoundedCornerShape(22.dp),
            )
            .padding(16.dp)
            .fillMaxWidth(),
    ) {
        Text(
            text = "Resumen del mes",
            color = OreoPalette.OnSurfaceMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        val hours = state.totals.totalHours
        val animatedHours by animateFloatAsState(
            targetValue = hours.toFloat(),
            animationSpec = tween(durationMillis = 600, easing = OreoMotion.EaseOut),
            label = "totalHoursAnim",
        )
        Text(
            text = "%.1f horas".format(Locale("es", "ES"), animatedHours.toDouble()),
            color = OreoPalette.OnSurface,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatTile(value = state.totals.totalRevisits, label = "Revisitas")
            StatTile(value = state.totals.totalStudies, label = "Cursos")
        }
    }
}

@Composable
private fun StatTile(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            color = OreoPalette.AccentSub,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = OreoPalette.OnSurfaceFaint,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun EmptyMonth() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Sin registros este mes",
            color = OreoPalette.OnSurfaceMuted,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tocá el + para agregar el primero.",
            color = OreoPalette.OnSurfaceFaint,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun MiniCalendar(
    month: MonthRef,
    records: List<RegistroCampo>,
    selectedDayMillis: Long,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSelectDay: (Long) -> Unit,
) {
    val today = remember { RegistroCampoRepository.startOfDayMillis(System.currentTimeMillis()) }
    val recordedDays = remember(records) {
        records.map { it.dateMillis }.toHashSet()
    }
    val monthLabel = remember(month) {
        val c = Calendar.getInstance().apply {
            clear()
            set(month.year, month.month, 1)
        }
        SimpleDateFormat("MMMM yyyy", Locale("es", "ES")).format(c.time)
            .replaceFirstChar { it.uppercase(Locale.getDefault()) }
    }
    Column(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .background(
                color = OreoPalette.SurfaceCard,
                shape = RoundedCornerShape(22.dp),
            )
            .padding(12.dp)
            .fillMaxWidth(),
    ) {
        // Header con mes + flechas.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrev) {
                Icon(
                    Icons.Outlined.ChevronLeft,
                    contentDescription = "Mes anterior",
                    tint = OreoPalette.Accent,
                )
            }
            Spacer(Modifier.weight(1f))
            AnimatedContent(
                targetState = monthLabel,
                transitionSpec = {
                    (slideInVertically { it / 4 } + fadeIn(tween(160))) togetherWith
                        (slideOutVertically { -it / 4 } + fadeOut(tween(120)))
                },
                label = "monthLabel",
            ) { label ->
                Text(
                    text = label,
                    color = OreoPalette.OnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onNext) {
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = "Mes siguiente",
                    tint = OreoPalette.Accent,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        // Etiquetas L M M J V S D (semana lunes-domingo).
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("L", "M", "M", "J", "V", "S", "D").forEach { d ->
                Text(
                    text = d,
                    color = OreoPalette.OnSurfaceFaint,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        // Grid 7×N.
        val cells = remember(month) { buildMonthCells(month.year, month.month) }
        val rows = cells.chunked(7)
        rows.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { cell ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (cell == null) {
                            Spacer(Modifier.size(36.dp))
                        } else {
                            DayCell(
                                dayMillis = cell.first,
                                dayOfMonth = cell.second,
                                isToday = cell.first == today,
                                isSelected = cell.first == selectedDayMillis,
                                hasRecord = cell.first in recordedDays,
                                onClick = { onSelectDay(cell.first) },
                            )
                        }
                    }
                }
                if (week.size < 7) {
                    repeat(7 - week.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    dayMillis: Long,
    dayOfMonth: Int,
    isToday: Boolean,
    isSelected: Boolean,
    hasRecord: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec = OreoMotion.SpringBouncy(),
        label = "dayPressScale",
    )
    val pillSize by animateDpAsState(
        targetValue = if (isSelected) 36.dp else 32.dp,
        animationSpec = OreoMotion.SpringBouncy(),
        label = "daySelectedSize",
    )
    val bg = when {
        isSelected -> OreoPalette.Accent
        isToday -> OreoPalette.AccentSub.copy(alpha = 0.25f)
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> Color.White
        isToday -> OreoPalette.AccentSub
        else -> OreoPalette.OnSurface
    }
    Box(
        modifier = Modifier
            .size(pillSize)
            .scale(pressScale)
            .clip(CircleShape)
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = dayOfMonth.toString(),
                color = textColor,
                fontSize = 14.sp,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
            )
            if (hasRecord && !isSelected) {
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(OreoPalette.Accent, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun RegistroRow(
    record: RegistroCampo,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = OreoMotion.SpringBouncy(),
        label = "registroPressScale",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .scale(pressScale)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) OreoPalette.AccentSub.copy(alpha = 0.18f)
                else OreoPalette.SurfaceCard,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DayBadge(record.dateMillis)
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            val desglose = listOfNotNull(
                record.revisits.takeIf { it > 0 }?.let { "$it rev." },
                record.studies.takeIf { it > 0 }?.let { "$it cursos" },
            ).joinToString(" · ")
            val headerText = "%.1f h".format(Locale("es", "ES"), record.hours) +
                if (desglose.isNotEmpty()) " · $desglose" else ""
            Text(
                text = headerText,
                color = OreoPalette.OnSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (record.notes.isNotBlank()) {
                Text(
                    text = record.notes,
                    color = OreoPalette.OnSurfaceMuted,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DayBadge(dayMillis: Long) {
    val cal = remember(dayMillis) {
        Calendar.getInstance().apply { timeInMillis = dayMillis }
    }
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val monthShort = remember(dayMillis) {
        SimpleDateFormat("MMM", Locale("es", "ES")).format(Date(dayMillis))
            .uppercase(Locale.getDefault())
            .trimEnd('.')
    }
    Column(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(OreoPalette.Accent.copy(alpha = 0.18f)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = monthShort,
            color = OreoPalette.AccentSub,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = day.toString(),
            color = OreoPalette.OnSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Construye las celdas de un mes para el grid 7×N. Devuelve `null`
 * para los huecos antes/después del mes y un par
 * (dayMillis, dayOfMonth) para cada día válido. La semana arranca
 * **lunes** (estilo internacional / Latinoamérica).
 */
private fun buildMonthCells(year: Int, month: Int): List<Pair<Long, Int>?> {
    val cal = Calendar.getInstance().apply {
        clear()
        set(year, month, 1, 0, 0, 0)
    }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Calendar.DAY_OF_WEEK: 1=Domingo, 2=Lunes, ... 7=Sábado.
    // Convertimos a "0=lunes ... 6=domingo".
    val firstDow = cal.get(Calendar.DAY_OF_WEEK)
    val leading = (firstDow + 5) % 7
    val cells = mutableListOf<Pair<Long, Int>?>()
    repeat(leading) { cells.add(null) }
    for (day in 1..daysInMonth) {
        val c = Calendar.getInstance().apply {
            clear()
            set(year, month, day, 0, 0, 0)
        }
        cells.add(c.timeInMillis to day)
    }
    // Padding final para completar la última semana.
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}


