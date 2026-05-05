package com.oreoexperience.notes.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.oreoexperience.notes.data.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer no provisto")
}
