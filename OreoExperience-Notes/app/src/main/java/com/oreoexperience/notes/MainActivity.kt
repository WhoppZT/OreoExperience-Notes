package com.oreoexperience.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.oreoexperience.notes.ui.LocalAppContainer
import com.oreoexperience.notes.ui.LocalOreoWindowSizeClass
import com.oreoexperience.notes.ui.OreoWidthSizeClass
import com.oreoexperience.notes.ui.OreoWindowSizeClass
import com.oreoexperience.notes.ui.nav.AppNav
import com.oreoexperience.notes.ui.theme.OreoExperienceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(0xFF0A0A0E.toInt()),
            navigationBarStyle = SystemBarStyle.dark(0xFF0A0A0E.toInt()),
        )
        val container = (application as OreoApp).container
        setContent {
            val config = LocalConfiguration.current
            val widthClass = when {
                config.screenWidthDp < 600 -> OreoWidthSizeClass.Compact
                config.screenWidthDp < 840 -> OreoWidthSizeClass.Medium
                else -> OreoWidthSizeClass.Expanded
            }
            val oreoWc = OreoWindowSizeClass(widthSizeClass = widthClass)
            val themeMode by container.userPreferences.themeModeState
            CompositionLocalProvider(
                LocalAppContainer provides container,
                LocalOreoWindowSizeClass provides oreoWc,
            ) {
                OreoExperienceTheme(themeMode = themeMode) {
                    AppNav()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        (application as OreoApp).container.userPreferences.lastBackgroundedAt =
            System.currentTimeMillis()
    }
}
