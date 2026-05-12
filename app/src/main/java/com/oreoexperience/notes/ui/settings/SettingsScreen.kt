@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.oreoexperience.notes.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.data.LicenseManager
import com.oreoexperience.notes.data.ThemeMode
import com.oreoexperience.notes.ui.LocalAppContainer
import com.oreoexperience.notes.ui.components.InlineStatusMessage
import com.oreoexperience.notes.ui.theme.OreoMotion
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla de **Ajustes** estilo iOS Notes:
 *
 *   - Header con back / título "Ajustes".
 *   - Sección "General": Auto-guardar, Tema, Orden por defecto.
 *   - Sección "Datos": Hacer copia de seguridad, Restaurar.
 *   - Sección "Avanzado": Eliminadas recientemente.
 *   - Sección "Acerca de": versión + créditos.
 *
 * Los toggles persisten en `UserPreferences` y se ven reflejados en
 * el resto de la app inmediatamente (Theme, sortBy, autoSave).
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onTrash: () -> Unit,
) {
    val container = LocalAppContainer.current
    val prefs = container.userPreferences
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var snackText by remember { mutableStateOf<String?>(null) }
    var showThemePicker by remember { mutableStateOf(false) }

    val themeMode by prefs.themeModeState
    val autoSave by prefs.autoSaveState

    /**
     * SAF: el usuario elige dónde guardar el `.json` (puede tocar
     * Google Drive, Dropbox, almacenamiento local, etc — depende de
     * los providers instalados).
     */
    val backupLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                container.backupManager.export(uri)
            }
            snackText = result.fold(
                onSuccess = { count -> "Copia de seguridad creada · $count notas" },
                onFailure = { e -> "Error al exportar: ${e.message}" },
            )
        }
    }

    /**
     * SAF: abrir un `.json` previo y restaurar. No reemplaza —
     * inserta — para no destruir notas existentes por accidente.
     */
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                container.backupManager.import(uri, replaceAll = false)
            }
            snackText = result.fold(
                onSuccess = { count -> "Importadas $count notas" },
                onFailure = { e -> "Error al restaurar: ${e.message}" },
            )
        }
    }

    Scaffold(containerColor = OreoPalette.Bg0) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item { TopBar(onBack = onBack) }

            item {
                Text(
                    text = "Ajustes",
                    color = OreoPalette.OnSurface,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 18.dp, top = 4.dp, bottom = 12.dp),
                )
            }

            item { SectionHeader("General") }
            item {
                SectionCard {
                    ToggleRow(
                        icon = Icons.Outlined.Save,
                        iconTint = OreoPalette.AccentSub,
                        title = "Auto-guardar",
                        subtitle = "Guardar al volver del editor",
                        checked = autoSave,
                        onCheckedChange = { prefs.autoSave = it },
                    )
                    Divider()
                    ChevronRow(
                        icon = Icons.Outlined.Brightness6,
                        iconTint = OreoPalette.WarnFill,
                        title = "Tema",
                        trailing = themeMode.label,
                        onClick = { showThemePicker = true },
                    )
                }
            }

            item { SectionHeader("Datos") }
            item {
                SectionCard {
                    ChevronRow(
                        icon = Icons.Outlined.CloudUpload,
                        iconTint = OreoPalette.Accent,
                        title = "Hacer copia de seguridad",
                        subtitle = "Guardar a Google Drive u otra ubicación",
                        onClick = {
                            val name = "OreoNotes-${
                                SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                            }.json"
                            backupLauncher.launch(name)
                        },
                    )
                    Divider()
                    ChevronRow(
                        icon = Icons.Outlined.Download,
                        iconTint = OreoPalette.OkFill,
                        title = "Restaurar desde archivo",
                        subtitle = "Importar un .json de copia",
                        onClick = {
                            restoreLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                    )
                }
            }

            item { SectionHeader("Avanzado") }
            item {
                SectionCard {
                    ChevronRow(
                        icon = Icons.Outlined.Delete,
                        iconTint = OreoPalette.DangerFill,
                        title = "Eliminadas recientemente",
                        subtitle = "Restaurar o vaciar la papelera",
                        onClick = onTrash,
                    )
                }
            }

            item { SectionHeader("Acerca de") }
            item {
                SectionCard {
                    InfoRow(
                        icon = Icons.Outlined.Info,
                        iconTint = OreoPalette.AccentSub,
                        title = "OreoExperience · Notas",
                        subtitle = "Creado por Elihu Rueda",
                    )
                    Divider()
                    InfoRow(
                        icon = Icons.Outlined.Info,
                        iconTint = OreoPalette.OnSurfaceFaint,
                        title = "Versión",
                        subtitle = "0.8.0",
                    )
                }
            }

            snackText?.let { msg ->
                item {
                    InlineStatusMessage(
                        text = msg,
                    )
                }
            }
        }

        if (showThemePicker) {
            ThemePicker(
                current = themeMode,
                onPick = {
                    prefs.themeMode = it
                    showThemePicker = false
                },
                onDismiss = { showThemePicker = false },
            )
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
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
                .padding(start = 0.dp, end = 12.dp),
        )
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label.uppercase(Locale.getDefault()),
        color = OreoPalette.OnSurfaceFaint,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .background(
                color = OreoPalette.SurfaceCard,
                shape = RoundedCornerShape(22.dp),
            ),
    ) { content() }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .padding(start = 56.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(OreoPalette.OutlineFaint),
    )
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBubble(icon = icon, tint = iconTint)
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = OreoPalette.OnSurface,
                fontSize = 16.sp,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    color = OreoPalette.OnSurfaceFaint,
                    fontSize = 12.sp,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = OreoPalette.Accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = OreoPalette.SurfaceCardHi,
                uncheckedBorderColor = OreoPalette.OutlineFaint,
            ),
        )
    }
}

@Composable
private fun ChevronRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
    trailing: String? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = OreoMotion.SpringBouncy(),
        label = "settingsRowScale",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(pressScale)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBubble(icon = icon, tint = iconTint)
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = OreoPalette.OnSurface,
                fontSize = 16.sp,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    color = OreoPalette.OnSurfaceFaint,
                    fontSize = 12.sp,
                )
            }
        }
        if (trailing != null) {
            Text(
                trailing,
                color = OreoPalette.OnSurfaceFaint,
                fontSize = 14.sp,
            )
            Spacer(Modifier.size(4.dp))
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = OreoPalette.OnSurfaceFaint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBubble(icon = icon, tint = iconTint)
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = OreoPalette.OnSurface, fontSize = 16.sp)
            Text(subtitle, color = OreoPalette.OnSurfaceFaint, fontSize = 12.sp)
        }
    }
}

@Composable
private fun IconBubble(icon: ImageVector, tint: Color) {
    // Círculo "chiclet" tipo iOS Settings: bubble sólida del color
    // del item, con el icono blanco encima. Más legible que el bubble
    // semi-transparente y refuerza la jerarquía visual.
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        tint.copy(alpha = 0.95f),
                        tint.copy(alpha = 0.75f),
                    ),
                ),
                shape = RoundedCornerShape(8.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ThemePicker(
    current: ThemeMode,
    onPick: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cerrar", color = OreoPalette.Accent)
            }
        },
        title = { Text("Tema", color = OreoPalette.OnSurface) },
        text = {
            Column {
                ThemeMode.values().forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(mode) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(
                                    color = if (mode == current) OreoPalette.Accent
                                    else Color.Transparent,
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (mode == current) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp),
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(
                                            color = Color.Transparent,
                                            shape = CircleShape,
                                        ),
                                )
                            }
                        }
                        Spacer(Modifier.size(12.dp))
                        Text(
                            mode.label,
                            color = OreoPalette.OnSurface,
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        },
        containerColor = OreoPalette.SurfaceCard,
        titleContentColor = OreoPalette.OnSurface,
        textContentColor = OreoPalette.OnSurfaceMuted,
        shape = RoundedCornerShape(28.dp),
    )
}

/**
 * Card de información de licencia: muestra clave activa (enmascarada),
 * fecha de inicio + caducidad, y una barra de progreso animada que
 * indica cuántos días restan sobre el período total (365 días).
 */
@Composable
private fun LicenseCard(license: LicenseManager) {
    // Observamos el state para que la card se actualice cuando se rote
    // la licencia desde la pantalla de Access.
    val activeKey by license.activeKeyState
    val licenseStart by license.licenseStartState

    // Re-evaluamos los derivados cada recomposición (cambian con
    // los states de arriba).
    val days = license.daysRemaining
    val totalDays = LicenseManager.LICENSE_DURATION_DAYS
    val pct = (days.coerceAtLeast(0).toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)
    val animatedPct by animateFloatAsState(
        targetValue = pct,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 700),
        label = "licenseProgress",
    )

    Column(
        modifier = Modifier
            .padding(horizontal = 18.dp)
            .background(
                color = OreoPalette.SurfaceCard,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(16.dp)
            .fillMaxWidth(),
    ) {
        if (activeKey == null) {
            Text(
                text = "Sin licencia activa",
                color = OreoPalette.OnSurfaceMuted,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Ingresá tu clave en la pantalla de acceso para activar tu licencia.",
                color = OreoPalette.OnSurfaceFaint,
                fontSize = 12.sp,
            )
            return@Column
        }
        Text(
            text = "Clave activa",
            color = OreoPalette.OnSurfaceFaint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = license.maskedActiveKey(),
            color = OreoPalette.OnSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Activada el",
                    color = OreoPalette.OnSurfaceFaint,
                    fontSize = 11.sp,
                )
                Text(
                    text = license.formatStart(),
                    color = OreoPalette.OnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = "Caduca el",
                    color = OreoPalette.OnSurfaceFaint,
                    fontSize = 11.sp,
                )
                Text(
                    text = license.formatEnd(),
                    color = OreoPalette.OnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        // Barra de progreso animada — verde si quedan más de 30 días,
        // ámbar entre 30 y 7, roja por debajo de 7.
        val barColor = when {
            days <= 0 -> OreoPalette.DangerFill
            days < 7 -> OreoPalette.DangerFill
            days < 30 -> OreoPalette.AccentSub
            else -> OreoPalette.Accent
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    color = OreoPalette.Outline.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(6.dp),
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedPct)
                    .height(8.dp)
                    .background(barColor, RoundedCornerShape(6.dp)),
            )
        }
        Spacer(Modifier.height(8.dp))
        val labelDays = when {
            days <= 0 -> "Licencia vencida"
            days == 1 -> "Queda 1 día"
            else -> "Quedan $days días"
        }
        Text(
            text = labelDays,
            color = if (days <= 0) OreoPalette.DangerFill else OreoPalette.OnSurfaceMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        // Mantener el valor de licenseStart referenciado para que Compose
        // observe sus cambios (la recomputación de daysRemaining depende
        // de él, pero Compose no lo "ve" directamente porque es derivado).
        @Suppress("UNUSED_VARIABLE") val _ignored = licenseStart
    }
}
