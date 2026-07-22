package com.oreoexperience.notes.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.oreoexperience.notes.data.MediaStorage
import com.oreoexperience.notes.ui.theme.OreoPalette

/**
 * Componente que renderiza un bloque de imagen / video dentro de la
 * nota:
 *   - **Imágenes**: se muestran fluidas (max-height 320 dp), bordes
 *     redondeados.
 *   - **Videos**: usamos Coil + VideoFrameDecoder para extraer un
 *     thumbnail del primer fotograma, y superponemos un play button
 *     centrado.
 *   - Tap → preview a pantalla completa.
 *   - Long-press → diálogo "¿Eliminar?".
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MediaPreview(
    fileName: String,
    isVideo: Boolean,
    storage: MediaStorage,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val uri = remember(fileName) { storage.uriFor(fileName) }
    var showFull by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val maxMediaHeight = (screenHeightDp * 0.5f).dp.coerceAtMost(360.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp, max = maxMediaHeight)
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(OreoPalette.SurfaceCard)
            .combinedClickable(
                onClick = { showFull = true },
                onLongClick = { showDelete = true },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isVideo) {
            val request = remember(uri) {
                ImageRequest.Builder(ctx)
                    .data(uri)
                    .decoderFactory(VideoFrameDecoder.Factory())
                    .videoFrameMillis(0L)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = "Video",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            // Play button centrado
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
        } else {
            AsyncImage(
                model = uri,
                contentDescription = "Imagen",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }

    if (showFull) {
        FullScreenMediaPreview(
            fileName = fileName,
            isVideo = isVideo,
            storage = storage,
            onClose = { showFull = false },
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = {
                Text(if (isVideo) "Eliminar video" else "Eliminar imagen")
            },
            text = {
                Text(
                    "Esto removerá el archivo de la nota.",
                    color = OreoPalette.OnSurfaceMuted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    onDelete()
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
            shape = RoundedCornerShape(28.dp),
        )
    }
}

@Composable
private fun FullScreenMediaPreview(
    fileName: String,
    isVideo: Boolean,
    storage: MediaStorage,
    onClose: () -> Unit,
) {
    val ctx = LocalContext.current
    val uri = remember(fileName) { storage.uriFor(fileName) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (isVideo) {
                // Para videos abrimos un VideoView nativo dentro de
                // un AndroidView. La vista previa full-screen reproduce
                // el video (con controles).
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { context ->
                        android.widget.VideoView(context).apply {
                            setVideoURI(uri)
                            val mc = android.widget.MediaController(context)
                            mc.setAnchorView(this)
                            setMediaController(mc)
                            setOnPreparedListener { it.isLooping = false; start() }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
            // Botón de cerrar arriba a la derecha
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.TopEnd,
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color.Black.copy(alpha = 0.55f),
                            CircleShape,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
