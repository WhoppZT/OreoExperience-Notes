@file:OptIn(ExperimentalMaterial3Api::class)

package com.oreoexperience.notes.ui.editor

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.oreoexperience.notes.data.Discurso
import com.oreoexperience.notes.data.PdfDensity
import com.oreoexperience.notes.data.PdfExportSettings
import com.oreoexperience.notes.data.PdfExportManager
import com.oreoexperience.notes.data.PdfMargin
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bottom sheet "Exportar PDF" — la nueva entrada al flujo de export
 * que reemplaza el SAF directo:
 *
 *   1. Vista previa de la página 1 (rasterizada con `PdfRenderer`).
 *   2. Pickers en línea para densidad / márgenes / portada / color.
 *      Al cambiar cualquiera, la preview se regenera (debounce 250ms).
 *   3. Dos acciones primarias:
 *        - **Guardar**: lanza `CreateDocument` para que el usuario elija
 *          ubicación. Mismo flujo que antes.
 *        - **Compartir**: genera el PDF en `cache/exports/` y lanza
 *          `Intent.ACTION_SEND` vía `FileProvider`. No pide permisos.
 *
 * Recibe un closure `onRequestSafExport` que el editor implementa con
 * un `rememberLauncherForActivityResult(CreateDocument)`. De esa
 * forma este composable no toca permisos ni SAF directamente.
 */
@Composable
fun ExportPdfSheet(
    discurso: Discurso,
    onDismiss: () -> Unit,
    onRequestSafExport: (settings: PdfExportSettings) -> Unit,
) {
    val container = com.oreoexperience.notes.ui.LocalAppContainer.current
    val pdfManager = container.pdfExportManager
    val prefs = container.userPreferences
    val ctx = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val density by prefs.pdfDensityState
    val margin by prefs.pdfMarginState
    val cover by prefs.pdfIncludeCoverState
    val watermark by prefs.pdfWatermarkState
    val colored by prefs.pdfColorState

    // Preview state. Cada vez que cambia un setting reanudamos la
    // generación del bitmap. El cálculo es caro (corre PdfDocument +
    // PdfRenderer), por eso lo pasamos a un Flow con debounce.
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var generating by remember { mutableStateOf(false) }
    val settingsFlow = remember { MutableStateFlow(prefs.pdfExportSettings()) }
    LaunchedEffect(density, margin, cover, watermark, colored) {
        settingsFlow.value = prefs.pdfExportSettings()
    }
    LaunchedEffect(discurso.id) {
        settingsFlow
            .debounce(250)
            .distinctUntilChanged()
            .onEach { generating = true }
            .map { s ->
                runCatching {
                    pdfManager.renderPreviewBitmap(
                        discursos = listOf(discurso),
                        title = discurso.title.ifBlank { "Nota" },
                        settings = s,
                    )
                }.getOrNull()
            }
            .flowOn(Dispatchers.Default)
            .collect { bmp ->
                preview = bmp
                generating = false
            }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OreoPalette.Bg1,
        dragHandle = null,
        sheetMaxWidth = 640.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // Handle pequeño centrado.
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .background(
                            color = OreoPalette.Outline,
                            shape = RoundedCornerShape(2.dp),
                        ),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Exportar como PDF",
                color = OreoPalette.OnSurface,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = discurso.title.ifBlank { "Nota" },
                color = OreoPalette.OnSurfaceMuted,
                fontSize = 13.sp,
            )

            Spacer(Modifier.height(14.dp))

            // ---------- Contenido scrolleable ----------
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                // Vista previa
                PreviewFrame(bitmap = preview, generating = generating)

                Spacer(Modifier.height(14.dp))

                // Settings inline
                SettingsCard(
                    density = density,
                    margin = margin,
                    cover = cover,
                    colored = colored,
                    onDensity = { prefs.pdfDensity = it },
                    onMargin = { prefs.pdfMargin = it },
                    onCover = { prefs.pdfIncludeCover = it },
                    onColored = { prefs.pdfColor = it },
                )
            }

            Spacer(Modifier.height(16.dp))

            // ---------- Acciones (fijas abajo) ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(56.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        sharePdf(
                            ctx = ctx,
                            pdfManager = pdfManager,
                            discurso = discurso,
                            settings = prefs.pdfExportSettings(),
                            onDone = onDismiss,
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, OreoPalette.Accent),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = OreoPalette.Accent,
                    ),
                ) {
                    Icon(
                        Icons.Outlined.Share,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Compartir",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                OutlinedButton(
                    onClick = {
                        onRequestSafExport(prefs.pdfExportSettings())
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, OreoPalette.Accent),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = OreoPalette.Accent,
                    ),
                ) {
                    Icon(
                        Icons.Outlined.Download,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Guardar",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PreviewFrame(bitmap: Bitmap?, generating: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(595f / 842f)
            .heightIn(max = 420.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = 1.dp,
                color = OreoPalette.Outline,
                shape = RoundedCornerShape(14.dp),
            )
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Vista previa del PDF",
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (generating) {
            // Overlay tenue mientras se regenera la preview.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OreoPalette.OnSurfaceFaint),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Renderizando…",
                    color = OreoPalette.Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    density: PdfDensity,
    margin: PdfMargin,
    cover: Boolean,
    colored: Boolean,
    onDensity: (PdfDensity) -> Unit,
    onMargin: (PdfMargin) -> Unit,
    onCover: (Boolean) -> Unit,
    onColored: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = OreoPalette.SurfaceCard,
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Densidad — segmented horizontal.
        Text(
            text = "Densidad",
            color = OreoPalette.OnSurfaceMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
        SegmentedRow(
            options = PdfDensity.values().toList(),
            current = density,
            labelOf = { it.label },
            onPick = onDensity,
        )

        Text(
            text = "Márgenes",
            color = OreoPalette.OnSurfaceMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp),
        )
        SegmentedRow(
            options = PdfMargin.values().toList(),
            current = margin,
            labelOf = { it.label },
            onPick = onMargin,
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleChip(
                label = "Portada",
                checked = cover,
                onCheckedChange = onCover,
                modifier = Modifier.weight(1f),
            )
            ToggleChip(
                label = "A color",
                checked = colored,
                onCheckedChange = onColored,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun <T> SegmentedRow(
    options: List<T>,
    current: T,
    labelOf: (T) -> String,
    onPick: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = OreoPalette.SurfaceCardHi,
                shape = RoundedCornerShape(10.dp),
            )
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { opt ->
            val selected = opt == current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        color = if (selected) OreoPalette.Accent else Color.Transparent,
                    )
                    .clickable { onPick(opt) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = labelOf(opt),
                    color = if (selected) Color.White else OreoPalette.OnSurfaceMuted,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun ToggleChip(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Colors for better visibility in both states
    val isDark = isSystemInDarkTheme()
    val bgColor = when {
        checked -> OreoPalette.Accent.copy(alpha = 0.15f)
        isDark -> OreoPalette.SurfaceCardHi  // Use SurfaceCardHi for better contrast
        else -> OreoPalette.SurfaceCardHi
    }
    val borderColor = when {
        checked -> OreoPalette.Accent
        isDark -> OreoPalette.Accent.copy(alpha = 0.4f)  // More visible border
        else -> OreoPalette.Outline
    }
    val textColor = when {
        checked -> OreoPalette.Accent
        else -> OreoPalette.OnSurface
    }

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color = bgColor)
            .border(
                width = if (checked) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

private fun sharePdf(
    ctx: android.content.Context,
    pdfManager: PdfExportManager,
    discurso: Discurso,
    settings: PdfExportSettings,
    onDone: () -> Unit,
) {
    // El export es bloqueante: lo lanzamos en un scope de aplicación
    // para que sobreviva al dismiss del sheet. Si falla, mostramos un
    // toast en el thread principal.
    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
        runCatching {
            val cacheDir = File(ctx.cacheDir, "exports").apply { mkdirs() }
            val safe = discurso.title.ifBlank { "nota" }
                .lowercase()
                .replace(" ", "_")
                .replace("/", "-")
                .take(40)
            val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val outFile = File(cacheDir, "oreo_${safe}_$timestamp.pdf")
            pdfManager.exportToFile(
                discursos = listOf(discurso),
                title = discurso.title.ifBlank { "Nota" },
                outFile = outFile,
                settings = settings,
            )

            // FileProvider URI — accesible al receptor del Intent.
            val authority = ctx.packageName + ".fileprovider"
            val shareUri = FileProvider.getUriForFile(ctx, authority, outFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                putExtra(Intent.EXTRA_SUBJECT, discurso.title.ifBlank { "Nota" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            withContext(Dispatchers.Main) {
                ctx.startActivity(
                    Intent.createChooser(intent, "Compartir PDF").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
                onDone()
            }
        }.onFailure { err ->
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(
                    ctx,
                    "No se pudo compartir: ${err.message ?: "error"}",
                    android.widget.Toast.LENGTH_LONG,
                ).show()
            }
        }
    }
}
