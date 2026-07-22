@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.oreoexperience.notes.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.BuildConfig
import com.oreoexperience.notes.R
import com.oreoexperience.notes.data.LicenseManager
import com.oreoexperience.notes.data.PdfDensity
import com.oreoexperience.notes.data.PdfMargin
import com.oreoexperience.notes.data.ThemeMode
import com.oreoexperience.notes.ui.LocalAppContainer
import com.oreoexperience.notes.ui.LocalOreoWindowSizeClass
import com.oreoexperience.notes.ui.components.InlineStatusMessage
import com.oreoexperience.notes.ui.components.UpdateDialog
import com.oreoexperience.notes.ui.components.sectionEntry
import com.oreoexperience.notes.ui.components.staggeredEntry
import com.oreoexperience.notes.ui.theme.OreoMotion
import com.oreoexperience.notes.data.update.CheckResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ──────────────────────────────────────────────────────────────────────────
//  Banani Design Tokens (from AjustesScreen.html)
// ──────────────────────────────────────────────────────────────────────────

private val BgScreen       = Color(0xFF0E0E12)
private val BgScreenRadial = Color(0xFF1A1528)
private val CardBg         = Color(0x3332284A)   // rgba(30,28,50,0.75) ≈ #1E1C32 @ 75%
private val CardBorder     = Color(0x738C78FF)   // rgba(140,120,255,0.45)
private val DividerColor   = Color(0x337864FF)   // rgba(120,100,255,0.18)
private val TextPrimary    = Color(0xFFF0F0F5)
private val TextMuted      = Color(0xFF7A7A9A)
private val Purple         = Color(0xFF7C5CFC)
private val Green          = Color(0xFF22C55E)
private val Orange         = Color(0xFFF97316)
private val Red            = Color(0xFFEF4444)
private val Blue           = Color(0xFF3B82F6)

// ──────────────────────────────────────────────────────────────────────────
//  SettingsScreen
// ──────────────────────────────────────────────────────────────────────────

// ─── Staggered entry animation for LazyColumn items ──────────
@Composable
private fun StaggeredEntry(index: Int, content: @Composable () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val offset = remember { Animatable(12f) }
    LaunchedEffect(Unit) {
        delay(index * 50L)
        launch {
            alpha.animateTo(1f, tween(320, easing = OreoMotion.EaseOut))
        }
        launch {
            offset.animateTo(0f, OreoMotion.SpringNavElegant())
        }
    }
    Box(
        Modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = offset.value
        },
    ) {
        content()
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onTrash: () -> Unit,
    onNavigateTo: (String) -> Unit = {},
) {
    val container = LocalAppContainer.current
    val prefs     = container.userPreferences
    val ctx       = LocalContext.current
    val scope     = rememberCoroutineScope()
    val windowSize = LocalOreoWindowSizeClass.current
    val isTablet = !windowSize.isCompact

    var snackText        by remember { mutableStateOf<String?>(null) }
    var showThemePicker  by remember { mutableStateOf(false) }
    var checkingUpdate   by remember { mutableStateOf(false) }
    var pendingUpdate    by remember { mutableStateOf<com.oreoexperience.notes.data.update.GitHubRelease?>(null) }
    var pendingIsCritical by remember { mutableStateOf(false) }
    var pendingSize      by remember { mutableStateOf(0L) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    val themeMode       by prefs.themeModeState
    val autoSave        by prefs.autoSaveState
    val pdfDensity      by prefs.pdfDensityState
    val pdfMargin       by prefs.pdfMarginState
    val pdfIncludeCover by prefs.pdfIncludeCoverState
    val pdfWatermark    by prefs.pdfWatermarkState
    val pdfColor        by prefs.pdfColorState
    var showPdfDensityPicker by remember { mutableStateOf(false) }
    var showPdfMarginPicker  by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                container.backupManager.export(uri)
            }
            snackText = result.fold(
                onSuccess = { count -> "Copia de seguridad creada \u00b7 $count notas" },
                onFailure = { e -> "Error al exportar: ${e.message}" },
            )
        }
    }

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

    // ── Background: radial purple gradient on dark ──────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        BgScreenRadial.copy(alpha = 0.18f),
                        Color.Transparent,
                    ),
                    radius = 800f,
                    center = Offset(0.5f, 0f),
                ),
            )
            .background(BgScreen),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding(),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = if (isTablet) 480.dp else 600.dp),
                    contentPadding = PaddingValues(bottom = 48.dp),
                ) {
                    item { StaggeredEntry(0) { TopBar(onBack = onBack) } }
                    item { StaggeredEntry(1) { SettingsHeader() } }

                    // ── GENERAL ────────────────────────────────
                    item { StaggeredEntry(2) { SettingsSection("General") } }
                    item {
                        SettingsCard {
                            SettingsRow(
                                icon = Icons.Outlined.Save,
                                iconTint = Purple,
                                title = "Auto-guardar",
                                subtitle = "Guardar al volver del editor",
                            ) {
                                OreoToggle(
                                    checked = autoSave,
                                    onCheckedChange = { prefs.autoSave = it },
                                )
                            }
                            SettingsDivider()
                            SettingsRow(
                                icon = Icons.Outlined.Brightness6,
                                iconTint = Orange,
                                title = "Tema",
                                trailingText = themeMode.label,
                                onClick = { showThemePicker = true },
                            )
                        }
                    }

                    // ── DATOS ──────────────────────────────────
                    item { SettingsSection("Datos") }
                    item {
                        SettingsCard {
                            SettingsRow(
                                icon = Icons.Outlined.CloudUpload,
                                iconTint = Purple,
                                title = "Hacer copia de seguridad",
                                subtitle = "Guardar a Google Drive u otra ubicaci\u00f3n",
                                onClick = {
                                    val name = "OreoNotes-${
                                        SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                                    }.json"
                                    backupLauncher.launch(name)
                                },
                            )
                            SettingsDivider()
                            SettingsRow(
                                icon = Icons.Outlined.Download,
                                iconTint = Green,
                                title = "Restaurar desde archivo",
                                subtitle = "Importar un .json de copia",
                                onClick = {
                                    restoreLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                            )
                        }
                    }

                    // ── AVANZADO ───────────────────────────────
                    item { SettingsSection("Avanzado") }
                    item {
                        SettingsCard {
                            SettingsRow(
                                icon = Icons.Outlined.Delete,
                                iconTint = Red,
                                title = "Eliminadas recientemente",
                                subtitle = "Restaurar o vaciar la papelera",
                                onClick = onTrash,
                            )
                        }
                    }

                    // ── EXPORTACIÓN A PDF ──────────────────────
                    item { SettingsSection("Exportaci\u00f3n a PDF") }
                    item {
                        SettingsCard {
                            SettingsRow(
                                icon = Icons.Outlined.PictureAsPdf,
                                iconTint = Purple,
                                title = "Densidad de texto",
                                trailingText = pdfDensity.label,
                                onClick = { showPdfDensityPicker = true },
                            )
                            SettingsDivider()
                            SettingsRow(
                                icon = Icons.Outlined.PictureAsPdf,
                                iconTint = Purple,
                                title = "M\u00e1rgenes",
                                trailingText = pdfMargin.label,
                                onClick = { showPdfMarginPicker = true },
                            )
                            SettingsDivider()
                            SettingsRow(
                                icon = Icons.Outlined.Favorite,
                                iconTint = Blue,
                                title = "Incluir portada",
                                subtitle = "P\u00e1gina de presentaci\u00f3n con t\u00edtulo y fecha",
                            ) {
                                OreoToggle(
                                    checked = pdfIncludeCover,
                                    onCheckedChange = { prefs.pdfIncludeCover = it },
                                )
                            }
                            SettingsDivider()
                            SettingsRow(
                                icon = Icons.Outlined.PictureAsPdf,
                                iconTint = Purple,
                                title = "Marca al pie",
                                subtitle = "\"OreoExperience Notes\" tenue",
                            ) {
                                OreoToggle(
                                    checked = pdfWatermark,
                                    onCheckedChange = { prefs.pdfWatermark = it },
                                )
                            }
                        }
                    }

                    // ── INTELIGENCIA ARTIFICIAL ────────────────
                    item { SettingsSection("Inteligencia artificial") }
                    item {
                        val aiEnabled by prefs.aiEnabledState
                        var aiKeyVisible      by remember { mutableStateOf(false) }
                        var testingConnection by remember { mutableStateOf(false) }

                        SettingsCard {
                            SettingsRow(
                                icon = Icons.Outlined.AutoAwesome,
                                iconTint = Purple,
                                title = "Habilitar IA",
                                subtitle = "Corregir, reescribir, resumir y m\u00e1s con Gemini",
                            ) {
                                OreoToggle(
                                    checked = aiEnabled,
                                    onCheckedChange = { prefs.aiEnabled = it },
                                )
                            }
                            SettingsDivider()
                            // ── API Key row ────────────────────
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                SettingsIconBubble(
                                    icon = Icons.Outlined.Lock,
                                    tint = Purple,
                                )
                                Spacer(Modifier.size(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Clave de API",
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        androidx.compose.foundation.text.BasicTextField(
                                            value = prefs.aiApiKey,
                                            onValueChange = { prefs.aiApiKey = it },
                                            textStyle = TextStyle(
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                            ),
                                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Purple),
                                            visualTransformation = if (aiKeyVisible)
                                                androidx.compose.ui.text.input.VisualTransformation.None
                                            else
                                                androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(CardBg)
                                                .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            decorationBox = { inner ->
                                                Box {
                                                    if (prefs.aiApiKey.isEmpty()) {
                                                        Text(
                                                            "Peg\u00e1 tu clave aqu\u00ed\u2026",
                                                            color = TextMuted,
                                                            fontSize = 14.sp,
                                                        )
                                                    }
                                                    inner()
                                                }
                                            },
                                        )
                                        Spacer(Modifier.size(8.dp))
                                        IconButton(onClick = { aiKeyVisible = !aiKeyVisible }) {
                                            Icon(
                                                imageVector = if (aiKeyVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                                contentDescription = if (aiKeyVisible) "Ocultar clave" else "Mostrar clave",
                                                tint = TextMuted,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    val testBtnInteraction = remember { MutableInteractionSource() }
                                    val testBtnPressed by testBtnInteraction.collectIsPressedAsState()
                                    val testBtnScale by animateFloatAsState(
                                        targetValue = if (testBtnPressed) 0.95f else 1f,
                                        animationSpec = OreoMotion.SpringPress(),
                                        label = "testBtnScale",
                                    )
                                    Box(
                                        modifier = Modifier
                                            .scale(testBtnScale)
                                            .clip(RoundedCornerShape(10.dp))
                                            .then(
                                                if (!testingConnection && prefs.aiApiKey.isNotBlank())
                                                    Modifier.background(
                                                        Brush.horizontalGradient(
                                                            listOf(Purple, Color(0xFFC084FC)),
                                                        ),
                                                    )
                                                else Modifier.background(CardBg)
                                            )
                                            .border(1.dp, DividerColor, RoundedCornerShape(10.dp))
                                            .clickable(
                                                interactionSource = testBtnInteraction,
                                                indication = null,
                                                enabled = !testingConnection && prefs.aiApiKey.isNotBlank(),
                                            ) {
                                                testingConnection = true
                                                scope.launch {
                                                    val result = container.aiService.testConnection()
                                                    testingConnection = false
                                                    snackText = result.fold(
                                                        onSuccess = { it },
                                                        onFailure = { it.message ?: "Error desconocido" },
                                                    )
                                                }
                                            }
                                            .padding(horizontal = 18.dp, vertical = 10.dp),
                                    ) {
                                        Text(
                                            if (testingConnection) "Probando\u2026" else "Probar conexi\u00f3n",
                                            color = if (!testingConnection && prefs.aiApiKey.isNotBlank()) Color.White else TextMuted,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            }
                            SettingsDivider()
                            SettingsRow(
                                icon = Icons.Outlined.Info,
                                iconTint = TextMuted,
                                title = "C\u00f3mo obtener una clave gratuita",
                                onClick = {
                                    val uri = android.net.Uri.parse("https://aistudio.google.com/apikey")
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                    ctx.startActivity(intent)
                                },
                            )
                            SettingsDivider()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    "Tu nota viaja directo entre la app y Gemini; nada pasa por nuestros servidores.",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Tier gratuito: 15 pedidos/min, 1.500/d\u00eda.",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }

                    // ── ACERCA DE ──────────────────────────────
                    item { SettingsSection("Acerca de") }
                    item {
                        val infiniteTransition = rememberInfiniteTransition(label = "aboutLogo")
                        val logoPulse by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.05f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2500, easing = EaseInOut),
                                repeatMode = RepeatMode.Reverse,
                            ),
                            label = "logoPulse",
                        )

                        SettingsCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .scale(logoPulse)
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(Purple, Color(0xFFA78BFA)),
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_oreo_cookie),
                                        contentDescription = null,
                                        modifier = Modifier.size(26.dp),
                                    )
                                }
                                Spacer(Modifier.size(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "OreoExperience \u00b7 Notas",
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val parts   = BuildConfig.VERSION_NAME.trim().split(" ", limit = 2)
                                        val core    = parts.firstOrNull().orEmpty()
                                        val channel = parts.getOrNull(1)
                                        Text(
                                            text = "v$core",
                                            color = TextMuted,
                                            fontSize = 13.sp,
                                        )
                                        if (!channel.isNullOrBlank()) {
                                            Spacer(Modifier.size(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        brush = Brush.horizontalGradient(
                                                            colors = listOf(Color(0xFF5B21B6), Purple),
                                                        ),
                                                        shape = RoundedCornerShape(5.dp),
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                            ) {
                                                Text(
                                                    text = channel,
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Creado por Elihu Rueda",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                            SettingsDivider()
                            SettingsRow(
                                icon = Icons.Outlined.CloudUpload,
                                iconTint = Purple,
                                title = "Buscar actualizaciones",
                                subtitle = if (checkingUpdate) "Verificando..." else "Versi\u00f3n actual: ${BuildConfig.VERSION_NAME}",
                                onClick = {
                                    checkingUpdate = true
                                    scope.launch {
                                        val result = container.updateManager.checkForUpdate()
                                        checkingUpdate = false
                                        when (result) {
                                            is CheckResult.Available -> {
                                                pendingUpdate = result.release
                                                pendingIsCritical = result.isCritical
                                                pendingSize = result.sizeBytes
                                                showUpdateDialog = true
                                            }
                                            is CheckResult.UpToDate -> {
                                                snackText = "Ya est\u00e1s en la \u00faltima versi\u00f3n"
                                            }
                                            is CheckResult.Error -> {
                                                snackText = result.message
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    }

                    snackText?.let { msg ->
                        item { InlineStatusMessage(text = msg) }
                    }
                }

                // ── Dialogs ───────────────────────────────────
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
                if (showPdfDensityPicker) {
                    EnumPicker(
                        title = "Densidad del PDF",
                        options = PdfDensity.values().toList(),
                        current = pdfDensity,
                        labelOf = { it.label },
                        onPick = {
                            prefs.pdfDensity = it
                            showPdfDensityPicker = false
                        },
                        onDismiss = { showPdfDensityPicker = false },
                    )
                }
                if (showPdfMarginPicker) {
                    EnumPicker(
                        title = "M\u00e1rgenes del PDF",
                        options = PdfMargin.values().toList(),
                        current = pdfMargin,
                        labelOf = { it.label },
                        onPick = {
                            prefs.pdfMargin = it
                            showPdfMarginPicker = false
                        },
                        onDismiss = { showPdfMarginPicker = false },
                    )
                }
                if (showUpdateDialog && pendingUpdate != null) {
                    UpdateDialog(
                        release = pendingUpdate!!,
                        isCritical = pendingIsCritical,
                        sizeBytes = pendingSize,
                        updateManager = container.updateManager,
                        onDismiss = {
                            showUpdateDialog = false
                            pendingUpdate = null
                        },
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
//  SettingsHeader — Glassmorphic icon + gradient title + purple line subtitle
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsHeader() {
    val infiniteTransition = rememberInfiniteTransition(label = "settingsHeader")
    val windowSize = LocalOreoWindowSizeClass.current
    val isTablet = !windowSize.isCompact

    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "gradientOffset",
    )
    val iconRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = EaseInOut),
            repeatMode = RepeatMode.Restart,
        ),
        label = "iconRotation",
    )
    val iconPulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "iconPulse",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 24.dp else 20.dp, vertical = 8.dp)
            .padding(top = 12.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Glassmorphic icon box
        Box(
            modifier = Modifier
                .size(if (isTablet) 64.dp else 56.dp)
                .scale(iconPulse)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Purple.copy(alpha = 0.25f),
                    spotColor = Purple.copy(alpha = 0.25f),
                )
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Purple.copy(alpha = 0.35f),
                            Color(0xFF5A3CC8).copy(alpha = 0.5f),
                        ),
                    ),
                )
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Brightness6,
                contentDescription = "Ajustes",
                tint = Purple,
                modifier = Modifier
                    .size(if (isTablet) 32.dp else 28.dp)
                    .rotate(iconRotation),
            )
        }
        Spacer(Modifier.size(16.dp))

        Column {
            Text(
                text = "Ajustes",
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFA78BFA),
                            Purple,
                            Color(0xFFC084FC),
                            Color(0xFFA78BFA),
                        ),
                        start = Offset(gradientOffset * 200f, 0f),
                        end   = Offset(gradientOffset * 200f + 300f, 0f),
                    ),
                    fontSize = if (isTablet) 30.sp else 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.02).sp,
                ),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(2.dp)
                        .background(Purple, RoundedCornerShape(1.dp)),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Personaliza tu experiencia",
                    color = TextMuted,
                    fontSize = if (isTablet) 14.sp else 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
//  TopBar — "< Volver" purple text, no background
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(onBack: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = OreoMotion.SpringPress(),
        label = "settingsBackScale",
    )
    val windowSize = LocalOreoWindowSizeClass.current
    val isTablet = !windowSize.isCompact

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 24.dp else 16.dp, vertical = 8.dp)
            .padding(top = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .scale(scale)
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onBack,
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowBackIosNew,
                contentDescription = "Volver",
                tint = Purple,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = "Volver",
                color = Purple,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
//  SettingsSection — purple dash + uppercase muted label
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(label: String) {
    val windowSize = LocalOreoWindowSizeClass.current
    val isTablet = !windowSize.isCompact

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 24.dp else 18.dp, vertical = 8.dp)
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(2.dp)
                .background(Purple.copy(alpha = 0.8f), RoundedCornerShape(1.dp)),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = label.uppercase(Locale.getDefault()),
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.1.sp,
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────
//  SettingsCard — Glassmorphic card
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val windowSize = LocalOreoWindowSizeClass.current
    val isTablet = !windowSize.isCompact

    Column(
        modifier = modifier
            .sectionEntry()
            .padding(horizontal = if (isTablet) 20.dp else 14.dp)
            .clip(RoundedCornerShape(16.dp))
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.35f),
            )
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
    ) {
        content()
    }
}

// ──────────────────────────────────────────────────────────────────────────
//  SettingsDivider — subtle purple-tinted line
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .padding(horizontal = 16.dp)
            .background(DividerColor),
    )
}

// ──────────────────────────────────────────────────────────────────────────
//  SettingsRow — icon square + text + trailing
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = OreoMotion.SpringPress(),
        label = "settingsRowScale",
    )
    val windowSize = LocalOreoWindowSizeClass.current
    val isTablet = !windowSize.isCompact

    val rowModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .scale(pressScale)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = if (isTablet) 20.dp else 16.dp, vertical = 12.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 20.dp else 16.dp, vertical = 12.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconBubble(icon = icon, tint = iconTint)
        Spacer(Modifier.size(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = TextPrimary,
                fontSize = if (isTablet) 16.sp else 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    color = TextMuted,
                    fontSize = if (isTablet) 12.sp else 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        when {
            trailing != null -> trailing()
            trailingText != null -> {
                Text(trailingText, color = TextMuted, fontSize = 13.sp)
                Spacer(Modifier.size(4.dp))
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
            onClick != null -> {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
//  SettingsIconBubble — square with rounded corners (Banani spec: rounded-lg)
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsIconBubble(icon: ImageVector, tint: Color) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = OreoMotion.SpringPress(),
        label = "iconBubbleScale",
    )
    val windowSize = LocalOreoWindowSizeClass.current
    val isTablet = !windowSize.isCompact

    Box(
        modifier = Modifier
            .size(if (isTablet) 40.dp else 36.dp)
            .scale(scale)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(10.dp),
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.35f),
            )
            .background(tint, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(if (isTablet) 20.dp else 18.dp),
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────
//  OreoToggle — custom animated toggle
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun OreoToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = OreoMotion.SpringPress(),
        label = "thumbOffset",
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) com.oreoexperience.notes.ui.theme.OreoPalette.Accent
        else Color(0xFF3A3A4A),
        animationSpec = tween(200),
        label = "trackColor",
    )

    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(trackColor)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .shadow(4.dp, CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────
//  ThemePicker
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun ThemePicker(
    current: ThemeMode,
    onPick: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val trigger = remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { trigger.value = trigger.value + 1 }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = Purple)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Brightness6,
                    contentDescription = null,
                    tint = Purple,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text("Tema", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                ThemeMode.values().forEachIndexed { index, mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntry(index = index, triggerKey = trigger.value, perItemDelayMs = 50)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPick(mode) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (mode == current) Purple else CardBg,
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
                            }
                        }
                        Spacer(Modifier.size(12.dp))
                        Text(
                            mode.label,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = if (mode == current) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFF1E1C32),
        titleContentColor = TextPrimary,
        textContentColor = TextMuted,
        shape = RoundedCornerShape(24.dp),
    )
}

// ──────────────────────────────────────────────────────────────────────────
//  EnumPicker
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun <T> EnumPicker(
    title: String,
    options: List<T>,
    current: T,
    labelOf: (T) -> String,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val trigger = remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { trigger.value = trigger.value + 1 }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = Purple)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.PictureAsPdf,
                    contentDescription = null,
                    tint = Purple,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(title, color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                options.forEachIndexed { index, opt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntry(index = index, triggerKey = trigger.value, perItemDelayMs = 50)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPick(opt) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (opt == current) Purple else CardBg,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (opt == current) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                        Spacer(Modifier.size(12.dp))
                        Text(
                            labelOf(opt),
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = if (opt == current) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFF1E1C32),
        titleContentColor = TextPrimary,
        textContentColor = TextMuted,
        shape = RoundedCornerShape(24.dp),
    )
}

// ──────────────────────────────────────────────────────────────────────────
//  LicenseCard
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun LicenseCard(license: LicenseManager) {
    val activeKey by license.activeKeyState
    val licenseStart by license.licenseStartState

    val days      = license.daysRemaining
    val totalDays = LicenseManager.LICENSE_DURATION_DAYS
    val pct       = (days.coerceAtLeast(0).toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)
    val animatedPct by animateFloatAsState(
        targetValue = pct,
        animationSpec = tween(durationMillis = 700, easing = EaseInOut),
        label = "licenseProgress",
    )

    Column(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(16.dp))
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.35f),
            )
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .fillMaxWidth(),
    ) {
        if (activeKey == null) {
            Text("Sin licencia activa", color = TextMuted, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Ingres\u00e1 tu clave en la pantalla de acceso para activar tu licencia.",
                color = TextMuted, fontSize = 12.sp,
            )
            return@Column
        }
        Text("Clave activa", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(license.maskedActiveKey(), color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Activada el", color = TextMuted, fontSize = 11.sp)
                Text(license.formatStart(), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("Caduca el", color = TextMuted, fontSize = 11.sp)
                Text(license.formatEnd(), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(14.dp))
        val barColor = when {
            days <= 0 -> Red
            days < 7  -> Red
            days < 30 -> Color(0xFFA78BFA)
            else      -> Purple
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(DividerColor),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedPct)
                    .height(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(barColor.copy(alpha = 0.7f), barColor),
                        ),
                    ),
            )
        }
        Spacer(Modifier.height(8.dp))
        val labelDays = when {
            days <= 0 -> "Licencia vencida"
            days == 1 -> "Queda 1 d\u00eda"
            else      -> "Quedan $days d\u00edas"
        }
        Text(
            text = labelDays,
            color = if (days <= 0) Red else TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        @Suppress("UNUSED_VARIABLE") val _ignored = licenseStart
    }
}
