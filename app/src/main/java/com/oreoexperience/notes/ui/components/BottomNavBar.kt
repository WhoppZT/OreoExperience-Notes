@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.oreoexperience.notes.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.oreoexperience.notes.ui.LocalOreoWindowSizeClass
import com.oreoexperience.notes.ui.theme.OreoDuration
import com.oreoexperience.notes.ui.theme.OreoElevation
import com.oreoexperience.notes.ui.theme.OreoMotion
import com.oreoexperience.notes.ui.theme.OreoPalette
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

enum class MainTab(val icon: ImageVector, val label: String) {
    NOTAS(Icons.AutoMirrored.Outlined.StickyNote2, "Notas"),
    SERVICIO(Icons.Outlined.Public, "Servicio"),
    AJUSTES(Icons.Outlined.Settings, "Ajustes"),
}

@Composable
fun OreoBottomNavBar(
    activeTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    noteCount: Int = 0,
) {
    val oreoWc = LocalOreoWindowSizeClass.current
    val isTablet = !oreoWc.isCompact
    val haptic = LocalHapticFeedback.current
    val activeIndex = MainTab.entries.indexOf(activeTab)

    val indicatorOffset by animateDpAsState(
        targetValue = (activeIndex * 100).dp,
        animationSpec = OreoMotion.SpringCard(),
        label = "navIndicatorOffset",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(OreoPalette.SurfaceNavBar),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(OreoPalette.BorderSubtle)
                .align(Alignment.TopCenter),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isTablet) Arrangement.Center else Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MainTab.entries.forEach { tab ->
                    val active = tab == activeTab
                    OreoBottomNavItem(
                        tab = tab,
                        active = active,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTabSelected(tab)
                        },
                        badge = if (tab == MainTab.NOTAS && noteCount > 0) "$noteCount" else null,
                        modifier = if (isTablet) Modifier.width(100.dp) else Modifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun OreoBottomNavItem(
    tab: MainTab,
    active: Boolean,
    onClick: () -> Unit,
    badge: String?,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = OreoMotion.SpringPress(),
        label = "navItemPress",
    )

    val iconBg by animateColorAsState(
        targetValue = if (active) OreoPalette.Accent.copy(alpha = 0.18f) else Color.Transparent,
        animationSpec = OreoMotion.SpringCard(),
        label = "navIconBg",
    )
    val iconBorder by animateColorAsState(
        targetValue = if (active) OreoPalette.Accent.copy(alpha = 0.35f) else Color.Transparent,
        animationSpec = OreoMotion.SpringCard(),
        label = "navIconBorder",
    )
    val iconTint by animateColorAsState(
        targetValue = if (active) OreoPalette.Accent else OreoPalette.TextTertiary,
        animationSpec = OreoMotion.SpringCard(),
        label = "navIconTint",
    )
    val labelColor by animateColorAsState(
        targetValue = if (active) OreoPalette.Accent else OreoPalette.TextTertiary,
        animationSpec = OreoMotion.SpringCard(),
        label = "navLabelColor",
    )
    val badgeScale by animateFloatAsState(
        targetValue = if (badge != null && active) 1f else 0f,
        animationSpec = OreoMotion.SpringPop(),
        label = "navBadgeScale",
    )

    Column(
        modifier = modifier
            .scale(pressScale)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBg, RoundedCornerShape(12.dp))
                .then(
                    if (active) Modifier.border(0.5.dp, iconBorder, RoundedCornerShape(12.dp))
                    else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .scale(badgeScale)
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .background(OreoPalette.Accent, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = badge,
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 8.sp,
                        )
                    }
                }
            }
        }
        Text(
            text = tab.label,
            color = labelColor,
            fontSize = 10.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
