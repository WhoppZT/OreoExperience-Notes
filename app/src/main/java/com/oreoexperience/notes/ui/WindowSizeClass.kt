package com.oreoexperience.notes.ui

import androidx.compose.runtime.staticCompositionLocalOf

enum class OreoWidthSizeClass { Compact, Medium, Expanded }

data class OreoWindowSizeClass(
    val widthSizeClass: OreoWidthSizeClass,
) {
    val isCompact: Boolean get() = widthSizeClass == OreoWidthSizeClass.Compact
    val isMedium: Boolean get() = widthSizeClass == OreoWidthSizeClass.Medium
    val isExpanded: Boolean get() = widthSizeClass == OreoWidthSizeClass.Expanded
}

val LocalOreoWindowSizeClass = staticCompositionLocalOf<OreoWindowSizeClass> {
    OreoWindowSizeClass(widthSizeClass = OreoWidthSizeClass.Compact)
}
