package com.oreoexperience.notes.ui.editor

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.RemoveCircleOutline
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

            item {
                Text(
                    text = stringResource(R.string.editor_section_points),
                    style = MaterialTheme.typography.titleSmall,
                    color = OreoPalette.OnSurfaceMuted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            itemsIndexed(state.points, key = { idx, _ -> "p$idx" }) { idx, p ->
                PointCard(
                    index = idx,
                    point = p,
                    onText = { vm.setPointText(idx, it) },
                    onAddSub = { vm.addSubpoint(idx) },
                    onSetSub = { si, t -> vm.setSubpoint(idx, si, t) },
                    onRemoveSub = { si -> vm.removeSubpoint(idx, si) },
                    onRemove = { vm.removePoint(idx) },
                )
            }

            item {
                FilledTonalButton(onClick = vm::addPoint, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Add, null)
                    Spacer(Modifier.height(0.dp))
                    Text(text = "  " + stringResource(R.string.editor_add_point))
                }
            }

            item { NotesField(state.notes, vm::setNotes) }
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
private fun NotesField(value: String, onChange: (String) -> Unit) {
    GlassFieldShell {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().padding(2.dp).height(220.dp),
            placeholder = { Text(stringResource(R.string.editor_notes_hint)) },
            singleLine = false,
            shape = RoundedCornerShape(16.dp),
            colors = transparentFieldColors(),
        )
    }
}

@Composable
private fun PointCard(
    index: Int,
    point: Punto,
    onText: (String) -> Unit,
    onAddSub: () -> Unit,
    onSetSub: (Int, String) -> Unit,
    onRemoveSub: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${index + 1}.",
                    color = OreoPalette.Accent,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp),
                )
                OutlinedTextField(
                    value = point.text,
                    onValueChange = onText,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.editor_point_hint)) },
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = transparentFieldColors(),
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Outlined.Delete, null, tint = OreoPalette.DangerFill)
                }
            }
            Spacer(Modifier.height(6.dp))
            point.subpoints.forEachIndexed { si, sub ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 24.dp)) {
                    Text(
                        text = "•",
                        color = OreoPalette.AccentSub,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    OutlinedTextField(
                        value = sub,
                        onValueChange = { onSetSub(si, it) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.editor_subpoint_hint)) },
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp),
                        colors = transparentFieldColors(),
                    )
                    IconButton(onClick = { onRemoveSub(si) }) {
                        Icon(Icons.Outlined.RemoveCircleOutline, null, tint = OreoPalette.OnSurfaceMuted)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            FilledTonalButton(onClick = onAddSub) {
                Icon(Icons.Outlined.Add, null)
                Text(text = "  Sub-punto")
            }
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
