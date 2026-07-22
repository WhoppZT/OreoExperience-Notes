package com.oreoexperience.notes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.oreoexperience.notes.ui.theme.OreoPalette

/**
 * Elemento del menú de contexto.
 */
data class ContextMenuItem(
    val icon: ImageVector,
    val label: String,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * Menú de contexto custom con posicionamiento bajo la nota.
 *
 * Usa Popup para controlar la posición exacta del menú,
 * apareciendo debajo del elemento que lo activó.
 */
@Composable
fun OreoContextMenu(
    items: List<ContextMenuItem>,
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!expanded) return

    Popup(
        alignment = Alignment.BottomStart,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            clippingEnabled = false,
        ),
    ) {
        Column(
            modifier = modifier
                .width(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    color = OreoPalette.SurfaceOverlay,
                    shape = RoundedCornerShape(16.dp),
                )
                .border(
                    width = 0.5.dp,
                    color = OreoPalette.BorderSubtle,
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(vertical = 4.dp),
        ) {
            items.forEachIndexed { index, item ->
                ContextMenuRow(
                    item = item,
                    onClick = {
                        onDismiss()
                        item.onClick()
                    },
                )

                if (index < items.lastIndex) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(OreoPalette.OutlineFaint),
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextMenuRow(
    item: ContextMenuItem,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = if (item.isDestructive) OreoPalette.DangerFill else OreoPalette.Accent,
            modifier = Modifier.size(18.dp),
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = item.label,
            color = if (item.isDestructive) OreoPalette.DangerFill else OreoPalette.OnSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
