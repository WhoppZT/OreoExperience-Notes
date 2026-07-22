package com.oreoexperience.notes.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.oreoexperience.notes.data.update.DownloadProgress
import com.oreoexperience.notes.data.update.GitHubRelease
import com.oreoexperience.notes.data.update.UpdateManager
import com.oreoexperience.notes.data.update.formatBytes
import com.oreoexperience.notes.data.update.formatRelativeTime
import com.oreoexperience.notes.data.update.parseGitHubDate
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlinx.coroutines.launch

@Composable
fun UpdateDialog(
    release: GitHubRelease,
    isCritical: Boolean = false,
    sizeBytes: Long = 0L,
    updateManager: UpdateManager,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val progress by updateManager.downloadProgress.collectAsState()
    val isCompact = screenWidthDp < 600

    val canDismiss = progress is DownloadProgress.Idle && !isCritical
    val releaseUrl = "https://github.com/WhoppZT/OreoExperience-Notes/releases/tag/${release.tagName}"
    val ctx = LocalContext.current

    Dialog(
        onDismissRequest = { if (canDismiss) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = canDismiss,
            dismissOnClickOutside = canDismiss,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isCompact) 20.dp else 48.dp)
                .widthIn(max = if (isCompact) 440.dp else 520.dp)
                .heightIn(max = (screenHeightDp * 0.75f).dp)
                .clip(RoundedCornerShape(24.dp))
                .background(OreoPalette.SurfaceCard),
        ) {
            Column {
                // ── Header ──────────────────────────────────────────────
                Header(
                    release = release,
                    isCritical = isCritical,
                    sizeBytes = sizeBytes,
                    canDismiss = canDismiss,
                    onDismiss = onDismiss,
                )

                // ── Body según estado ───────────────────────────────────
                when (val p = progress) {
                    is DownloadProgress.Idle -> {
                        ReleaseBody(
                            release = release,
                            isCritical = isCritical,
                            releaseUrl = releaseUrl,
                        )
                        ActionButtons(
                            isCritical = isCritical,
                            onDownload = { updateManager.downloadAndInstall(release, scope) },
                            onDismiss = onDismiss,
                            onViewOnGitHub = {
                                try {
                                    ctx.startActivity(android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(releaseUrl),
                                    ))
                                } catch (_: Exception) {}
                            },
                        )
                    }
                    is DownloadProgress.Downloading -> {
                        DownloadProgressBody(p)
                    }
                    is DownloadProgress.Verifying -> {
                        VerifyingBody()
                    }
                    is DownloadProgress.Failed -> {
                        FailedBody(p)
                        FailedButtons(
                            onRetry = { updateManager.downloadAndInstall(release, scope) },
                            onDismiss = onDismiss,
                        )
                    }
                    is DownloadProgress.Ready -> {
                        LaunchedEffect(Unit) { onDismiss() }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    release: GitHubRelease,
    isCritical: Boolean,
    sizeBytes: Long,
    canDismiss: Boolean,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(OreoPalette.SurfaceCard)
            .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(OreoPalette.Accent.copy(alpha = 0.15f))
                    .border(1.dp, OreoPalette.Accent.copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Update,
                    contentDescription = null,
                    tint = OreoPalette.Accent,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Actualización disponible",
                        color = OreoPalette.OnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isCritical) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(OreoPalette.WarnFill.copy(alpha = 0.2f))
                                .border(0.5.dp, OreoPalette.WarnFill, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "Crítica",
                                color = OreoPalette.WarnFill,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                val versionStr = release.tagName.trimStart('v', 'V')
                val sizeStr = if (sizeBytes > 0) formatBytes(sizeBytes) else ""
                val timeStr = formatRelativeTime(parseGitHubDate(release.publishedAt))
                Text(
                    text = listOfNotNull(versionStr.ifBlank { null }, sizeStr.ifBlank { null }, timeStr.ifBlank { null })
                        .joinToString(" · "),
                    color = OreoPalette.OnSurfaceMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (canDismiss) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Cerrar",
                        tint = OreoPalette.OnSurfaceMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.ReleaseBody(
    release: GitHubRelease,
    isCritical: Boolean,
    releaseUrl: String,
) {
    val isCached = release.body.isBlank()

    if (isCached) {
        Text(
            text = "Novedades",
            color = OreoPalette.Accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Mejoras de rendimiento y correcciones de errores.",
            color = OreoPalette.OnSurface,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Visita GitHub para ver la lista completa de cambios.",
            color = OreoPalette.OnSurfaceMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }

    if (isCritical) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Esta actualización corrige un problema importante. Te recomendamos instalarla ahora.",
            color = OreoPalette.WarnFill,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}

@Composable
private fun ActionButtons(
    isCritical: Boolean,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
    onViewOnGitHub: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Button(
            onClick = onDownload,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = OreoPalette.Accent,
                contentColor = androidx.compose.ui.graphics.Color.White,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Text(text = "Actualizar ahora", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        if (!isCritical) {
            OutlinedButton(
                onClick = onViewOnGitHub,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OreoPalette.Accent),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.dp,
                ),
            ) {
                Text(text = "Ver en GitHub", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(40.dp),
            ) {
                Text(
                    text = "Más tarde",
                    color = OreoPalette.OnSurfaceMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.DownloadProgressBody(progress: DownloadProgress.Downloading) {
    val animRatio by animateFloatAsState(
        targetValue = progress.ratio,
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "downloadProgress",
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Descargando...",
            color = OreoPalette.OnSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { animRatio },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = OreoPalette.Accent,
            trackColor = OreoPalette.Bg1,
            strokeCap = StrokeCap.Round,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "${(progress.ratio * 100).toInt()}% · ${formatBytes(progress.bytesReceived)} de ${formatBytes(progress.totalBytes)}",
            color = OreoPalette.OnSurfaceMuted,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun ColumnScope.VerifyingBody() {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Verificando descarga...",
            color = OreoPalette.OnSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { 1f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = OreoPalette.Accent,
            trackColor = OreoPalette.Bg1,
            strokeCap = StrokeCap.Round,
        )
    }
}

@Composable
private fun ColumnScope.FailedBody(fail: DownloadProgress.Failed) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No se pudo descargar",
            color = OreoPalette.DangerFill,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = fail.message,
            color = OreoPalette.OnSurfaceMuted,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun FailedButtons(
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = OreoPalette.Accent,
                contentColor = androidx.compose.ui.graphics.Color.White,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Text(text = "Reintentar", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = OreoPalette.OnSurfaceMuted),
        ) {
            Text(text = "Cerrar", fontSize = 15.sp)
        }
    }
}
