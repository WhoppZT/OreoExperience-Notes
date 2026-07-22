package com.oreoexperience.notes.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.data.Discurso
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlinx.coroutines.delay

/**
 * Diálogo de confirmación antes de mandar una nota a la papelera.
 *
 * El botón "Eliminar" comienza deshabilitado (alpha bajo) y se habilita
 * tras 600ms con un efecto de fade para llamar la atención.
 */
@Composable
fun ConfirmDeleteDialog(
    discurso: Discurso,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val enabled by produceState<Boolean>(initialValue = false) {
        delay(600)
        value = true
    }

    val fadeAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.4f,
        animationSpec = tween(durationMillis = 200),
        label = "deleteBtnAlpha",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OreoPalette.SurfaceCard,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "¿Eliminar nota?",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                Text(
                    text = "Se moverá a la papelera.",
                    color = OreoPalette.OnSurfaceMuted,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "\"${discurso.title.ifBlank { "Nota nueva" }}\"",
                    color = OreoPalette.OnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.alpha(fadeAlpha),
                enabled = enabled,
            ) {
                Text(
                    text = "Eliminar",
                    color = OreoPalette.DangerFill,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancelar",
                    color = OreoPalette.Accent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    )
}
