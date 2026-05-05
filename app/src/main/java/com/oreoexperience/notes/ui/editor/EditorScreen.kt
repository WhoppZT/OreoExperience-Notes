package com.oreoexperience.notes.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.oreoexperience.notes.R
import com.oreoexperience.notes.data.Punto
import com.oreoexperience.notes.ui.LocalAppContainer
import com.oreoexperience.notes.ui.components.ConfirmDialog
import com.oreoexperience.notes.ui.components.GlassCard
import com.oreoexperience.notes.ui.components.RichBodyEditor
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDelete by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                title = {
                    Text(
                        text = if (state.isNew) stringResource(R.string.editor_new) else stringResource(R.string.editor_edit),
                        style = MaterialTheme.typography.titleMedium,
                        color = OreoPalette.OnSurface,
                    )
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.editor_delete), tint = OreoPalette.DangerFill)
                        }
                    }
                    IconButton(onClick = {
                        scope.launch {
                            val newId = vm.save()
                            snackbar.showSnackbar(message = "")  // pequeño feedback
                            onSaved(newId)
                        }
                    }) {
                        Icon(Icons.Outlined.Check, contentDescription = stringResource(R.string.editor_save), tint = OreoPalette.Accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        }
    ) { padding ->
        if (!state.loaded) return@Scaffold

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { TitleField(state.title, vm::setTitle) }
            item { ScripturesField(state.scriptures, vm::setScriptures) }
            item { TagsField(state.tags, vm::setTags) }
            item { TargetMinutesField(state.targetMinutes, vm::setTargetMinutes) }

            item {
                Text(
                    text = stringResource(R.string.editor_section_points),
                    style = MaterialTheme.typography.titleSmall,
                    color = OreoPalette.OnSurfaceMuted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            itemsIndexed(state.points, key = { idx, _ -> "p$idx" }) { idx, p ->
                SectionCard(
                    point = p,
                    canMoveUp = idx > 0,
                    canMoveDown = idx < state.points.lastIndex,
                    onTitle = { vm.setPointText(idx, it) },
                    onBody = { vm.setPointBody(idx, it) },
                    onMoveUp = { vm.movePointUp(idx) },
                    onMoveDown = { vm.movePointDown(idx) },
                    onRemove = { vm.removePoint(idx) },
                )
            }

            item {
                FilledTonalButton(onClick = vm::addPoint, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Add, null)
                    Text(text = "  " + stringResource(R.string.editor_add_point))
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = stringResource(R.string.editor_confirm_delete_title),
            message = stringResource(R.string.editor_confirm_delete_msg),
            onConfirm = {
                showDelete = false
                scope.launch {
                    vm.deleteCurrent()
                    onBack()
                }
            },
            onDismiss = { showDelete = false },
        )
    }
}

@Composable
private fun TitleField(value: String, onChange: (String) -> Unit) {
    GlassFieldShell {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().padding(2.dp),
            placeholder = { Text(stringResource(R.string.editor_title_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            colors = transparentFieldColors(),
        )
    }
}

@Composable
private fun ScripturesField(value: String, onChange: (String) -> Unit) {
    GlassFieldShell {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().padding(2.dp),
            placeholder = { Text(stringResource(R.string.editor_scriptures_hint)) },
            singleLine = false,
            maxLines = 3,
            shape = RoundedCornerShape(16.dp),
            colors = transparentFieldColors(),
        )
    }
}

@Composable
private fun TagsField(value: String, onChange: (String) -> Unit) {
    GlassFieldShell {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().padding(2.dp),
            placeholder = { Text(stringResource(R.string.editor_tags_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = transparentFieldColors(),
        )
    }
}

@Composable
private fun TargetMinutesField(value: String, onChange: (String) -> Unit) {
    GlassFieldShell {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Icon(
                Icons.Outlined.Timer,
                contentDescription = null,
                tint = OreoPalette.Accent,
                modifier = Modifier.padding(end = 12.dp),
            )
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.weight(1f).padding(2.dp),
                placeholder = { Text(stringResource(R.string.editor_target_minutes_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = transparentFieldColors(),
                trailingIcon = if (value.isNotBlank()) {
                    @Composable {
                        Text(
                            text = "min",
                            color = OreoPalette.OnSurfaceMuted,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                    }
                } else null,
            )
        }
    }
}

/**
 * Card de una sección del discurso. Tiene:
 *   - una fila con el número, el campo de título (ej: "Introducción"),
 *     y los botones de mover arriba/abajo + eliminar.
 *   - un campo de cuerpo grande, que crece con el texto.
 */
@Composable
private fun SectionCard(
    point: Punto,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onTitle: (String) -> Unit,
    onBody: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            OutlinedTextField(
                value = point.text,
                onValueChange = onTitle,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.editor_section_title_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                colors = transparentFieldColors(),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(
                        Icons.Outlined.KeyboardArrowUp,
                        contentDescription = "Mover arriba",
                        tint = if (canMoveUp) OreoPalette.OnSurfaceMuted
                        else OreoPalette.OnSurfaceMuted.copy(alpha = 0.3f),
                    )
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(
                        Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Mover abajo",
                        tint = if (canMoveDown) OreoPalette.OnSurfaceMuted
                        else OreoPalette.OnSurfaceMuted.copy(alpha = 0.3f),
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Eliminar sección", tint = OreoPalette.DangerFill)
                }
            }
            RichBodyEditor(
                text = point.body,
                onTextChange = onBody,
                placeholder = stringResource(R.string.editor_section_body_hint),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun GlassFieldShell(content: @Composable () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        content()
    }
}

@Composable
private fun transparentFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedBorderColor = OreoPalette.Accent.copy(alpha = 0.7f),
    unfocusedBorderColor = Color.Transparent,
    cursorColor = OreoPalette.Accent,
    focusedTextColor = OreoPalette.OnSurface,
    unfocusedTextColor = OreoPalette.OnSurface,
    focusedPlaceholderColor = OreoPalette.OnSurfaceMuted,
    unfocusedPlaceholderColor = OreoPalette.OnSurfaceMuted,
)
