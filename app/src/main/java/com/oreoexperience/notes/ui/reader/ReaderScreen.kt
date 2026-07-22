@file:OptIn(ExperimentalRichTextApi::class)

package com.oreoexperience.notes.ui.reader

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichText
import com.oreoexperience.notes.data.MediaStorage
import com.oreoexperience.notes.data.NoteBlock
import kotlinx.coroutines.delay
import com.oreoexperience.notes.ui.LocalAppContainer
import com.oreoexperience.notes.ui.components.OreoTimer
import com.oreoexperience.notes.ui.components.OreoTimerVariant
import com.oreoexperience.notes.ui.theme.OreoPalette

@Composable
fun ReaderScreen(
    discursoId: Long,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val vm = viewModel<ReaderViewModel>(
        factory = viewModelFactory {
            initializer { ReaderViewModel(container.repository) }
        },
    )
    val state by vm.state.collectAsState()

    LaunchedEffect(discursoId) {
        vm.load(discursoId)
    }

    val context = LocalContext.current
    val activity = context as? Activity

    // Full-screen inmersivo: oculta status y nav bars sin cambiar
    // el modo de insets de la ventana, para que al salir no haya
    // redimensión brusca.
    SideEffect {
        activity?.let { act ->
            WindowInsetsControllerCompat(act.window, act.window.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    // Restaurar system bars al salir.
    DisposableEffect(Unit) {
        onDispose {
            activity?.let { act ->
                WindowInsetsControllerCompat(act.window, act.window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Mantener pantalla encendida.
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    BackHandler { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OreoPalette.Bg0),
    ) {
        val scrollState = rememberScrollState()

        // Contenido scrolleable
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 32.dp, vertical = 24.dp),
        ) {
            // Título
            Text(
                text = state.title.ifBlank { "Nota sin título" },
                color = OreoPalette.OnSurface,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Bloques
            state.blocks.forEach { block ->
                when (block) {
                    is NoteBlock.Text -> ReaderTextBlock(block)
                    is NoteBlock.Image -> ReaderImageBlock(block, container.mediaStorage)
                    is NoteBlock.Video -> ReaderVideoBlock(block, container.mediaStorage)
                    is NoteBlock.Checklist -> ReaderChecklistBlock(block)
                }
                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(80.dp))
        }

        // Cronómetro al pie (mismo diseño que el editor)
        if (state.loaded && state.targetSec > 0) {
            var timerElapsed by remember(state.targetSec) { mutableIntStateOf(0) }
            var timerRunning by remember { mutableStateOf(false) }

            LaunchedEffect(timerRunning, state.targetSec) {
                while (timerRunning) {
                    delay(1_000)
                    timerElapsed += 1
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                OreoTimer(
                    targetSec = state.targetSec,
                    elapsedSec = timerElapsed,
                    running = timerRunning,
                    onToggle = { timerRunning = !timerRunning },
                    onReset = { timerElapsed = 0; timerRunning = false },
                    onClose = null,
                    variant = OreoTimerVariant.Editor,
                )
            }
        }

        // Botón cerrar (top-right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 20.dp, end = 20.dp),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(OreoPalette.SurfaceCard.copy(alpha = 0.6f)),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Cerrar modo lectura",
                    tint = OreoPalette.OnSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

// ─── Bloques ─────────────────────────────────────────────────────

@Composable
private fun ReaderTextBlock(block: NoteBlock.Text) {
    val richState = rememberRichTextState()
    LaunchedEffect(block.markdown) {
        richState.setHtml(block.markdown)
    }
    BasicRichText(
        state = richState,
        modifier = Modifier.fillMaxWidth(),
        style = androidx.compose.ui.text.TextStyle(
            color = OreoPalette.OnSurface,
            fontSize = 19.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Normal,
        ),
    )
}

@Composable
private fun ReaderImageBlock(
    block: NoteBlock.Image,
    mediaStorage: MediaStorage,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(mediaStorage.uriFor(block.fileName))
            .crossfade(true)
            .build(),
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun ReaderVideoBlock(
    block: NoteBlock.Video,
    mediaStorage: MediaStorage,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(mediaStorage.uriFor(block.fileName))
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit,
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = "Reproducir video",
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun ReaderChecklistBlock(block: NoteBlock.Checklist) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        block.items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (item.checked) OreoPalette.Accent
                            else Color.Transparent,
                        )
                        .then(
                            if (!item.checked) Modifier
                                .border(1.5.dp, OreoPalette.Outline, RoundedCornerShape(4.dp))
                            else Modifier,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (item.checked) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = item.text,
                    color = if (item.checked) OreoPalette.OnSurfaceFaint else OreoPalette.OnSurface,
                    fontSize = 19.sp,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                )
            }
        }
    }
}
