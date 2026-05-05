package com.oreoexperience.notes.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.oreoexperience.notes.R
import com.oreoexperience.notes.data.Discurso
import com.oreoexperience.notes.ui.LocalAppContainer
import com.oreoexperience.notes.ui.components.GlassCard
import com.oreoexperience.notes.ui.theme.OreoPalette
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleSmall,
                            color = OreoPalette.OnSurfaceMuted,
                        )
                        Text(
                            text = stringResource(R.string.home_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = OreoPalette.OnSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
        floatingActionButton = {
            // Cuando todavía no hay discursos guardados, el FAB respira
            // suave para guiar la mirada del usuario hacia él.
            val infinite = rememberInfiniteTransition(label = "fabPulse")
            val rawPulse by infinite.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1_400),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "fabPulseScale",
            )
            val pulse = if (state.items.isEmpty()) rawPulse else 1.0f
            ExtendedFloatingActionButton(
                onClick = onNew,
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text(stringResource(R.string.home_fab_new)) },
                containerColor = OreoPalette.Accent,
                contentColor = Color(0xFF1A0B33),
                modifier = Modifier.scale(pulse),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            // Search box
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.home_search_hint)) },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = OreoPalette.GlassFill,
                    unfocusedContainerColor = OreoPalette.GlassFill,
                    focusedBorderColor = OreoPalette.Accent.copy(alpha = 0.7f),
                    unfocusedBorderColor = OreoPalette.Outline,
                    cursorColor = OreoPalette.Accent,
                    focusedTextColor = OreoPalette.OnSurface,
                    unfocusedTextColor = OreoPalette.OnSurface,
                    focusedPlaceholderColor = OreoPalette.OnSurfaceMuted,
                    unfocusedPlaceholderColor = OreoPalette.OnSurfaceMuted,
                    focusedLeadingIconColor = OreoPalette.AccentSub,
                    unfocusedLeadingIconColor = OreoPalette.AccentSub,
                ),
            )
            Spacer(Modifier.height(12.dp))

            if (state.items.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(state.items, key = { _, d -> d.id }) { index, d ->
                        AnimatedRow(index = index) {
                            DiscursoRow(d, onClick = { onOpen(d.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscursoRow(d: Discurso, onClick: () -> Unit) {
    val df = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Description,
                contentDescription = null,
                tint = OreoPalette.Accent,
                modifier = Modifier.padding(end = 14.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = d.title.ifBlank { "(sin título)" },
                    style = MaterialTheme.typography.titleMedium,
                    color = OreoPalette.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (d.scriptures.isNotBlank()) {
                    Text(
                        text = d.scriptures,
                        style = MaterialTheme.typography.bodySmall,
                        color = OreoPalette.OnSurfaceMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = df.format(Date(d.updatedAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = OreoPalette.OnSurfaceMuted,
                    )
                    if (d.targetDurationSec > 0) {
                        Icon(
                            Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = OreoPalette.AccentSub,
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .size(14.dp),
                        )
                        Text(
                            text = " ${d.targetDurationSec / 60} min",
                            style = MaterialTheme.typography.labelSmall,
                            color = OreoPalette.AccentSub,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Anima la entrada de cada row de la lista con un fade + slide hacia
 * arriba. El delay escalonado es proporcional al índice (con un cap
 * para que la lista no tarde demasiado en aparecer si hay muchos items).
 */
@Composable
private fun AnimatedRow(index: Int, content: @Composable () -> Unit) {
    val visibleState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * 35L).coerceAtMost(500L))
        visibleState.targetState = true
    }
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(animationSpec = tween(durationMillis = 280)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 320),
                initialOffsetY = { it / 6 },
            ),
    ) {
        content()
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = OreoPalette.Accent,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = OreoPalette.OnSurface,
            )
            Text(
                text = stringResource(R.string.home_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = OreoPalette.OnSurfaceMuted,
            )
        }
    }
}
