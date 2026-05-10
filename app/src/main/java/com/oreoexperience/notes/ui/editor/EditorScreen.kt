@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi::class,
)

package com.oreoexperience.notes.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import com.oreoexperience.notes.ui.LocalAppContainer
import com.oreoexperience.notes.ui.components.BottomTimerBar
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Editor de nota estilo **iOS Notes** (modo oscuro): fondo negro,
 * título y cuerpo a pantalla completa, top bar con botón "Notas"
 * (volver) a la izquierda y "Listo" a la derecha en amarillo iOS.
 *
 * Comportamiento:
 *   - Auto-guardado al volver atrás (no hay botón "guardar" — el flujo
 *     iOS guarda solo al cerrar la nota).
 *   - El menú "⋯" expone funciones extra: "Establecer cronómetro" y
 *     "Eliminar nota".
 *   - Si la nota tiene `targetDurationSec > 0`, aparece la barra
 *     [BottomTimerBar] anclada arriba del teclado / nav bar.
 */
@Composable
fun EditorScreen(
    discursoId: Long,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: EditorViewModel = viewModel(
        factory = viewModelFactory {
            initializer { EditorViewModel(container.repository) }
        }
    )
    LaunchedEffect(discursoId) { vm.load(discursoId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showDelete by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    // Editor de texto rich (WYSIWYG)
    val richState = rememberRichTextState()
    LaunchedEffect(state.loaded, state.id) {
        if (state.loaded && richState.toMarkdown() != state.body) {
            richState.setMarkdown(state.body)
        }
    }
    LaunchedEffect(richState) {
        snapshotFlow { richState.annotatedString }
            .drop(1)
            .distinctUntilChanged()
            .collect { vm.setBody(richState.toMarkdown()) }
    }

    suspend fun saveAndBack() {
        val newId = vm.save()
        onSaved(newId)
    }
    BackHandler {
        scope.launch { saveAndBack() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OreoPalette.Bg0),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // Top bar estilo iOS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { scope.launch { saveAndBack() } }) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = null,
                        tint = OreoPalette.Accent,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "Notas",
                        color = OreoPalette.Accent,
                        fontSize = 17.sp,
                    )
                }
                Spacer(Modifier.weight(1f))

                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreHoriz,
                            contentDescription = "Opciones",
                            tint = OreoPalette.Accent,
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Establecer cronómetro") },
                            onClick = {
                                menuOpen = false
                                showTimerDialog = true
                            },
                        )
                        if (!state.isNew) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Eliminar nota",
                                        color = OreoPalette.DangerFill,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        null,
                                        tint = OreoPalette.DangerFill,
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    showDelete = true
                                },
                            )
                        }
                    }
                }

                TextButton(onClick = { scope.launch { saveAndBack() } }) {
                    Text(
                        text = "Listo",
                        color = OreoPalette.Accent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                    )
                }
            }

            // Cuerpo scrolleable
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
            ) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatNowDate(),
                    color = OreoPalette.OnSurfaceFaint,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
                BasicTextField(
                    value = state.title,
                    onValueChange = vm::setTitle,
                    textStyle = LocalTextStyle.current.copy(
                        color = OreoPalette.OnSurface,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp,
                    ),
                    cursorBrush = SolidColor(OreoPalette.Accent),
                    keyboardOptions = KeyboardOptions(),
                    keyboardActions = KeyboardActions(),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box {
                            if (state.title.isEmpty()) {
                                Text(
                                    text = "Título",
                                    color = OreoPalette.OnSurfaceFaint,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            inner()
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))

                RichTextEditor(
                    state = richState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 360.dp),
                    textStyle = LocalTextStyle.current.copy(
                        color = OreoPalette.OnSurface,
                        fontSize = 17.sp,
                        lineHeight = 24.sp,
                    ),
                    colors = RichTextEditorDefaults.richTextEditorColors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent,
                        cursorColor = OreoPalette.Accent,
                        textColor = OreoPalette.OnSurface,
                        containerColor = Color.Transparent,
                    ),
                    placeholder = {
                        Text(
                            text = "Empezá a escribir…",
                            color = OreoPalette.OnSurfaceFaint,
                            fontSize = 17.sp,
                        )
                    },
                )
                Spacer(Modifier.height(120.dp))
            }

            // Cronómetro inferior. Slide-up con spring low-bouncy para
            // que aparezca con un pequeño rebote al asentarse.
            AnimatedVisibility(
                visible = state.loaded && state.targetDurationSec > 0,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    initialOffsetY = { it },
                ) + fadeIn(),
                exit = slideOutVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    targetOffsetY = { it },
                ) + fadeOut(),
            ) {
                BottomTimerBar(
                    targetSec = state.targetDurationSec,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Toolbar de formato
            FormatToolbar(richState)
        }

        if (showTimerDialog) {
            TimerDurationDialog(
                initialMinutes = state.targetDurationSec / 60,
                onDismiss = { showTimerDialog = false },
                onConfirm = { minutes ->
                    vm.setTargetMinutes(minutes)
                    showTimerDialog = false
                },
            )
        }

        if (showDelete) {
            AlertDialog(
                onDismissRequest = { showDelete = false },
                title = { Text("Eliminar nota") },
                text = { Text("Esta acción no se puede deshacer.") },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            vm.deleteCurrent()
                            showDelete = false
                            onSaved(0L)
                        }
                    }) {
                        Text("Eliminar", color = OreoPalette.DangerFill)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDelete = false }) {
                        Text("Cancelar", color = OreoPalette.Accent)
                    }
                },
                containerColor = OreoPalette.SurfaceCard,
                titleContentColor = OreoPalette.OnSurface,
                textContentColor = OreoPalette.OnSurfaceMuted,
            )
        }
    }
}

@Composable
private fun FormatToolbar(state: RichTextState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(OreoPalette.Bg1)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
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
            VerticalDivider()
            ToolbarButton(
                icon = Icons.Outlined.Title,
                description = "Encabezado",
                active = state.currentSpanStyle.fontSize == 22.sp,
                onClick = {
                    state.toggleSpanStyle(SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold))
                },
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
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (active) OreoPalette.Accent else OreoPalette.OnSurfaceMuted,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .height(20.dp)
            .width(1.dp)
            .background(OreoPalette.Outline),
    )
}

@Composable
private fun TimerDurationDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var text by remember { mutableStateOf(if (initialMinutes > 0) initialMinutes.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cronómetro de la nota") },
        text = {
            Column {
                Text(
                    text = "Duración objetivo en minutos. Dejá vacío o 0 para ocultar la barra.",
                    color = OreoPalette.OnSurfaceMuted,
                )
                Spacer(Modifier.height(12.dp))
                BasicTextField(
                    value = text,
                    onValueChange = { v -> text = v.filter { it.isDigit() }.take(4) },
                    textStyle = LocalTextStyle.current.copy(
                        color = OreoPalette.OnSurface,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    cursorBrush = SolidColor(OreoPalette.Accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    decorationBox = { inner ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (text.isEmpty()) {
                                    Text(
                                        "0",
                                        color = OreoPalette.OnSurfaceFaint,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                inner()
                            }
                            Text(
                                "min",
                                color = OreoPalette.OnSurfaceMuted,
                                fontSize = 16.sp,
                            )
                        }
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.toIntOrNull() ?: 0) }) {
                Text("Guardar", color = OreoPalette.Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = OreoPalette.OnSurfaceMuted)
            }
        },
        containerColor = OreoPalette.SurfaceCard,
        titleContentColor = OreoPalette.OnSurface,
    )
}

private fun formatNowDate(): String {
    val sdf = SimpleDateFormat("d 'de' MMMM 'de' yyyy 'a las' HH:mm", Locale("es"))
    return sdf.format(Date())
}
