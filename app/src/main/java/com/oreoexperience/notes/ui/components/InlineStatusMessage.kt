package com.oreoexperience.notes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.ui.theme.OreoPalette

@Composable
fun InlineStatusMessage(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .background(
                color = OreoPalette.SurfaceCardHi,
                shape = RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = text,
            color = OreoPalette.AccentSub,
            fontSize = 13.sp,
        )
    }
}
