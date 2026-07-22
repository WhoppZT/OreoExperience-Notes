@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.oreoexperience.notes.ui.servicio

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.data.RegistroCampo
import com.oreoexperience.notes.data.RegistroCampoRepository
import com.oreoexperience.notes.ui.theme.OreoMotion
import com.oreoexperience.notes.ui.theme.OreoPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RegistroEditorDialog(
    initialDayMillis: Long,
    existing: RegistroCampo?,
    onDismiss: () -> Unit,
    onSave: (RegistroCampo) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dayLabel = remember(initialDayMillis) {
        SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES"))
            .format(Date(initialDayMillis))
            .replaceFirstChar { it.uppercase(Locale.getDefault()) }
    }

    var hoursText by remember {
        mutableStateOf(
            existing?.hours?.let { "%.1f".format(Locale("es", "ES"), it) } ?: ""
        )
    }
    var revisits by remember { mutableStateOf(existing?.revisits ?: 0) }
    var studies by remember { mutableStateOf(existing?.studies ?: 0) }
    val carriedPublications = existing?.publications ?: 0
    val carriedVideos = existing?.videos ?: 0
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OreoPalette.SurfaceCard,
        contentColor = OreoPalette.OnSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .padding(bottom = 22.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                OreoPalette.Accent.copy(alpha = 0.15f),
                                OreoPalette.Accent.copy(alpha = 0.05f),
                            ),
                        ),
                    )
                    .padding(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Registro del día",
                            color = OreoPalette.OnSurfaceFaint,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = dayLabel,
                            color = OreoPalette.OnSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (onDelete != null) {
                        DeleteButton(onClick = onDelete)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            HoursField(
                value = hoursText,
                onValueChange = { hoursText = it.replace(',', '.').filter { ch -> ch.isDigit() || ch == '.' } },
            )
            Spacer(Modifier.height(10.dp))

            StepperWithIcon(
                icon = Icons.Outlined.People,
                label = "Revisitas",
                value = revisits,
                onDelta = { revisits = (revisits + it).coerceAtLeast(0) },
            )
            Spacer(Modifier.height(8.dp))
            StepperWithIcon(
                icon = Icons.Outlined.School,
                label = "Cursos bíblicos",
                value = studies,
                onDelta = { studies = (studies + it).coerceAtLeast(0) },
            )
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas (lugar, anécdotas...)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = OreoPalette.OnSurface,
                    unfocusedTextColor = OreoPalette.OnSurface,
                    focusedLabelColor = OreoPalette.AccentSub,
                    unfocusedLabelColor = OreoPalette.OnSurfaceMuted,
                    cursorColor = OreoPalette.Accent,
                    focusedBorderColor = OreoPalette.Accent,
                    unfocusedBorderColor = OreoPalette.Outline,
                ),
            )
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = OreoPalette.OnSurfaceMuted)
                }
                Spacer(Modifier.size(6.dp))
                SaveButton(
                    onClick = {
                        val parsedHours = hoursText
                            .replace(',', '.')
                            .toDoubleOrNull() ?: 0.0
                        onSave(
                            RegistroCampo(
                                id = existing?.id ?: 0,
                                dateMillis = RegistroCampoRepository.startOfDayMillis(initialDayMillis),
                                hours = parsedHours,
                                revisits = revisits,
                                publications = carriedPublications,
                                videos = carriedVideos,
                                studies = studies,
                                notes = notes.trim(),
                            )
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun HoursField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Horas") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.AccessTime,
                contentDescription = null,
                tint = OreoPalette.Accent,
                modifier = Modifier.size(20.dp),
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = OreoPalette.OnSurface,
            unfocusedTextColor = OreoPalette.OnSurface,
            focusedLabelColor = OreoPalette.AccentSub,
            unfocusedLabelColor = OreoPalette.OnSurfaceMuted,
            cursorColor = OreoPalette.Accent,
            focusedBorderColor = OreoPalette.Accent,
            unfocusedBorderColor = OreoPalette.Outline,
        ),
    )
}

@Composable
private fun StepperWithIcon(
    icon: ImageVector,
    label: String,
    value: Int,
    onDelta: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OreoPalette.SurfaceCardHi)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OreoPalette.Accent,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            color = OreoPalette.OnSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.weight(1f))
        StepperButton(symbol = "−", enabled = value > 0) { onDelta(-1) }
        Text(
            text = value.toString(),
            color = OreoPalette.OnSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .size(width = 28.dp, height = 28.dp)
                .padding(top = 2.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        StepperButton(symbol = "+", enabled = true) { onDelta(+1) }
    }
}

@Composable
private fun DeleteButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = OreoMotion.SpringPress(),
        label = "deleteBtnScale",
    )
    
    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(OreoPalette.DangerFill.copy(alpha = 0.12f))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = "Eliminar registro",
            tint = OreoPalette.DangerFill,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SaveButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = OreoMotion.SpringPress(),
        label = "saveBtnScale",
    )
    
    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        OreoPalette.Accent,
                        OreoPalette.AccentLight,
                    ),
                ),
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(
            text = "Guardar",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StepperButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = OreoMotion.SpringPress(),
        label = "stepperScale",
    )
    Box(
        modifier = Modifier
            .size(32.dp)
            .scale(pressScale)
            .clip(CircleShape)
            .background(
                if (enabled) OreoPalette.Accent.copy(alpha = 0.85f)
                else OreoPalette.Outline.copy(alpha = 0.45f),
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
