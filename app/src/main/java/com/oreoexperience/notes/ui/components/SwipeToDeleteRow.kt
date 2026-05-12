@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.oreoexperience.notes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.ui.theme.OreoPalette

/**
 * Wrapper que envuelve una fila para soportar **swipe-to-delete**
 * estilo iOS. Al deslizar de derecha a izquierda aparece un fondo rojo
 * con el ícono de papelera + texto "Eliminar"; soltar luego de cruzar
 * el threshold dispara [onDelete].
 *
 * - Swipe sólo hacia la izquierda (EndToStart) — no soportamos archivar.
 * - El threshold es 40% del ancho — el mismo que usa iOS Notes.
 */
@Composable
fun SwipeToDeleteRow(
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            value == SwipeToDismissBoxValue.EndToStart
        },
        positionalThreshold = { distance -> distance * 0.4f },
    )

    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(OreoPalette.DangerFill)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = Color.White,
                )
                Text(
                    text = "  Eliminar",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(start = 28.dp),
                )
            }
        },
        content = { content() },
    )
}
